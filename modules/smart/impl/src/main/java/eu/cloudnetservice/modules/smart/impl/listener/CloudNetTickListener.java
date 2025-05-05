/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.smart.impl.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.modules.smart.impl.CloudNetSmartModule;
import eu.cloudnetservice.modules.smart.impl.util.SmartUtil;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.event.instance.CloudNetTickServiceStartEvent;
import eu.cloudnetservice.node.service.CloudServiceManager;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class CloudNetTickListener {

  private final CloudNetSmartModule module;
  private final ServiceTaskProvider taskProvider;
  private final CloudServiceManager serviceManager;
  private final CloudServiceFactory serviceFactory;
  private final NodeServerProvider nodeServerProvider;

  private final Map<String, Long> autoStartBlocks = new HashMap<>();
  private final Map<UUID, AtomicLong> autoStopTicks = new HashMap<>();

  @Inject
  public CloudNetTickListener(
    @NonNull CloudNetSmartModule module,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull CloudServiceManager serviceManager,
    @NonNull CloudServiceFactory serviceFactory,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.module = module;
    this.taskProvider = taskProvider;
    this.serviceManager = serviceManager;
    this.serviceFactory = serviceFactory;
    this.nodeServerProvider = nodeServerProvider;
  }

  @EventListener
  public void handleTick(@NonNull CloudNetTickServiceStartEvent event) {
    this.handleSmartEntries();
  }

  private void handleSmartEntries() {
    this.taskProvider.serviceTasks().forEach(task -> {
      var config = this.module.smartConfig(task);
      if (config != null && config.enabled()) {
        // get all services of the task
        var services = this.serviceManager.servicesByTask(task.name());
        // get all prepared services
        var preparedServices = services.stream()
          .filter(service -> service.lifeCycle() == ServiceLifeCycle.PREPARED)
          .collect(Collectors.toSet());
        // get all running services
        var runningServices = services.stream()
          .filter(service -> service.lifeCycle() == ServiceLifeCycle.RUNNING)
          .collect(Collectors.toSet());
        // get all services which are marked as online by the bridge
        var onlineServices = runningServices.stream()
          .filter(service -> service.readProperty(BridgeDocProperties.IS_ONLINE))
          .collect(Collectors.toSet());
        // handle all smart entries
        this.handleAutoStop(task, config, runningServices, onlineServices);
        this.handleAutoStart(task, config, preparedServices, runningServices, onlineServices);
      }
    });
  }

  private void handleAutoStop(
    @NonNull ServiceTask task,
    @NonNull SmartServiceTaskConfig config,
    @NonNull Collection<ServiceInfoSnapshot> runningServices,
    @NonNull Collection<ServiceInfoSnapshot> onlineServices
  ) {
    // check if we should stop a service now or if that operation would cause an instant restart of a service
    if (!SmartUtil.canStopNow(task, config, runningServices.size())) {
      return;
    }
    // go over all online services
    for (var service : onlineServices) {
      // check if the service should be stopped
      var playerLoad = SmartUtil.playerPercentage(service);
      if (playerLoad <= config.percentOfPlayersToCheckShouldStopTheService()) {
        // get the auto stop ticker for the service
        var stopTicker = this.autoStopTicks.computeIfAbsent(
          service.serviceId().uniqueId(), $ -> new AtomicLong(config.autoStopTimeByUnusedServiceInSeconds()));
        if (stopTicker.decrementAndGet() <= 0) {
          // stop the service now
          service.provider().stop();
        }
      }
    }
  }

  private void handleAutoStart(
    @NonNull ServiceTask task,
    @NonNull SmartServiceTaskConfig config,
    @NonNull Collection<ServiceInfoSnapshot> preparedServices,
    @NonNull Collection<ServiceInfoSnapshot> runningServices,
    @NonNull Collection<ServiceInfoSnapshot> onlineServices
  ) {
    // combine all prepared and running for logic splitting over nodes
    Collection<ServiceInfoSnapshot> allServices = new HashSet<>();
    allServices.addAll(preparedServices);
    allServices.addAll(runningServices);
    // check the prepared service count now as they don't count to the maximum services
    if (config.preparedServices() > preparedServices.size()) {
      var service = this.createService(task, config, allServices);
      // create only one service per heartbeat
      if (service != null) {
        return;
      }
    }
    // check if the maximum service count is reached
    if (config.maxServices() > 0 && runningServices.size() >= config.maxServices()) {
      return;
    }
    // only start services by the smart module if the smart min service count overrides the task min service count
    if (config.smartMinServiceCount() > task.minServiceCount()
      && config.smartMinServiceCount() > runningServices.size()) {
      var service = this.createService(task, config, runningServices);
      // check if the service was created successfully and start it
      if (service != null) {
        service.provider().start();
        // create only one service per heartbeat
        return;
      }
    }
    // check if the auto-start based on the player count is enabled
    if (config.percentOfPlayersForANewServiceByInstance() < 0) {
      return;
    }
    // validate that we can start a service now
    var nextAutoStartTime = this.autoStartBlocks.get(task.name());
    if (nextAutoStartTime != null && nextAutoStartTime >= System.currentTimeMillis()) {
      return;
    }
    // get the overall player counts
    var onlinePlayers = onlineServices.stream()
      .mapToDouble(service -> service.readProperty(BridgeDocProperties.ONLINE_COUNT))
      .sum();
    var maximumPlayers = onlineServices.stream()
      .mapToDouble(service -> Math.max(0, service.readProperty(BridgeDocProperties.MAX_PLAYERS)))
      .sum();
    // check if we can create a percentage count
    if (onlinePlayers == 0 || maximumPlayers == 0) {
      return;
    }
    // make the values absolute
    var absoluteOnline = onlinePlayers / runningServices.size();
    var absoluteMaximum = maximumPlayers / runningServices.size();
    // create the percentage
    var percentage = SmartUtil.percentage(absoluteOnline, absoluteMaximum);
    if (percentage >= config.percentOfPlayersForANewServiceByInstance()) {
      var service = this.createService(task, config, runningServices);
      // check if the service was created successfully and start it
      if (service != null) {
        service.provider().start();
        // block player based service starting now
        this.autoStartBlocks.put(
          task.name(),
          System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.forAnewInstanceDelayTimeInSeconds()));
      }
    }
  }

  private @Nullable ServiceInfoSnapshot createService(
    @NonNull ServiceTask task,
    @NonNull SmartServiceTaskConfig config,
    @NonNull Collection<ServiceInfoSnapshot> services
  ) {
    // check if we should decide directly which node server we use
    NodeServer server = null;
    if (config.splitLogicallyOverNodes()) {
      server = this.selectNodeServer(task, services);
    }
    // create a new service based on the task
    var createResult = this.serviceFactory.createCloudService(ServiceConfiguration.builder(task)
      .node(server == null ? null : server.info().uniqueId())
      .build());
    return createResult.state() == ServiceCreateResult.State.CREATED ? createResult.serviceInfo() : null;
  }

  private @Nullable NodeServer selectNodeServer(
    @NonNull ServiceTask serviceTask,
    @NonNull Collection<ServiceInfoSnapshot> services
  ) {
    // find the node server with the least services on it
    return this.nodeServerProvider.nodeServers().stream()
      .filter(nodeServer -> nodeServer.available() && !nodeServer.draining())
      .filter(nodeServer -> {
        var allowedNodes = serviceTask.associatedNodes();
        return allowedNodes.isEmpty() || allowedNodes.contains(nodeServer.name());
      })
      .map(node -> new Tuple2<>(node, services.stream()
        .filter(service -> service.serviceId().nodeUniqueId().equals(node.info().uniqueId()))
        .count()))
      .min(Comparator.comparingLong(Tuple2::_2))
      .map(Tuple2::_1)
      .orElse(null);
  }
}
