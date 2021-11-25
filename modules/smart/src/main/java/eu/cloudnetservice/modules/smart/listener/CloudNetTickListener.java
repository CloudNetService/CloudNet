/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.CloudNetTick;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import eu.cloudnetservice.modules.smart.CloudNetSmartModule;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.modules.smart.util.SmartUtil;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CloudNetTickListener {

  private final CloudNetSmartModule module;

  private final Map<String, Long> autoStartBlocks = new HashMap<>();
  private final Map<UUID, AtomicLong> autoStopTicks = new HashMap<>();

  public CloudNetTickListener(@NotNull CloudNetSmartModule module) {
    this.module = module;
  }

  @EventListener
  public void handleTick(@NotNull CloudNetTickEvent event) {
    if (CloudNet.getInstance().getClusterNodeServerProvider().getSelfNode().isHeadNode()
      && event.getTicker().getCurrentTick() % CloudNetTick.TPS == 0) {
      this.handleSmartEntries();
    }
  }

  private void handleSmartEntries() {
    CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().forEach(task -> {
      SmartServiceTaskConfig config = this.module.getSmartConfig(task);
      if (config != null && config.isEnabled()) {
        // get all services of the task
        Collection<ServiceInfoSnapshot> services = this.getServiceManager().getCloudServicesByTask(task.getName());
        // get all prepared services
        Collection<ServiceInfoSnapshot> preparedServices = services.stream()
          .filter(service -> service.getLifeCycle() == ServiceLifeCycle.PREPARED)
          .collect(Collectors.toSet());
        // get all running services
        Collection<ServiceInfoSnapshot> runningServices = services.stream()
          .filter(service -> service.getLifeCycle() == ServiceLifeCycle.RUNNING)
          .collect(Collectors.toSet());
        // get all services which are marked as online by the bridge
        Set<ServiceInfoSnapshot> onlineServices = runningServices.stream()
          .filter(service -> BridgeServiceProperties.IS_ONLINE.get(service).orElse(false))
          .collect(Collectors.toSet());
        // handle all smart entries
        this.handleAutoStop(task, config, runningServices, onlineServices);
        this.handleAutoStart(task, config, preparedServices, runningServices, onlineServices);
      }
    });
  }

  private void handleAutoStop(
    @NotNull ServiceTask task,
    @NotNull SmartServiceTaskConfig config,
    @NotNull Collection<ServiceInfoSnapshot> runningServices,
    @NotNull Collection<ServiceInfoSnapshot> onlineServices
  ) {
    // check if we should stop a service now or if that operation would cause an instant restart of a service
    if (!SmartUtil.canStopNow(task, config, runningServices.size())) {
      return;
    }
    // go over all online services
    for (ServiceInfoSnapshot service : onlineServices) {
      // check if the service should be stopped
      double playerLoad = SmartUtil.getPlayerPercentage(service);
      if (playerLoad <= config.getPercentOfPlayersToCheckShouldStopTheService()) {
        // get the auto stop ticker for the service
        AtomicLong stopTicker = this.autoStopTicks.computeIfAbsent(
          service.getServiceId().getUniqueId(), $ -> new AtomicLong(config.getAutoStopTimeByUnusedServiceInSeconds()));
        if (stopTicker.decrementAndGet() <= 0) {
          // stop the service now
          service.provider().stop();
        }
      }
    }
  }

  private void handleAutoStart(
    @NotNull ServiceTask task,
    @NotNull SmartServiceTaskConfig config,
    @NotNull Collection<ServiceInfoSnapshot> preparedServices,
    @NotNull Collection<ServiceInfoSnapshot> runningServices,
    @NotNull Collection<ServiceInfoSnapshot> onlineServices
  ) {
    // combine all prepared and running for logic splitting over nodes
    Collection<ServiceInfoSnapshot> allServices = new HashSet<>();
    allServices.addAll(preparedServices);
    allServices.addAll(runningServices);
    // check the prepared service count now as they don't count to the maximum services
    if (config.getPreparedServices() > preparedServices.size()) {
      ServiceInfoSnapshot service = this.createService(task, config, allServices);
      // create only one service per heartbeat
      if (service != null) {
        return;
      }
    }
    // check if the maximum service count is reached
    if (config.getMaxServices() > 0 && runningServices.size() >= config.getMaxServices()) {
      return;
    }
    // only start services by the smart module if the smart min service count overrides the task min service count
    if (config.getSmartMinServiceCount() > task.getMinServiceCount()
      && config.getSmartMinServiceCount() > runningServices.size()) {
      ServiceInfoSnapshot service = this.createService(task, config, runningServices);
      // check if the service was created successfully and start it
      if (service != null) {
        service.provider().start();
        // create only one service per heartbeat
        return;
      }
    }
    // check if the auto-start based on the player count is enabled
    if (config.getPercentOfPlayersToCheckShouldStopTheService() < 0) {
      return;
    }
    // validate that we can start a service now
    Long nextAutoStartTime = this.autoStartBlocks.get(task.getName());
    if (nextAutoStartTime != null && nextAutoStartTime >= System.currentTimeMillis()) {
      return;
    }
    // get the overall player counts
    double onlinePlayers = onlineServices.stream()
      .mapToDouble(service -> BridgeServiceProperties.ONLINE_COUNT.get(service).orElse(0))
      .sum();
    double maximumPlayers = onlineServices.stream()
      .mapToDouble(service -> BridgeServiceProperties.MAX_PLAYERS.get(service).orElse(0))
      .sum();
    // check if we can create a percentage count
    if (onlinePlayers == 0 || maximumPlayers == 0) {
      return;
    }
    // make the values absolute
    double absoluteOnline = onlinePlayers / runningServices.size();
    double absoluteMaximum = maximumPlayers / runningServices.size();
    // create the percentage
    double percentage = SmartUtil.getPercentage(absoluteOnline, absoluteMaximum);
    if (percentage >= config.getPercentOfPlayersForANewServiceByInstance()) {
      ServiceInfoSnapshot service = this.createService(task, config, runningServices);
      // check if the service was created successfully and start it
      if (service != null) {
        service.provider().start();
        // block player based service starting now
        this.autoStartBlocks.put(
          task.getName(),
          System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.getForAnewInstanceDelayTimeInSeconds()));
      }
    }
  }

  private @Nullable ServiceInfoSnapshot createService(
    @NotNull ServiceTask task,
    @NotNull SmartServiceTaskConfig config,
    @NotNull Collection<ServiceInfoSnapshot> services
  ) {
    // check if we should decide directly which node server we use
    NodeServer server = null;
    if (config.isSplitLogicallyOverNodes()) {
      server = this.selectNodeServer(services);
    }
    // create a new service based on the task
    return this.getServiceFactory().createCloudService(ServiceConfiguration.builder(task)
      .node(server == null ? null : server.getNodeInfo().getUniqueId())
      .build());
  }

  private @Nullable NodeServer selectNodeServer(@NotNull Collection<ServiceInfoSnapshot> services) {
    // get all node servers
    Collection<? extends NodeServer> nodeServers = this.getNodeServerProvider().getNodeServers().stream()
      .filter(nodeServer -> nodeServer.isAvailable() && !nodeServer.isDrain())
      .map(nodeServer -> (NodeServer) nodeServer) // looks stupid but transforms the stream type (we love generics)
      .collect(Collectors.collectingAndThen(Collectors.toSet(), set -> {
        // add the local node to the list if the node is not draining
        NodeServer local = this.getNodeServerProvider().getSelfNode();
        if (!local.isDrain()) {
          set.add(local);
        }
        // return the completed set
        return set;
      }));
    // find the node server with the least services on it
    return nodeServers.stream()
      .map(node -> new Pair<>(node, services.stream()
        .filter(service -> service.getServiceId().getNodeUniqueId().equals(node.getNodeInfo().getUniqueId()))
        .count()))
      .min(Comparator.comparingLong(Pair::getSecond))
      .map(Pair::getFirst)
      .orElse(null);
  }

  private @NotNull ICloudServiceManager getServiceManager() {
    return CloudNet.getInstance().getCloudServiceProvider();
  }

  private @NotNull IClusterNodeServerProvider getNodeServerProvider() {
    return CloudNet.getInstance().getClusterNodeServerProvider();
  }

  private @NotNull CloudServiceFactory getServiceFactory() {
    return CloudNet.getInstance().getCloudServiceFactory();
  }
}
