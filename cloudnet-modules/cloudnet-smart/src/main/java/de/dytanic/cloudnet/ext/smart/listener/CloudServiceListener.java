package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostDeleteEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostPrepareEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.smart.CloudNetServiceSmartProfile;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class CloudServiceListener {

    @EventListener
    public void handleCreateCloudService(CloudServiceCreateEvent event) {
        ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(event.getServiceConfiguration().getServiceId().getTaskName());

        if (serviceTask != null && CloudNetSmartModule.getInstance().hasSmartServiceTaskConfig(serviceTask)) {
            SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask);
            if (smartTask.getMaxServiceCount() > 0 &&
                    CloudNet.getInstance().getCloudServiceProvider().getCloudServices(serviceTask.getName())
                            .stream().filter(serviceInfoSnapshot -> !serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)).count() >= smartTask.getMaxServiceCount()) {
                event.setCancelled(true);
                return;
            }

            CloudNetSmartModule.getInstance().updateAsSmartService(event.getServiceConfiguration(), serviceTask, smartTask);
        }
    }

    @EventListener
    public void handle(CloudServicePostPrepareEvent event) {
        ICloudService cloudService = event.getCloudService();
        ServiceTask task = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(cloudService.getServiceId().getTaskName());

        if (task != null && CloudNetSmartModule.getInstance().hasSmartServiceTaskConfig(task)) {
            SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(task);

            if (smartTask.isDirectTemplatesAndInclusionsSetup()) {
                cloudService.includeInclusions();
                cloudService.includeTemplates();
            }

            UUID uniqueId = cloudService.getServiceId().getUniqueId();

            CloudNetSmartModule.getInstance().getProvidedSmartServices().put(uniqueId, new CloudNetServiceSmartProfile(
                    uniqueId,
                    new AtomicInteger(smartTask.getAutoStopTimeByUnusedServiceInSeconds())
            ));
        }
    }

    @EventListener
    public void handle(CloudServicePostDeleteEvent event) {
        CloudNetSmartModule.getInstance().getProvidedSmartServices().remove(event.getCloudService().getServiceId().getUniqueId());
    }
}
