package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.DriverAPIUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class WrapperServiceTaskProvider implements ServiceTaskProvider, DriverAPIUser {

    private final Wrapper wrapper;

    public WrapperServiceTaskProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return this.getPermanentServiceTasksAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public ServiceTask getServiceTask(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return this.getServiceTaskAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public boolean isServiceTaskPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return this.isServiceTaskPresentAsync(name).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public void addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
        this.addPermanentServiceTaskAsync(serviceTask).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public void removePermanentServiceTask(@NotNull String name) {
        this.removePermanentServiceTaskAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);
        this.removePermanentServiceTask(serviceTask.getName());
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_PERMANENT_SERVICE_TASKS,
                packet -> packet.getBody().readObjectCollection(ServiceTask.class)
        );
    }

    @Override
    @NotNull
    public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_PERMANENT_SERVICE_TASK_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBody().readObject(ServiceTask.class)
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.IS_SERVICE_TASK_PRESENT,
                buffer -> buffer.writeString(name),
                packet -> packet.getBody().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<Void> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.ADD_PERMANENT_SERVICE_TASK,
                buffer -> buffer.writeObject(serviceTask)
        );
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.REMOVE_PERMANENT_SERVICE_TASK,
                buffer -> buffer.writeString(name)
        );
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.removePermanentServiceTaskAsync(serviceTask.getName());
    }

    @Override
    public INetworkClient getNetworkClient() {
        return this.wrapper.getNetworkClient();
    }
}
