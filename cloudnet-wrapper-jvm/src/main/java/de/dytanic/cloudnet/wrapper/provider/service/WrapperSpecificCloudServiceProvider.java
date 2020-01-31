package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperSpecificCloudServiceProvider implements SpecificCloudServiceProvider {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private Wrapper wrapper;
    private UUID uniqueId;
    private String name;
    private ServiceInfoSnapshot serviceInfoSnapshot;

    public WrapperSpecificCloudServiceProvider(Wrapper wrapper, UUID uniqueId) {
        this.wrapper = wrapper;
        this.uniqueId = uniqueId;
    }

    public WrapperSpecificCloudServiceProvider(Wrapper wrapper, String name) {
        this.wrapper = wrapper;
        this.name = name;
    }

    public WrapperSpecificCloudServiceProvider(Wrapper wrapper, ServiceInfoSnapshot serviceInfoSnapshot) {
        this.wrapper = wrapper;
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        if (this.serviceInfoSnapshot != null) {
            return this.serviceInfoSnapshot;
        }
        try {
            return this.getServiceInfoSnapshotAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
        if (this.serviceInfoSnapshot != null) {
            return this.wrapper.getTaskScheduler().schedule(() -> this.serviceInfoSnapshot);
        }
        if (this.uniqueId != null) {
            return this.wrapper.getCloudServiceProvider().getCloudServiceAsync(this.uniqueId);
        }
        if (this.name != null) {
            return this.wrapper.getCloudServiceProvider().getCloudServiceByNameAsync(this.name);
        }
        throw new IllegalArgumentException("Cannot get ServiceInfoSnapshot without uniqueId or name");
    }

    @Override
    public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
        try {
            this.addServiceTemplateAsync(serviceTemplate).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(serviceTemplate);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_template_to_cloud_service")
                        .append("serviceTemplate", serviceTemplate), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        try {
            this.addServiceRemoteInclusionAsync(serviceRemoteInclusion).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(serviceRemoteInclusion);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_remote_inclusion_to_cloud_service")
                        .append("serviceRemoteInclusion", serviceRemoteInclusion), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
        try {
            this.addServiceDeploymentAsync(serviceDeployment).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(serviceDeployment);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_deployment_to_cloud_service")
                        .append("serviceDeployment", serviceDeployment), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    public Queue<String> getCachedLogMessages() {
        try {
            return this.getCachedLogMessagesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public ITask<Queue<String>> getCachedLogMessagesAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cached_log_messages_from_service"), null,
                documentPair -> documentPair.getFirst().get("cachedLogMessages", new TypeToken<Queue<String>>() {
                }.getType()));
    }

    @Override
    public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
        try {
            this.setCloudServiceLifeCycleAsync(lifeCycle).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(lifeCycle);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "set_service_life_cycle")
                        .append("lifeCycle", lifeCycle),
                null,
                VOID_FUNCTION
        );
    }

    @Override
    public void restart() {
        try {
            this.restartAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> restartAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "restart_cloud_service"),
                null,
                VOID_FUNCTION
        );
    }

    @Override
    public void kill() {
        try {
            this.killAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> killAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "kill_cloud_service"),
                null,
                VOID_FUNCTION
        );
    }

    @Override
    public void runCommand(@NotNull String command) {
        try {
            this.runCommandAsync(command).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> runCommandAsync(@NotNull String command) {
        Validate.checkNotNull(command);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "run_command_cloud_service")
                        .append("command", command),
                null,
                VOID_FUNCTION
        );
    }

    @Override
    public void includeWaitingServiceTemplates() {
        try {
            this.includeWaitingServiceTemplatesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void includeWaitingServiceInclusions() {
        try {
            this.includeWaitingServiceInclusionsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void deployResources(boolean removeDeployments) {
        try {
            this.deployResourcesAsync(removeDeployments).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ITask<Void> includeWaitingServiceTemplatesAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_templates"),
                null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> includeWaitingServiceInclusionsAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_inclusions"),
                null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> deployResourcesAsync(boolean removeDeployments) {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                this.createDocumentWithUniqueIdAndName()
                        .append(PacketConstants.SYNC_PACKET_ID_PROPERTY, "deploy_resources_from_service")
                        .append("removeDeployments", removeDeployments), null,
                VOID_FUNCTION);
    }

    private JsonDocument createDocumentWithUniqueIdAndName() {
        return new JsonDocument()
                .append("uniqueId", this.serviceInfoSnapshot != null ? this.serviceInfoSnapshot.getServiceId().getUniqueId() : this.uniqueId)
                .append("name", this.name);
    }
}
