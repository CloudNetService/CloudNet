package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.*;

import java.util.*;

public final class ExampleCreateCloudService {

    private static final CloudNetDriver DRIVER = CloudNetDriver.getInstance();

    public void getServiceByName(String name) {
        //node filter with name parameter
        ServiceInfoSnapshot service = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServiceByName(name);

        if (service != null) {
            //service exist
        } else {
            //service doesn't exist
        }

        //wrapper filter is more specific
        Optional<ServiceInfoSnapshot> optionalServiceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices("Lobby").stream()
                .filter(serviceInfoSnapshot1 -> serviceInfoSnapshot1.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                        serviceInfoSnapshot1.getServiceId().getEnvironment() == ServiceEnvironmentType.MINECRAFT_SERVER &&
                        serviceInfoSnapshot1.getServiceId().getName().equalsIgnoreCase(name))
                .findFirst();

        if (optionalServiceInfoSnapshot.isPresent()) {
            //Service is online and exists
        } else {
            //Service is not online or doesn't exist
        }
    }

    public void getServiceByNameAsync(String name) {
        //use the short cut async
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServiceByNameAsync(name).onComplete(serviceInfoSnapshot -> {
            if (serviceInfoSnapshot != null) {
                //service exists
            } else {
                //service doesn't exist
            }
        });

        //use this as alternative filtering
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync("Lobby").onComplete(serviceInfoSnapshots -> {
            Optional<ServiceInfoSnapshot> optionalServiceInfoSnapshot = serviceInfoSnapshots.stream()
                    .filter(serviceInfoSnapshot1 -> serviceInfoSnapshot1.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                            serviceInfoSnapshot1.getServiceId().getEnvironment() == ServiceEnvironmentType.MINECRAFT_SERVER &&
                            serviceInfoSnapshot1.getServiceId().getName().equalsIgnoreCase(name))
                    .findFirst();

            if (optionalServiceInfoSnapshot.isPresent()) {
                //Service is online and exists
            } else {
                //Service is not online or doesn't exist
            }
        }).fireExceptionOnFailure();
    }

    public void createCloudServiceByTask() {
        if (DRIVER.getServiceTaskProvider().isServiceTaskPresent("Lobby")) {
            ServiceTask serviceTask = DRIVER.getServiceTaskProvider().getServiceTask("Lobby"); //getDef ServiceTask instance
            ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration.builder(serviceTask).build().createNewService(); //Creates a service on cluster and returns the initial snapshot

            if (serviceInfoSnapshot != null) {
                serviceInfoSnapshot.provider().start(); //Starting service
            }
        }
    }

    public void createCustomCloudService() {
        ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration.builder()
                .task("Lobby")
                .node("Node-1")
                .autoDeleteOnStop(false)
                .staticService(false)
                .templates(new ServiceTemplate("Lobby", "default", "local"))
                .groups("Lobby", "Global")
                .maxHeapMemory(512)
                .environment(ServiceEnvironmentType.MINECRAFT_SERVER)
                .properties(JsonDocument.newDocument("votes", "10"))
                .build()
                .createNewService();

        if (serviceInfoSnapshot != null) {
            serviceInfoSnapshot.provider().start();
        }
    }

    public void stopCloudService(UUID serviceUniqueId) {
        // stop the cloud service. If the autoDeleteOnStop is not enabled, you can restart the service with the restart method.
        DRIVER.getCloudServiceProvider(serviceUniqueId).stop();
    }

    public void stopAndDeleteService(UUID serviceUniqueId) {
        // stops and deletes the service gracefully
        DRIVER.getCloudServiceProvider(serviceUniqueId).delete();
    }

    public void createCloudServiceFromATaskWithRandomTemplate(String serviceTaskName) {
        DRIVER.getServiceTaskProvider().getServiceTaskAsync(serviceTaskName).onComplete(serviceTask -> {
            if (serviceTask == null) {
                return;
            }

            serviceTask.setTemplates(
                    serviceTask.getTemplates().size() > 1 ?
                            new ArrayList<>(Collections.singletonList(new ArrayList<>(serviceTask.getTemplates()).get(new Random().nextInt(serviceTask.getTemplates().size()))))
                            :
                            serviceTask.getTemplates()
            );

            ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration.builder(serviceTask).build().createNewService();
            if (serviceInfoSnapshot != null) {
                serviceInfoSnapshot.provider().start();
            }
        });
    }
}