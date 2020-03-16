package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperServiceTaskProvider implements ServiceTaskProvider {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private Wrapper wrapper;

    public WrapperServiceTaskProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        try {
            return this.getPermanentServiceTasksAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceTask getServiceTask(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            return this.getServiceTaskAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isServiceTaskPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);

        try {
            return this.isServiceTaskPresentAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Override
    public void addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
        try {
            this.addPermanentServiceTaskAsync(serviceTask).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void removePermanentServiceTask(@NotNull String name) {
        try {
            this.removePermanentServiceTaskAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);
        this.removePermanentServiceTask(serviceTask.getName());
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_permanent_serviceTasks"), null,
                documentPair -> documentPair.getFirst().get("serviceTasks", new TypeToken<Collection<ServiceTask>>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_service_task").append("name", name), null,
                documentPair -> documentPair.getFirst().get("serviceTask", new TypeToken<ServiceTask>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "is_service_task_present").append("name", name), null,
                documentPair -> documentPair.getFirst().get("result", new TypeToken<Boolean>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<Void> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_permanent_service_task").append("serviceTask", serviceTask), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "remove_permanent_service_task").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.removePermanentServiceTaskAsync(serviceTask.getName());
    }

}
