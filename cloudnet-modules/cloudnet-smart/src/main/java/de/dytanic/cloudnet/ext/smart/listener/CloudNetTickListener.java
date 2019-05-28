package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.smart.CloudNetServiceSmartProfile;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class CloudNetTickListener {

  private final Collection<UUID> newInstanceDelay = Iterables
    .newCopyOnWriteArrayList();

  private final AtomicInteger ticksPerSecond = new AtomicInteger();

  @EventListener
  public void handle(CloudNetTickEvent event) {
    if (ticksPerSecond.getAndIncrement() >= CloudNet.TPS) {
      this.handleSmartTasksConfigItems();
      this.handlePercentStart();
      this.handleAutoStop();

      ticksPerSecond.set(0);
    }
  }

  private void handleSmartTasksConfigItems() {
    List<SmartServiceTaskConfig> smartServiceTaskConfigs = Iterables
      .newArrayList(CloudNetSmartModule.getInstance()
        .getSmartServiceTaskConfigurations());
    Collections.sort(smartServiceTaskConfigs);

    for (SmartServiceTaskConfig task : smartServiceTaskConfigs) {
      ServiceTask serviceTask = CloudNetDriver.getInstance()
        .getServiceTask(task.getTask());

      if (serviceTask != null &&
        !serviceTask.isMaintenance() &&
        serviceTask.getAssociatedNodes() != null &&
        (serviceTask.getAssociatedNodes().contains(
          CloudNet.getInstance().getConfig().getIdentity().getUniqueId()) ||
          serviceTask.getAssociatedNodes().isEmpty()
        )) {
        this.handleMinOnlineCount(task, serviceTask);
        this.autoGeneratePreparedServices(task, serviceTask);
      }
    }
  }

  private void handleMinOnlineCount(SmartServiceTaskConfig task,
    ServiceTask serviceTask) {
    Collection<ServiceInfoSnapshot> services = Iterables.filter(
      CloudNetDriver.getInstance().getCloudService(serviceTask.getName()),
      new Predicate<ServiceInfoSnapshot>() {
        @Override
        public boolean test(ServiceInfoSnapshot serviceInfoSnapshot) {
          return serviceInfoSnapshot.getLifeCycle()
            == ServiceLifeCycle.RUNNING;
        }
      });

    if (task.getMinServiceOnlineCount() > 0 && services.size() < task
      .getMinServiceOnlineCount()) {
      if (services.size() < task.getMinServiceOnlineCount()) {
        ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule
          .getInstance().getFreeNonStartedService(serviceTask.getName());

        if (serviceInfoSnapshot == null) {
          serviceInfoSnapshot = CloudNetSmartModule.getInstance()
            .createSmartCloudService(serviceTask, task);
        }

        if (serviceInfoSnapshot != null) {
          CloudNetSmartModule.getInstance().getProvidedSmartServices()
            .remove(serviceInfoSnapshot.getServiceId().getUniqueId());
          CloudNetDriver.getInstance()
            .setCloudServiceLifeCycle(serviceInfoSnapshot,
              ServiceLifeCycle.RUNNING);
          services.add(serviceInfoSnapshot);
        }
      }
    }
  }

  private void autoGeneratePreparedServices(SmartServiceTaskConfig task,
    ServiceTask serviceTask) {
    Collection<ServiceInfoSnapshot> services = Iterables.filter(
      CloudNetDriver.getInstance().getCloudService(serviceTask.getName()),
      new Predicate<ServiceInfoSnapshot>() {
        @Override
        public boolean test(ServiceInfoSnapshot serviceInfoSnapshot) {
          return
            serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED
              ||
              serviceInfoSnapshot.getLifeCycle()
                == ServiceLifeCycle.DEFINED;
        }
      });

    if (task.getPreparedServices() > 0 && services.size() < task
      .getPreparedServices()) {
      if (services.size() < task.getPreparedServices()) {
        ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule
          .getInstance().createSmartCloudService(serviceTask, task);

        if (serviceInfoSnapshot != null) {
          services.add(serviceInfoSnapshot);
        }
      }
    }
  }

  private void handleAutoStop() {
    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = Iterables
      .filter(CloudNetDriver.getInstance().getCloudServices(),
        new Predicate<ServiceInfoSnapshot>() {
          @Override
          public boolean test(ServiceInfoSnapshot cloudService) {
            return
              CloudNetSmartModule.getInstance().getProvidedSmartServices()
                .containsKey(cloudService.getServiceId().getUniqueId())
                &&
                cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING
                &&
                cloudService.getProperties().contains("Online-Count") &&
                cloudService.getProperties().contains("Max-Players");
          }
        });

    for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
      SmartServiceTaskConfig taskConfig = getSmartTaskConfig(
        serviceInfoSnapshot);
      CloudNetServiceSmartProfile cloudServiceProfile = CloudNetSmartModule
        .getInstance().getProvidedSmartServices()
        .get(serviceInfoSnapshot.getServiceId().getUniqueId());

      if (taskConfig.getAutoStopTimeByUnusedServiceInSeconds() > 0 &&
        taskConfig
          .getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() > -1
        &&
        getPercentOf(
          serviceInfoSnapshot.getProperties().getInt("Online-Count"),
          serviceInfoSnapshot.getProperties().getInt("Max-Players")
        ) <= taskConfig
          .getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture()) {
        if (cloudServiceProfile.getAutoStopCount().decrementAndGet() <= 0) {
          CloudNetDriver.getInstance()
            .setCloudServiceLifeCycle(serviceInfoSnapshot,
              ServiceLifeCycle.STOPPED);
        }

      } else {
        cloudServiceProfile.getAutoStopCount()
          .set(taskConfig.getAutoStopTimeByUnusedServiceInSeconds());
      }
    }
  }

  private void handlePercentStart() {
    for (ICloudService cloudService : CloudNet.getInstance()
      .getCloudServiceManager().getCloudServices().values()) {
      if (cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING &&
        cloudService.getServiceInfoSnapshot().getProperties()
          .contains("Online-Count") &&
        cloudService.getServiceInfoSnapshot().getProperties()
          .contains("Max-Players")) {
        SmartServiceTaskConfig taskConfig = getSmartTaskConfig(
          cloudService.getServiceInfoSnapshot());

        if (isIngameService(cloudService.getServiceInfoSnapshot())) {
          continue;
        }

        if (taskConfig != null
          && taskConfig.getPercentOfPlayersForANewServiceByInstance() > 0
          && !newInstanceDelay
          .contains(cloudService.getServiceId().getUniqueId()) &&
          getPercentOf(
            cloudService.getServiceInfoSnapshot().getProperties()
              .getInt("Online-Count"),
            cloudService.getServiceInfoSnapshot().getProperties()
              .getInt("Max-Players")
          ) >= taskConfig.getPercentOfPlayersForANewServiceByInstance()) {
          newInstanceDelay.add(cloudService.getServiceId().getUniqueId());

          ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule
            .getInstance().getFreeNonStartedService(taskConfig.getTask());

          ServiceTask serviceTask = CloudNetDriver.getInstance()
            .getServiceTask(taskConfig.getTask());

          if (serviceInfoSnapshot == null && serviceTask != null && !serviceTask
            .isMaintenance()) {
            serviceInfoSnapshot = CloudNetSmartModule.getInstance()
              .createSmartCloudService(
                CloudNetDriver.getInstance()
                  .getServiceTask(taskConfig.getTask()),
                taskConfig
              );
          }

          CloudNetDriver.getInstance().getTaskScheduler()
            .schedule(new Runnable() {
                        @Override
                        public void run() {
                          newInstanceDelay
                            .remove(cloudService.getServiceId().getUniqueId());
                        }
                      }, taskConfig.getForAnewInstanceDelayTimeInSeconds(),
              TimeUnit.SECONDS);

          if (serviceInfoSnapshot != null) {
            CloudNetDriver.getInstance()
              .setCloudServiceLifeCycle(serviceInfoSnapshot,
                ServiceLifeCycle.RUNNING);
            break;
          }
        }
      }
    }
  }

  private SmartServiceTaskConfig getSmartTaskConfig(
    ServiceInfoSnapshot serviceInfoSnapshot) {
    return Iterables.first(CloudNetSmartModule.getInstance()
        .getSmartServiceTaskConfigurations(),
      new Predicate<SmartServiceTaskConfig>() {

        @Override
        public boolean test(SmartServiceTaskConfig serviceTaskConfig) {
          return serviceTaskConfig.getTask()
            .equals(serviceInfoSnapshot.getServiceId().getTaskName());
        }
      });
  }

  private double getPercentOf(double onlinePlayer, double maxPlayers) {
    return ((onlinePlayer * 100) / maxPlayers);
  }

  private boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot.getProperties().contains("State")
      && isIngameService0(
      serviceInfoSnapshot.getProperties().getString("State"))) {
      return true;
    }

    if (serviceInfoSnapshot.getProperties().contains("Extra")
      && isIngameService0(
      serviceInfoSnapshot.getProperties().getString("Extra"))) {
      return true;
    }

    if (serviceInfoSnapshot.getProperties().contains("Motd")
      && isIngameService0(
      serviceInfoSnapshot.getProperties().getString("Motd"))) {
      return true;
    }

    return false;
  }

  private boolean isIngameService0(String text) {
    text = text.toLowerCase();

    return text.contains("ingame") || text.contains("running") || text
      .contains("playing");
  }
}