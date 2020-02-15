package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.ArrayList;
import java.util.Collections;

public final class ExampleServiceTasks {

    public void test() {
        addServiceTask();
        updateServiceTask();
        removeServiceTask();
    }

    public void addServiceTask() {
        ServiceTask serviceTask = new ServiceTask(
                new ArrayList<>(), //includes
                new ArrayList<>(Collections.singletonList(
                        new ServiceTemplate(
                                "TestTask",
                                "default",
                                "local"
                        )
                )), //templates
                new ArrayList<>(), //deployments
                "TestTask", //name
                null, //runtime can be null for the default jvm wrapper or "jvm"
                true, //autoDeleteOnStop => if the service stops naturally it will be automatic deleted
                true, //The service won't be deleted fully and will store in the configured directory. The default is /local/services
                new ArrayList<>(), //node ids
                new ArrayList<>(Collections.singletonList("TestTask")), //groups
                new ProcessConfiguration(
                        ServiceEnvironmentType.MINECRAFT_SERVER, //environement type
                        356, //max heap memory size
                        new ArrayList<>()
                ),
                4000, //start port
                0 //min services count with auto creation
        );

        CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    }

    public void updateServiceTask() {
        if (CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent("TestTask")) {
            CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTaskAsync("TestTask").onComplete(result -> {
                result.setMinServiceCount(1);
                CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(result);
            });
        }
    }

    public void removeServiceTask() {
        CloudNetDriver.getInstance().getServiceTaskProvider().removePermanentServiceTask("TestTask");
    }
}