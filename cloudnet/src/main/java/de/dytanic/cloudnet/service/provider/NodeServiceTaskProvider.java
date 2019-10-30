package de.dytanic.cloudnet.service.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.provider.ServiceTaskProvider;

import java.util.Collection;

public class NodeServiceTaskProvider implements ServiceTaskProvider {

    private CloudNet cloudNet;

    public NodeServiceTaskProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return this.cloudNet.getCloudServiceManager().getServiceTasks();
    }

    @Override
    public ServiceTask getServiceTask(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().getServiceTask(name);
    }

    @Override
    public boolean isServiceTaskPresent(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().isTaskPresent(name);
    }

    @Override
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        this.cloudNet.getCloudServiceManager().addPermanentServiceTask(serviceTask);
    }

    @Override
    public void removePermanentServiceTask(String name) {
        Validate.checkNotNull(name);

        this.cloudNet.getCloudServiceManager().removePermanentServiceTask(name);
    }

    @Override
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        this.cloudNet.getCloudServiceManager().removePermanentServiceTask(serviceTask);
    }

}
