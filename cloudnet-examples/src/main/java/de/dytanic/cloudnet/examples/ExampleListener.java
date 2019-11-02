package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.*;

public final class ExampleListener {

    @EventListener
    public void handleServiceStart(CloudServiceStartEvent event) {
        event.getDriver().getLogger().log(LogLevel.INFO, "Service " + event.getServiceInfo().getServiceId().getName() + " is starting...");
    }

    @EventListener
    public void handleServiceConnected(CloudServiceConnectNetworkEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo(); //The serviceInfoSnapshot with all important information from a service

        ServiceId serviceId = serviceInfoSnapshot.getServiceId();
        serviceId.getName();
        serviceId.getUniqueId();
        serviceId.getEnvironment();
        serviceId.getNodeUniqueId();
        serviceId.getTaskName();
        serviceId.getTaskServiceId();

        JsonDocument properties = serviceInfoSnapshot.getProperties();

        ServiceLifeCycle serviceLifeCycle = serviceInfoSnapshot.getLifeCycle();

        if (serviceLifeCycle == ServiceLifeCycle.RUNNING) {
            event.getDriver().getLogger().info("Service is running");
        }

        ProcessSnapshot processSnapshot = serviceInfoSnapshot.getProcessSnapshot();
        processSnapshot.getCpuUsage();
        processSnapshot.getNoHeapUsageMemory();
        processSnapshot.getHeapUsageMemory();
        processSnapshot.getMaxHeapMemory();

        for (ThreadSnapshot threadSnapshot : processSnapshot.getThreads()) {
            threadSnapshot.getName();
            threadSnapshot.isDaemon();
            threadSnapshot.getPriority();
            threadSnapshot.getThreadState();
        }

        processSnapshot.getCurrentLoadedClassCount();
        processSnapshot.getUnloadedClassCount();
        processSnapshot.getTotalLoadedClassCount();

        serviceInfoSnapshot.isConnected();
        serviceInfoSnapshot.getAddress();

        serviceInfoSnapshot.getCreationTime(); //Snapshot creation time in millis

        ServiceConfiguration serviceConfiguration = serviceInfoSnapshot.getConfiguration();

        serviceConfiguration.getPort();
        serviceConfiguration.getGroups();
        serviceConfiguration.getProcessConfig();
        serviceConfiguration.getServiceId();

        for (ServiceTemplate serviceTemplate : serviceConfiguration.getTemplates()) {
            serviceTemplate.getStorage();
            serviceTemplate.getPrefix();
            serviceTemplate.getName();
        }

        for (ServiceDeployment serviceDeployment : serviceConfiguration.getDeployments()) {
            serviceDeployment.getExcludes();

            ServiceTemplate serviceTemplate = serviceDeployment.getTemplate();
        }

        for (ServiceRemoteInclusion serviceRemoteInclusion : serviceConfiguration.getIncludes()) {
            serviceRemoteInclusion.getUrl();
            serviceRemoteInclusion.getDestination();
        }


        serviceConfiguration.getRuntime();
        serviceConfiguration.getProperties();
    }

    @EventListener(channel = "test_channel") //listen the ExampleOwnEvent, which called on "test_channel"
    public void handleExampleOwnEvent(ExampleOwnEvent event) {
        System.out.println(event.getModuleWrapper().getModule().getName()); //print the module name
    }

    @EventListener
    public void handleNotCalledEvent(ExampleOwnEvent event) //On this example module, this listener won't called
    {
        System.out.println("No event listening on public channel");
    }

    @EventListener
    public void handleServiceStop(CloudServiceStopEvent event) {
        event.getDriver().getLogger().log(LogLevel.INFO, "Service " + event.getServiceInfo().getServiceId().getName() + " is stopped...");
    }
}