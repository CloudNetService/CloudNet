package de.dytanic.cloudnet.driver;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.*;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.registry.DefaultServicesRegistry;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.event.DefaultEventManager;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Function;

public abstract class CloudNetDriver {

    private static CloudNetDriver instance;

    protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();

    protected final IEventManager eventManager = new DefaultEventManager();

    protected final IModuleProvider moduleProvider = new DefaultModuleProvider();

    protected final ITaskScheduler taskScheduler = new DefaultTaskScheduler();
    protected final ILogger logger;
    protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

    public CloudNetDriver(ILogger logger) {
        this.logger = logger;
    }

    public static CloudNetDriver getInstance() {
        return CloudNetDriver.instance;
    }

    protected static void setInstance(CloudNetDriver instance) {
        CloudNetDriver.instance = instance;
    }


    public abstract void start() throws Exception;

    public abstract void stop();


    public abstract INetworkClient getNetworkClient();

    public abstract Collection<CommandInfo> getConsoleCommands();

    public abstract CommandInfo getConsoleCommand(String commandLine);

    public abstract String[] sendCommandLine(String commandLine);

    public abstract String[] sendCommandLine(String nodeUniqueId, String commandLine);

    public abstract void sendChannelMessage(String channel, String message, JsonDocument data);

    public abstract void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data);

    public abstract void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data);

    public abstract ServiceInfoSnapshot createCloudService(ServiceTask serviceTask);

    public abstract ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration);

    public abstract ServiceInfoSnapshot createCloudService(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    public abstract Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId,
            int amount,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    public abstract ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine);

    public abstract ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate);

    public abstract ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion);

    public abstract ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment);

    public abstract Queue<String> getCachedLogMessagesFromService(UUID uniqueId);

    public void stopCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.STOPPED);
    }

    public void startCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
    }

    public void deleteCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.DELETED);
    }

    public abstract void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle);

    public abstract void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot);

    public abstract void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot);

    public abstract void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command);

    public abstract Collection<UUID> getServicesAsUniqueId();

    public abstract ServiceInfoSnapshot getCloudServiceByName(String name);

    public abstract Collection<ServiceInfoSnapshot> getCloudServices();

    public abstract Collection<ServiceInfoSnapshot> getStartedCloudServices();

    public abstract Collection<ServiceInfoSnapshot> getCloudService(String taskName);

    public abstract Collection<ServiceInfoSnapshot> getCloudServiceByGroup(String group);

    public abstract ServiceInfoSnapshot getCloudService(UUID uniqueId);

    public abstract Integer getServicesCount();

    public abstract Integer getServicesCountByGroup(String group);

    public abstract Integer getServicesCountByTask(String taskName);

    public abstract Collection<ServiceTask> getPermanentServiceTasks();

    public abstract ServiceTask getServiceTask(String name);

    public abstract boolean isServiceTaskPresent(String name);

    public abstract void addPermanentServiceTask(ServiceTask serviceTask);

    public abstract void removePermanentServiceTask(String name);

    public abstract void removePermanentServiceTask(ServiceTask serviceTask);

    public abstract Collection<GroupConfiguration> getGroupConfigurations();

    public abstract GroupConfiguration getGroupConfiguration(String name);

    public abstract boolean isGroupConfigurationPresent(String name);

    public abstract void addGroupConfiguration(GroupConfiguration groupConfiguration);

    public abstract void removeGroupConfiguration(String name);

    public abstract void removeGroupConfiguration(GroupConfiguration groupConfiguration);

    public abstract NetworkClusterNode[] getNodes();

    public abstract NetworkClusterNode getNode(String uniqueId);

    public abstract NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots();

    public abstract NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId);

    public abstract Collection<ServiceTemplate> getLocalTemplateStorageTemplates();

    public abstract Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment);

    public abstract Collection<ServiceTemplate> getTemplateStorageTemplates(String serviceName);

    public abstract Pair<Boolean, String[]> sendCommandLineAsPermissionUser(UUID uniqueId, String commandLine);

    public abstract void addUser(IPermissionUser permissionUser);

    public abstract void updateUser(IPermissionUser permissionUser);

    public abstract void deleteUser(String name);

    public abstract void deleteUser(IPermissionUser permissionUser);

    public abstract boolean containsUser(UUID uniqueId);

    public abstract boolean containsUser(String name);

    public abstract IPermissionUser getUser(UUID uniqueId);

    public abstract List<IPermissionUser> getUser(String name);

    public abstract Collection<IPermissionUser> getUsers();

    public abstract void setUsers(Collection<? extends IPermissionUser> users);

    public abstract Collection<IPermissionUser> getUserByGroup(String group);

    public abstract void addGroup(IPermissionGroup permissionGroup);

    public abstract void updateGroup(IPermissionGroup permissionGroup);

    public abstract void deleteGroup(String group);

    public abstract void deleteGroup(IPermissionGroup group);

    public abstract boolean containsGroup(String group);

    public abstract IPermissionGroup getGroup(String name);

    public abstract Collection<IPermissionGroup> getGroups();

    public abstract void setGroups(Collection<? extends IPermissionGroup> groups);


    public abstract ITask<Collection<CommandInfo>> getConsoleCommandsAsync();

    public abstract ITask<CommandInfo> getConsoleCommandAsync(String commandLine);

    public abstract ITask<String[]> sendCommandLineAsync(String commandLine);

    public abstract ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine);

    public abstract ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask);

    public abstract ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration);

    public abstract ITask<ServiceInfoSnapshot> createCloudServiceAsync(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    public abstract ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(
            String nodeUniqueId,
            int amount,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    public abstract ITask<ServiceInfoSnapshot> sendCommandLineToCloudServiceAsync(UUID uniqueId, String commandLine);

    public abstract ITask<ServiceInfoSnapshot> addServiceTemplateToCloudServiceAsync(UUID uniqueId, ServiceTemplate serviceTemplate);

    public abstract ITask<ServiceInfoSnapshot> addServiceRemoteInclusionToCloudServiceAsync(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion);

    public abstract ITask<ServiceInfoSnapshot> addServiceDeploymentToCloudServiceAsync(UUID uniqueId, ServiceDeployment serviceDeployment);

    public abstract ITask<Queue<String>> getCachedLogMessagesFromServiceAsync(UUID uniqueId);

    public abstract void includeWaitingServiceTemplates(UUID uniqueId);

    public abstract void includeWaitingServiceInclusions(UUID uniqueId);

    public abstract void deployResources(UUID uniqueId, boolean removeDeployments);

    public abstract ITask<Collection<UUID>> getServicesAsUniqueIdAsync();

    public abstract ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name);

    public abstract ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync();

    public abstract ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServiceInfoSnapshotsAsync();

    public abstract ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName);

    public abstract ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group);

    public abstract ITask<Integer> getServicesCountAsync();

    public abstract ITask<Integer> getServicesCountByGroupAsync(String group);

    public abstract ITask<Integer> getServicesCountByTaskAsync(String taskName);

    public abstract ITask<ServiceInfoSnapshot> getCloudServicesAsync(UUID uniqueId);

    public abstract ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync();

    public abstract ITask<ServiceTask> getServiceTaskAsync(String name);

    public abstract ITask<Boolean> isServiceTaskPresentAsync(String name);

    public abstract ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync();

    public abstract ITask<GroupConfiguration> getGroupConfigurationAsync(String name);

    public abstract ITask<Boolean> isGroupConfigurationPresentAsync(String name);

    public abstract ITask<NetworkClusterNode[]> getNodesAsync();

    public abstract ITask<NetworkClusterNode> getNodeAsync(String uniqueId);

    public abstract ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync();

    public abstract ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId);

    public abstract ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync();

    public abstract ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment);

    public abstract ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(String serviceName);

    public abstract ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(UUID uniqueId, String commandLine);

    public abstract ITask<Void> addUserAsync(IPermissionUser permissionUser);

    public abstract ITask<Boolean> containsUserAsync(UUID uniqueId);

    public abstract ITask<Boolean> containsUserAsync(String name);

    public abstract ITask<IPermissionUser> getUserAsync(UUID uniqueId);

    public abstract ITask<List<IPermissionUser>> getUserAsync(String name);

    public abstract ITask<Collection<IPermissionUser>> getUsersAsync();

    public abstract ITask<Collection<IPermissionUser>> getUserByGroupAsync(String group);

    public abstract ITask<Boolean> containsGroupAsync(String name);

    public abstract ITask<IPermissionGroup> getGroupAsync(String name);

    public abstract ITask<Collection<IPermissionGroup>> getGroupsAsync();


    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, String id, JsonDocument data, Function<JsonDocument, R> function) {
        Validate.checkNotNull(networkChannel);
        Validate.checkNotNull(channel);
        Validate.checkNotNull(id);
        Validate.checkNotNull(data);
        Validate.checkNotNull(function);

        return this.sendCallablePacket(networkChannel, channel, data.append(PacketConstants.SYNC_PACKET_ID_PROPERTY, id), null, jsonDocumentPair -> function.apply(jsonDocumentPair.getFirst()));
    }

    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacketWithAsDriverSyncAPI(this.getNetworkClient().getChannels().iterator().next(), header, body, function);
    }

    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPI(INetworkChannel channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacket(channel, "cloudnet_driver_sync_api", header, body, function);
    }

    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return sendCallablePacket0(networkChannel, channel, header, body, function);
    }

    private <R> ITask<R> sendCallablePacket0(INetworkChannel networkChannel, String channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        header.append(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY, channel);

        Value<R> value = new Value<>();

        ITask<R> listenableTask = new ListenableTask<>(value::getValue);

        InternalSyncPacketChannel.sendCallablePacket(networkChannel, header, body, new ITaskListener<Pair<JsonDocument, byte[]>>() {

            @Override
            public void onComplete(ITask<Pair<JsonDocument, byte[]>> task, Pair<JsonDocument, byte[]> result) {
                value.setValue(function.apply(result));
                try {
                    listenableTask.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(ITask<Pair<JsonDocument, byte[]>> task, Throwable th) {
                th.printStackTrace();
            }
        });

        return listenableTask;
    }

    public ServiceInfoSnapshot createCloudService(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            Integer port
    ) {
        return createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    public Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId,
            int amount,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            Integer port
    ) {
        return createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            Integer port
    ) {
        return createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(
            String nodeUniqueId,
            int amount,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            Integer port
    ) {
        return createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    public void deployResources(UUID uniqueId) {
        this.deployResources(uniqueId, true);
    }

    public IServicesRegistry getServicesRegistry() {
        return this.servicesRegistry;
    }

    public IEventManager getEventManager() {
        return this.eventManager;
    }

    public IModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    public ITaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    public ILogger getLogger() {
        return this.logger;
    }

    public DriverEnvironment getDriverEnvironment() {
        return this.driverEnvironment;
    }
}