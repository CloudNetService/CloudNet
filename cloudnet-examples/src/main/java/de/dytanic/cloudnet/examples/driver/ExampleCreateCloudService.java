package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
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
        ServiceInfoSnapshot serviceInfoSnapshot = Iterables.first(CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices("Lobby"),
                serviceInfoSnapshot1 -> serviceInfoSnapshot1.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                        serviceInfoSnapshot1.getServiceId().getEnvironment() == ServiceEnvironmentType.MINECRAFT_SERVER &&
                        serviceInfoSnapshot1.getServiceId().getName().equalsIgnoreCase(name));

        if (serviceInfoSnapshot != null) {
            //Service is online and exists
        } else {
            //Service is not online or doesn't exist
        }
    }

    public void getServiceByNameAsync(String name) {
        //use the short cut async
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServiceByNameAsync(name).onComplete(serviceInfoSnapshot -> {
            if (serviceInfoSnapshot != null) {
                //service exist
            } else {
                //service doesn't exit
            }
        });

        //use this as alternative filtering
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync("Lobby").onComplete(serviceInfoSnapshots -> {
            ServiceInfoSnapshot serviceInfoSnapshot = Iterables.first(serviceInfoSnapshots, serviceInfoSnapshot1 -> serviceInfoSnapshot1.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                    serviceInfoSnapshot1.getServiceId().getEnvironment() == ServiceEnvironmentType.MINECRAFT_SERVER &&
                    serviceInfoSnapshot1.getServiceId().getName().equalsIgnoreCase(name));

            if (serviceInfoSnapshot != null) {
                //Service is online and exists
            } else {
                //Service is not online or doesn't exist
            }
        }).addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void createCloudServiceByTask() {
        if (DRIVER.getServiceTaskProvider().isServiceTaskPresent("Lobby")) {
            ServiceTask serviceTask = DRIVER.getServiceTaskProvider().getServiceTask("Lobby"); //getDef ServiceTask instance
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(serviceTask); //Creates a service on cluster and returns the initial snapshot

            DRIVER.getCloudServiceProvider(serviceInfoSnapshot).start(); //Starting service
        }
    }

    public void createCustomCloudService() {
        ServiceInfoSnapshot serviceInfoSnapshot = DRIVER.getCloudServiceFactory().createCloudService(
                "Lobby", //task name
                "jvm", //runtime null or jvm or an custom from a custom module
                false, //auto delete on stop
                false, //if the created service static or not
                new ArrayList<>(), // service remote inclusions
                Collections.singletonList(new ServiceTemplate( //Service templates
                        "Lobby",
                        "default",
                        "local"
                )),
                new ArrayList<>(), //service deployments
                Arrays.asList("Lobby", "Global"), //groups
                new ProcessConfiguration( //process configurations
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        356,
                        new ArrayList<>()
                ),
                JsonDocument.newDocument().append("votes", "10"), //define useful properties to call up later
                null //automatic defined port or the start port
        );

        DRIVER.getCloudServiceProvider(serviceInfoSnapshot).start();
    }

    public void createCustomCloudServiceCountsOnOneSpecificNode() {
        DRIVER.getNodeInfoProvider().getNodesAsync().onComplete(networkClusterNodes -> {
            NetworkClusterNode node = networkClusterNodes[0];

            for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                    node.getUniqueId(),
                    2,
                    "PremiumLobby",
                    "jvm",
                    true,
                    false,
                    Iterables.newArrayList(),
                    Collections.singletonList(new ServiceTemplate(
                            "Lobby",
                            "default",
                            "local"
                    )),
                    Iterables.newArrayList(),
                    Collections.singletonList("Lobby"),
                    new ProcessConfiguration(
                            ServiceEnvironmentType.MINECRAFT_SERVER,
                            356,
                            Collections.EMPTY_LIST
                    ),
                    JsonDocument.newDocument().append("votes", "10"), //define useful properties to call up later
                    null //automatic defined port or the start port
            )) {
                DRIVER.getCloudServiceProvider(serviceInfoSnapshot).start(); //start the services
            }
        });
    }

    public void stopCloudService(UUID serviceUniqueId) //stop the cloud service. if the configuration for the service autoDeleteOnStop not enabled. You can restart the service
    {
        DRIVER.getCloudServiceProvider(serviceUniqueId).stop();
    }

    public void stopAndDeleteService(UUID serviceUniqueId) //stops and deletes the service gracefully
    {
        DRIVER.getCloudServiceProvider(serviceUniqueId).delete();
    }

    public void createCloudServiceViaCommand(String task) {
        DRIVER.getNodeInfoProvider().sendCommandLineAsync("create by " + task + " 1 --start"); //send the commandline to the provided node from the service that you the commandline send
    }

    public void createCloudServiceFromATaskWithRandomTemplate(String serviceTaskName) {
        DRIVER.getServiceTaskProvider().getServiceTaskAsync(serviceTaskName).onComplete(serviceTask -> {
            if (serviceTask == null) {
                return;
            }

            serviceTask.setTemplates(
                    serviceTask.getTemplates().size() > 1 ?
                            Iterables.newArrayList(new ServiceTemplate[]{Iterables.newArrayList(serviceTask.getTemplates()).get(new Random().nextInt(serviceTask.getTemplates().size()))})
                            :
                            serviceTask.getTemplates()
            );

            ServiceInfoSnapshot serviceInfoSnapshot = DRIVER.getCloudServiceFactory().createCloudService(serviceTask);
            DRIVER.getCloudServiceProvider(serviceInfoSnapshot.getServiceId().getUniqueId()).start();
        });
    }
}