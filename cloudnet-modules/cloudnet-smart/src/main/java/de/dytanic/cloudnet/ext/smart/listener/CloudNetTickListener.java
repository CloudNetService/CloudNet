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

package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.smart.CloudNetServiceSmartProfile;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class CloudNetTickListener {

  private final Collection<UUID> newInstanceDelay = new CopyOnWriteArrayList<>();

  private final AtomicInteger ticksPerSecond = new AtomicInteger();

  @EventListener
  public void handle(CloudNetTickEvent event) {
    if (CloudNet.getInstance().getClusterNodeServerProvider().getSelfNode().isHeadNode()
      && this.ticksPerSecond.getAndIncrement() >= CloudNet.TPS) {
      this.handleSmartTasksConfigItems();
      this.handlePercentStart();
      this.handleAutoStop();

      this.ticksPerSecond.set(0);
    }
  }

  private void handleSmartTasksConfigItems() {
    CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
      .filter(CloudNetSmartModule.getInstance()::hasSmartServiceTaskConfig)
      .sorted(Comparator.comparingInt(
        serviceTask -> CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask).getPriority()))
      .forEachOrdered(serviceTask -> {
        if (serviceTask.canStartServices() && CloudNet.getInstance().canStartServices(serviceTask)) {

          SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask);

          this.autoGeneratePreparedServices(smartTask, serviceTask);
          this.launchEmptyServices(smartTask, serviceTask);
        }
      });
  }

  private void launchEmptyServices(SmartServiceTaskConfig task, ServiceTask serviceTask) {
    if (task.getMinNonFullServices() <= 0) {
      return;
    }

    long nonFullServices = CloudNet.getInstance().getCloudServiceProvider().getCloudServices()
      .stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equals(serviceTask.getName()))
      .filter(serviceInfoSnapshot ->
        !serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false) ||
          this.getPercentOf(
            serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0),
            serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0)
          ) < task.getPercentOfPlayersForANewServiceByInstance()
      ).count();

    long requiredServices = task.getMinNonFullServices() - nonFullServices;

    if (requiredServices <= 0) {
      return;
    }

    for (int i = 0; i < requiredServices; i++) {
      this.startService(serviceTask.getName());
    }
  }

  private void autoGeneratePreparedServices(SmartServiceTaskConfig task, ServiceTask serviceTask) {
    long preparedServices = CloudNetDriver.getInstance().getCloudServiceProvider()
      .getCloudServices(serviceTask.getName()).stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED ||
        serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.DEFINED)
      .count();

    if (task.getPreparedServices() > 0 && preparedServices < task.getPreparedServices()) {
      if (preparedServices < task.getPreparedServices()) {
        CloudNet.getInstance().getCloudServiceFactory().createCloudService(serviceTask);
      }
    }
  }

  private void handleAutoStop() {
    Collection<ServiceInfoSnapshot> runningServiceInfoSnapshots = CloudNetDriver.getInstance().getCloudServiceProvider()
      .getCloudServices()
      .stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .collect(Collectors.toList());
    Collection<ServiceInfoSnapshot> onlineServiceInfoSnapshots = runningServiceInfoSnapshots.stream()
      .filter(serviceInfoSnapshot -> !serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false))
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperties().contains("Online-Count"))
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperties().contains("Max-Players"))
      .collect(Collectors.toList());

    for (ServiceInfoSnapshot serviceInfoSnapshot : onlineServiceInfoSnapshots) {
      SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance()
        .getSmartServiceTaskConfig(serviceInfoSnapshot);
      if (smartTask == null || !smartTask.isEnabled()) {
        continue;
      }
      CloudNetServiceSmartProfile cloudServiceProfile = CloudNetSmartModule.getInstance().getProvidedSmartServices()
        .get(serviceInfoSnapshot.getServiceId().getUniqueId());
      if (cloudServiceProfile == null) {
        continue;
      }
      if (smartTask.getAutoStopTimeByUnusedServiceInSeconds() > 0 &&
        smartTask.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() > -1 &&
        this.getPercentOf(
          serviceInfoSnapshot.getProperties().getInt("Online-Count"),
          serviceInfoSnapshot.getProperties().getInt("Max-Players")
        ) <= smartTask.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture()) {

        int runningServices = (int) runningServiceInfoSnapshots.stream().filter(
          runningServiceInfoSnapshot -> runningServiceInfoSnapshot.getServiceId().getTaskName()
            .equals(serviceInfoSnapshot.getServiceId().getTaskName())).count();
        ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider()
          .getServiceTask(serviceInfoSnapshot.getServiceId().getTaskName());
        if (serviceTask == null || runningServices <= serviceTask.getMinServiceCount()) {
          continue;
        }

        if (cloudServiceProfile.getAutoStopCount().decrementAndGet() <= 0) {
          System.out.println(LanguageManager.getMessage("module-smart-stop-service-automatically")
            .replace("%id%", serviceInfoSnapshot.getServiceId().getUniqueId().toString())
            .replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName())
            .replace("%serviceId%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()))
          );
          serviceInfoSnapshot.provider().stop();
        }

      } else {
        cloudServiceProfile.getAutoStopCount().set(smartTask.getAutoStopTimeByUnusedServiceInSeconds());
      }
    }
  }

  private void handlePercentStart() {
    for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getLocalCloudServices()) {
      if (cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING &&
        cloudService.getServiceInfoSnapshot().getProperties().contains("Online-Count") &&
        cloudService.getServiceInfoSnapshot().getProperties().contains("Max-Players")) {
        SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance()
          .getSmartServiceTaskConfig(cloudService.getServiceInfoSnapshot());

        if (smartTask == null || !smartTask.isEnabled() || this
          .isIngameService(cloudService.getServiceInfoSnapshot())) {
          continue;
        }
        if (smartTask.getPercentOfPlayersForANewServiceByInstance() <= 0
          || smartTask.getForAnewInstanceDelayTimeInSeconds() <= 0) {
          continue;
        }

        if (!this.newInstanceDelay.contains(cloudService.getServiceId().getUniqueId()) &&
          this.getPercentOf(
            cloudService.getServiceInfoSnapshot().getProperties().getInt("Online-Count"),
            cloudService.getServiceInfoSnapshot().getProperties().getInt("Max-Players")
          ) >= smartTask.getPercentOfPlayersForANewServiceByInstance()) {

          if (this.startService(cloudService.getServiceId().getTaskName()) != null) {
            this.newInstanceDelay.add(cloudService.getServiceId().getUniqueId());
            CloudNetDriver.getInstance().getTaskExecutor().schedule(
              () -> this.newInstanceDelay.remove(cloudService.getServiceId().getUniqueId()),
              smartTask.getForAnewInstanceDelayTimeInSeconds(),
              TimeUnit.SECONDS
            );
          }
        }
      }
    }
  }

  private ServiceInfoSnapshot startService(String task) {
    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule.getInstance().getFreeNonStartedService(task);

    if (serviceInfoSnapshot == null) {
      ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(task);
      if (serviceTask != null && serviceTask.canStartServices()) {
        serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceFactory().createCloudService(serviceTask);
      }
    }

    if (serviceInfoSnapshot != null) {
      serviceInfoSnapshot.provider().start();
    }

    return serviceInfoSnapshot;
  }

  private double getPercentOf(double onlinePlayer, double maxPlayers) {
    return ((onlinePlayer * 100) / maxPlayers);
  }

  private boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return (serviceInfoSnapshot.getProperties().contains("State") && this
      .isIngameService0(serviceInfoSnapshot.getProperties().getString("State"))) ||
      (serviceInfoSnapshot.getProperties().contains("Motd") && this
        .isIngameService0(serviceInfoSnapshot.getProperties().getString("Motd"))) ||
      (serviceInfoSnapshot.getProperties().contains("Extra") && this
        .isIngameService0(serviceInfoSnapshot.getProperties().getString("Extra")));
  }

  private boolean isIngameService0(String text) {
    text = text.toLowerCase();

    return text.contains("ingame") || text.contains("running") || text.contains("playing");
  }
}
