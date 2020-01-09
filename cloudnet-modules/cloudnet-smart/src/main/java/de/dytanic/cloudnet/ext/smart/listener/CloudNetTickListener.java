package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.ext.smart.CloudNetServiceSmartProfile;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class CloudNetTickListener {

    private final Collection<UUID> newInstanceDelay = Iterables.newCopyOnWriteArrayList();

    private final AtomicInteger ticksPerSecond = new AtomicInteger();

    @EventListener
    public void handle(CloudNetTickEvent event) {
        if (this.ticksPerSecond.getAndIncrement() >= CloudNet.TPS) {
            this.handleSmartTasksConfigItems();
            this.handlePercentStart();
            this.handleAutoStop();

            this.ticksPerSecond.set(0);
        }
    }

    private void handleSmartTasksConfigItems() {
        CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
                .filter(CloudNetSmartModule.getInstance()::hasSmartServiceTaskConfig)
                .sorted(Comparator.comparingInt(serviceTask -> CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask).getPriority()))
                .forEachOrdered(serviceTask -> {
                    if (serviceTask.canStartServices() &&
                            serviceTask.getAssociatedNodes() != null &&
                            (serviceTask.getAssociatedNodes().contains(CloudNet.getInstance().getConfig().getIdentity().getUniqueId()) ||
                                    serviceTask.getAssociatedNodes().isEmpty())) {

                        SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask);
                        this.autoGeneratePreparedServices(smartTask, serviceTask);
                    }
                });
    }

    private void autoGeneratePreparedServices(SmartServiceTaskConfig task, ServiceTask serviceTask) {
        long preparedServices = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(serviceTask.getName()).stream()
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
        Collection<ServiceInfoSnapshot> runningServiceInfoSnapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices()
                .stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
                .collect(Collectors.toList());
        Collection<ServiceInfoSnapshot> onlineServiceInfoSnapshots = runningServiceInfoSnapshots.stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperties().contains("Online-Count"))
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperties().contains("Max-Players"))
                .collect(Collectors.toList());

        for (ServiceInfoSnapshot serviceInfoSnapshot : onlineServiceInfoSnapshots) {
            SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceInfoSnapshot);
            if (smartTask == null) {
                continue;
            }
            CloudNetServiceSmartProfile cloudServiceProfile = CloudNetSmartModule.getInstance().getProvidedSmartServices().get(serviceInfoSnapshot.getServiceId().getUniqueId());
            if (cloudServiceProfile == null) {
                continue;
            }
            if (smartTask.getAutoStopTimeByUnusedServiceInSeconds() > 0 &&
                    smartTask.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() > -1 &&
                    this.getPercentOf(
                            serviceInfoSnapshot.getProperties().getInt("Online-Count"),
                            serviceInfoSnapshot.getProperties().getInt("Max-Players")
                    ) <= smartTask.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture()) {

                int runningServices = (int) runningServiceInfoSnapshots.stream().filter(runningServiceInfoSnapshot -> runningServiceInfoSnapshot.getServiceId().getTaskName().equals(serviceInfoSnapshot.getServiceId().getTaskName())).count();
                ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(serviceInfoSnapshot.getServiceId().getTaskName());
                if (runningServices <= serviceTask.getMinServiceCount()) {
                    continue;
                }

                if (cloudServiceProfile.getAutoStopCount().decrementAndGet() <= 0) {
                    System.out.println(LanguageManager.getMessage("module-smart-stop-service-automatically")
                            .replace("%id%", serviceInfoSnapshot.getServiceId().getUniqueId().toString())
                            .replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName())
                            .replace("%serviceId%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()))
                    );
                    CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).stop();
                }

            } else {
                cloudServiceProfile.getAutoStopCount().set(smartTask.getAutoStopTimeByUnusedServiceInSeconds());
            }
        }
    }

    private void handlePercentStart() {
        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                    cloudService.getServiceInfoSnapshot().getProperties().contains("Online-Count") &&
                    cloudService.getServiceInfoSnapshot().getProperties().contains("Max-Players")) {
                SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(cloudService.getServiceInfoSnapshot());

                if (this.isIngameService(cloudService.getServiceInfoSnapshot())) {
                    continue;
                }

                if (smartTask != null && smartTask.getPercentOfPlayersForANewServiceByInstance() > 0 && !this.newInstanceDelay.contains(cloudService.getServiceId().getUniqueId()) &&
                        this.getPercentOf(
                                cloudService.getServiceInfoSnapshot().getProperties().getInt("Online-Count"),
                                cloudService.getServiceInfoSnapshot().getProperties().getInt("Max-Players")
                        ) >= smartTask.getPercentOfPlayersForANewServiceByInstance()) {
                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule.getInstance().getFreeNonStartedService(cloudService.getServiceId().getTaskName());

                    ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(cloudService.getServiceId().getTaskName());

                    if (serviceInfoSnapshot == null && serviceTask != null && serviceTask.canStartServices()) {
                        serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceFactory().createCloudService(serviceTask);
                    }

                    if (serviceInfoSnapshot != null) {
                        this.newInstanceDelay.add(cloudService.getServiceId().getUniqueId());
                        CloudNetDriver.getInstance().getTaskScheduler().schedule(() -> {
                            this.newInstanceDelay.remove(cloudService.getServiceId().getUniqueId());
                        }, smartTask.getForAnewInstanceDelayTimeInSeconds(), TimeUnit.SECONDS);

                        CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).start();
                    }
                }
            }
        }
    }

    private double getPercentOf(double onlinePlayer, double maxPlayers) {
        return ((onlinePlayer * 100) / maxPlayers);
    }

    private boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return (serviceInfoSnapshot.getProperties().contains("State") && this.isIngameService0(serviceInfoSnapshot.getProperties().getString("State"))) ||
                (serviceInfoSnapshot.getProperties().contains("Motd") && this.isIngameService0(serviceInfoSnapshot.getProperties().getString("Motd"))) ||
                (serviceInfoSnapshot.getProperties().contains("Extra") && this.isIngameService0(serviceInfoSnapshot.getProperties().getString("Extra")));
    }

    private boolean isIngameService0(String text) {
        text = text.toLowerCase();

        return text.contains("ingame") || text.contains("running") || text.contains("playing");
    }
}