package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

    private Wrapper wrapper;

    public WrapperGeneralCloudServiceProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<UUID> getServicesAsUniqueId() {
        try {
            return this.getServicesAsUniqueIdAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        try {
            return this.getCloudServiceByNameAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        try {
            return this.getCloudServicesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        try {
            return this.getStartedCloudServicesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(String taskName) {
        try {
            return this.getCloudServicesAsync(taskName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group) {
        try {
            return this.getCloudServicesByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        try {
            return this.getCloudServiceAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getServicesCount() {
        try {
            return this.getServicesCountAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getServicesCountByGroup(String group) {
        try {
            return this.getServicesCountByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getServicesCountByTask(String taskName) {
        try {
            return this.getServicesCountByTaskAsync(taskName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_as_uuid"), null,
                documentPair -> documentPair.getFirst().get("serviceUniqueIds", new TypeToken<Collection<UUID>>() {
                }.getType()));
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudService_by_name").append("name", name), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos"), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_started"), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_taskName").append("taskName", taskName), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_group").append("group", group), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    @Override
    public ITask<Integer> getServicesCountAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count"), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    @Override
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count_by_group").append("group", group), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    @Override
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count_by_task").append("taskName", taskName), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId) {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }
}
