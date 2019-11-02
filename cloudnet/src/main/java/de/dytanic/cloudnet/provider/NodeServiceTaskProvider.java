package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;

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

    @Override
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.cloudNet.scheduleTask(this::getPermanentServiceTasks);
    }

    @Override
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.getServiceTask(name));
    }

    @Override
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.isServiceTaskPresent(name));
    }

    @Override
    public ITask<Void> addPermanentServiceTaskAsync(ServiceTask serviceTask) {
        return this.cloudNet.scheduleTask(() -> {
            this.addPermanentServiceTask(serviceTask);
            return null;
        });
    }

    @Override
    public ITask<Void> removePermanentServiceTaskAsync(String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.removePermanentServiceTask(name);
            return null;
        });
    }

    @Override
    public ITask<Void> removePermanentServiceTaskAsync(ServiceTask serviceTask) {
        return this.cloudNet.scheduleTask(() -> {
            this.removePermanentServiceTask(serviceTask);
            return null;
        });
    }

}
