package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
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
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class CloudNetTickListener {

    private final Collection<UUID> newInstanceDelay = Iterables.newCopyOnWriteArrayList();

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
        CloudNet.getInstance().getPermanentServiceTasks().stream()
                .filter(CloudNetSmartModule.getInstance()::hasSmartServiceTaskConfig)
                .sorted(
                        Comparator.comparingInt(
                                serviceTask -> CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask).getPriority()
                        )
                )
                .forEachOrdered(serviceTask -> {
                    if (!serviceTask.isMaintenance() &&
                            serviceTask.getAssociatedNodes() != null &&
                            (serviceTask.getAssociatedNodes().contains(CloudNet.getInstance().getConfig().getIdentity().getUniqueId()) ||
                                    serviceTask.getAssociatedNodes().isEmpty())) {

                        SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask);
                        this.handleMinOnlineCount(serviceTask);
                        this.autoGeneratePreparedServices(smartTask, serviceTask);
                    }
                });
    }

    private void handleMinOnlineCount(ServiceTask serviceTask) {
        Collection<ServiceInfoSnapshot> services = Iterables.filter(
                CloudNetDriver.getInstance().getCloudService(serviceTask.getName()), serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING);

        if (serviceTask.getMinServiceCount() > 0 && services.size() < serviceTask.getMinServiceCount()) {
            if (services.size() < serviceTask.getMinServiceCount()) {
                ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule.getInstance().getFreeNonStartedService(serviceTask.getName());

                if (serviceInfoSnapshot == null) {
                    serviceInfoSnapshot = CloudNet.getInstance().createCloudService(serviceTask);
                }

                if (serviceInfoSnapshot != null) {
                    CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
                }
            }
        }
    }

    private void autoGeneratePreparedServices(SmartServiceTaskConfig task, ServiceTask serviceTask) {
        long preparedServices = CloudNetDriver.getInstance().getCloudService(serviceTask.getName()).stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED ||
                        serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.DEFINED)
                .count();

        if (task.getPreparedServices() > 0 && preparedServices < task.getPreparedServices()) {
            if (preparedServices < task.getPreparedServices()) {
                CloudNet.getInstance().createCloudService(serviceTask);
            }
        }
    }

    private void handleAutoStop() {
        Collection<ServiceInfoSnapshot> serviceInfoSnapshots = Iterables.filter(CloudNetDriver.getInstance().getCloudServices(), cloudService -> CloudNetSmartModule.getInstance().getProvidedSmartServices().containsKey(cloudService.getServiceId().getUniqueId()) &&
                cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                cloudService.getProperties().contains("Online-Count") &&
                cloudService.getProperties().contains("Max-Players"));

        for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
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
                    getPercentOf(
                            serviceInfoSnapshot.getProperties().getInt("Online-Count"),
                            serviceInfoSnapshot.getProperties().getInt("Max-Players")
                    ) <= smartTask.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture()) {

                int onlineServices = CloudNet.getInstance().getServicesCountByTask(serviceInfoSnapshot.getServiceId().getTaskName());
                ServiceTask serviceTask = CloudNet.getInstance().getServiceTask(serviceInfoSnapshot.getServiceId().getTaskName());
                if (onlineServices <= serviceTask.getMinServiceCount()) {
                    continue;
                }

                if (cloudServiceProfile.getAutoStopCount().decrementAndGet() <= 0) {
                    System.out.println(LanguageManager.getMessage("module-smart-stop-service-automatically")
                            .replace("%id%", serviceInfoSnapshot.getServiceId().getUniqueId().toString())
                            .replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName())
                            .replace("%serviceId%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()))
                    );
                    CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.STOPPED);
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

                if (isIngameService(cloudService.getServiceInfoSnapshot())) {
                    continue;
                }

                if (smartTask != null && smartTask.getPercentOfPlayersForANewServiceByInstance() > 0 && !this.newInstanceDelay.contains(cloudService.getServiceId().getUniqueId()) &&
                        getPercentOf(
                                cloudService.getServiceInfoSnapshot().getProperties().getInt("Online-Count"),
                                cloudService.getServiceInfoSnapshot().getProperties().getInt("Max-Players")
                        ) >= smartTask.getPercentOfPlayersForANewServiceByInstance()) {
                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetSmartModule.getInstance().getFreeNonStartedService(cloudService.getServiceId().getTaskName());

                    ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTask(cloudService.getServiceId().getTaskName());

                    if (serviceInfoSnapshot == null && serviceTask != null && !serviceTask.isMaintenance()) {
                        serviceInfoSnapshot = CloudNet.getInstance().createCloudService(serviceTask);
                    }

                    if (serviceInfoSnapshot != null) {
                        this.newInstanceDelay.add(cloudService.getServiceId().getUniqueId());
                        CloudNetDriver.getInstance().getTaskScheduler().schedule(() -> {
                            this.newInstanceDelay.remove(cloudService.getServiceId().getUniqueId());
                        }, smartTask.getForAnewInstanceDelayTimeInSeconds(), TimeUnit.SECONDS);

                        CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
                    }
                }
            }
        }
    }

    private double getPercentOf(double onlinePlayer, double maxPlayers) {
        return ((onlinePlayer * 100) / maxPlayers);
    }

    private boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return (serviceInfoSnapshot.getProperties().contains("State") && isIngameService0(serviceInfoSnapshot.getProperties().getString("State"))) ||
                (serviceInfoSnapshot.getProperties().contains("Motd") && isIngameService0(serviceInfoSnapshot.getProperties().getString("Motd"))) ||
                (serviceInfoSnapshot.getProperties().contains("Extra") && isIngameService0(serviceInfoSnapshot.getProperties().getString("Extra")));
    }

    private boolean isIngameService0(String text) {
        text = text.toLowerCase();

        return text.contains("ingame") || text.contains("running") || text.contains("playing");
    }
}