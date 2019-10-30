package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;

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
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceTask getServiceTask(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getServiceTaskAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isServiceTaskPresent(String name) {
        Validate.checkNotNull(name);

        try {
            return this.isServiceTaskPresentAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        try {
            this.addPermanentServiceTaskAsync(serviceTask).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePermanentServiceTask(String name) {
        try {
            this.removePermanentServiceTaskAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);
        this.removePermanentServiceTask(serviceTask.getName());
    }

    @Override
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_permanent_serviceTasks"), null,
                documentPair -> documentPair.getFirst().get("serviceTasks", new TypeToken<Collection<ServiceTask>>() {
                }.getType()));
    }

    @Override
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_service_task").append("name", name), null,
                documentPair -> documentPair.getFirst().get("serviceTask", new TypeToken<ServiceTask>() {
                }.getType()));
    }

    @Override
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "is_service_task_present").append("name", name), null,
                documentPair -> documentPair.getFirst().get("result", new TypeToken<Boolean>() {
                }.getType()));
    }

    @Override
    public ITask<Void> addPermanentServiceTaskAsync(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_permanent_service_task").append("serviceTask", serviceTask), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> removePermanentServiceTaskAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "remove_permanent_service_task").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> removePermanentServiceTaskAsync(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        return this.removePermanentServiceTaskAsync(serviceTask.getName());
    }

}
