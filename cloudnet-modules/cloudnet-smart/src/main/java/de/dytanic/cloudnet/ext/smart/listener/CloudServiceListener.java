package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostDeleteEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostPrepareEvent;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.service.ICloudService;

public final class CloudServiceListener {

    @EventListener
    public void handleCreateCloudService(CloudServiceCreateEvent event) {
        ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(event.getServiceConfiguration().getServiceId().getTaskName());

        if (serviceTask != null && CloudNetSmartModule.getInstance().hasSmartServiceTaskConfig(serviceTask)) {
            SmartServiceTaskConfig smartTask = CloudNetSmartModule.getInstance().getSmartServiceTaskConfig(serviceTask);
            if (smartTask.getMaxServiceCount() > 0 &&
                    CloudNet.getInstance().getCloudServiceProvider().getCloudServices(serviceTask.getName()).size() >= smartTask.getMaxServiceCount()) {
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
        }
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        CloudNetSmartModule.getInstance().removeSmartProfile(event.getServiceInfo().getServiceId().getUniqueId());
    }

    @EventListener
    public void handle(CloudServicePostDeleteEvent event) {
        CloudNetSmartModule.getInstance().removeSmartProfile(event.getCloudService().getServiceId().getUniqueId());
    }

}