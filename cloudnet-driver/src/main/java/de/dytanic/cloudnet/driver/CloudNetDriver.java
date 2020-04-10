package de.dytanic.cloudnet.driver;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.registry.DefaultServicesRegistry;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.event.DefaultEventManager;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.PacketQueryProvider;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.provider.*;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Function;

public abstract class CloudNetDriver {

    private static CloudNetDriver instance;

    protected PacketQueryProvider packetQueryProvider;

    protected IPermissionManagement permissionManagement;
    protected PermissionProvider permissionProvider;

    protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();

    protected final IEventManager eventManager = new DefaultEventManager();

    protected final IModuleProvider moduleProvider = new DefaultModuleProvider();

    protected final ITaskScheduler taskScheduler = new DefaultTaskScheduler();
    protected final ILogger logger;
    protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

    private int pid = -2;

    public CloudNetDriver(@NotNull ILogger logger) {
        this.logger = logger;
    }

    public static CloudNetDriver getInstance() {
        return CloudNetDriver.instance;
    }

    /**
     * The CloudNetDriver instance won't be null usually, this method is only relevant for tests
     *
     * @return optional CloudNetDriver
     */
    public static Optional<CloudNetDriver> optionalInstance() {
        return Optional.ofNullable(CloudNetDriver.instance);
    }

    protected static void setInstance(@NotNull CloudNetDriver instance) {
        CloudNetDriver.instance = instance;
    }


    public abstract void start() throws Exception;

    public abstract void stop();


    @NotNull
    public abstract CloudServiceFactory getCloudServiceFactory();

    @NotNull
    public abstract ServiceTaskProvider getServiceTaskProvider();

    @NotNull
    public abstract NodeInfoProvider getNodeInfoProvider();

    @NotNull
    public abstract GroupConfigurationProvider getGroupConfigurationProvider();

    @NotNull
    public abstract CloudMessenger getMessenger();

    /**
     * @deprecated replaced with {@link #getPermissionManagement()}
     */
    @NotNull
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public PermissionProvider getPermissionProvider() {
        return this.permissionProvider != null ? this.permissionProvider : (this.permissionProvider = new DefaultPermissionProvider(this::getPermissionManagement));
    }

    @NotNull
    public IPermissionManagement getPermissionManagement() {
        Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
        return this.permissionManagement;
    }

    public void setPermissionManagement(@NotNull IPermissionManagement permissionManagement) {
        if (this.permissionManagement != null && !this.permissionManagement.canBeOverwritten()) {
            throw new IllegalStateException("Current permission management cannot be overwritten");
        }

        this.permissionManagement = permissionManagement;
    }

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param name the name of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param uniqueId the uniqueId of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param serviceInfoSnapshot the info of the service to create a provider for
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    /**
     * Returns the general CloudServiceProvider
     *
     * @return the instance of the {@link GeneralCloudServiceProvider}
     */
    @NotNull
    public abstract GeneralCloudServiceProvider getCloudServiceProvider();

    @NotNull
    public abstract INetworkClient getNetworkClient();

    @NotNull
    public abstract ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync();

    @NotNull
    public abstract ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName);

    public abstract Collection<ServiceTemplate> getLocalTemplateStorageTemplates();

    public abstract Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName);

    public abstract void setGlobalLogLevel(@NotNull LogLevel logLevel);

    public abstract void setGlobalLogLevel(int logLevel);

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#sendCommandLine(String)
     * @deprecated moved to {@link NodeInfoProvider#sendCommandLine(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public String[] sendCommandLine(String commandLine) {
        return this.getNodeInfoProvider().sendCommandLine(commandLine);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#sendCommandLine(String, String)
     * @deprecated moved to {@link NodeInfoProvider#sendCommandLine(String, String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public String[] sendCommandLine(String nodeUniqueId, String commandLine) {
        return this.getNodeInfoProvider().sendCommandLine(nodeUniqueId, commandLine);
    }

    /**
     * Fetches the PID of this process.
     *
     * @return the PID as an int or -1, if it couldn't be fetched
     */
    public int getOwnPID() {
        if (this.pid == -2) {
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            int index = runtimeName.indexOf('@');
            try {
                return this.pid = (index < 1 ? -1 : Integer.parseInt(runtimeName.substring(0, index)));
            } catch (NumberFormatException ignored) {
                return this.pid = -1;
            }
        }
        return this.pid;
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getConsoleCommands()
     * @deprecated moved to {@link NodeInfoProvider#getConsoleCommands()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<CommandInfo> getConsoleCommands() {
        return this.getNodeInfoProvider().getConsoleCommands();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getConsoleCommand(String)
     * @deprecated moved to {@link NodeInfoProvider#getConsoleCommand(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public CommandInfo getConsoleCommand(String commandLine) {
        return this.getNodeInfoProvider().getConsoleCommand(commandLine);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getConsoleCommandsAsync()
     * @deprecated moved to {@link NodeInfoProvider#getConsoleCommandsAsync()}
     */
    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
        return this.getNodeInfoProvider().getConsoleCommandsAsync();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getConsoleCommandAsync(String)
     * @deprecated moved to {@link NodeInfoProvider#getConsoleCommandAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<CommandInfo> getConsoleCommandAsync(String commandLine) {
        return this.getNodeInfoProvider().getConsoleCommandAsync(commandLine);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#sendCommandLineAsync(String)
     * @deprecated moved to {@link NodeInfoProvider#sendCommandLineAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<String[]> sendCommandLineAsync(String commandLine) {
        return this.getNodeInfoProvider().sendCommandLineAsync(commandLine);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#sendCommandLine(String, String)
     * @deprecated moved to {@link NodeInfoProvider#sendCommandLineAsync(String, String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine) {
        return this.getNodeInfoProvider().sendCommandLineAsync(nodeUniqueId, commandLine);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodesAsync()
     * @deprecated moved to {@link NodeInfoProvider#getNodesAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return this.getNodeInfoProvider().getNodesAsync();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodeAsync(String)
     * @deprecated moved to {@link NodeInfoProvider#getNodeAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<NetworkClusterNode> getNodeAsync(String uniqueId) {
        return this.getNodeInfoProvider().getNodeAsync(uniqueId);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodeInfoSnapshotsAsync()
     * @deprecated moved to {@link NodeInfoProvider#getNodeInfoSnapshotsAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return this.getNodeInfoProvider().getNodeInfoSnapshotsAsync();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodeInfoSnapshotAsync(String)
     * @deprecated moved to {@link NodeInfoProvider#getNodeInfoSnapshotAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId) {
        return this.getNodeInfoProvider().getNodeInfoSnapshotAsync(uniqueId);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodes()
     * @deprecated moved to {@link NodeInfoProvider#getNodes()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public NetworkClusterNode[] getNodes() {
        return this.getNodeInfoProvider().getNodes();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNode(String)
     * @deprecated moved to {@link NodeInfoProvider#getNode(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public NetworkClusterNode getNode(String uniqueId) {
        return this.getNodeInfoProvider().getNode(uniqueId);
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodeInfoSnapshots()
     * @deprecated moved to {@link NodeInfoProvider#getNodeInfoSnapshots()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
        return this.getNodeInfoProvider().getNodeInfoSnapshots();
    }

    /**
     * @see #getNodeInfoProvider()
     * @see NodeInfoProvider#getNodeInfoSnapshot(String)
     * @deprecated moved to {@link NodeInfoProvider#getNodeInfoSnapshot(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId) {
        return this.getNodeInfoProvider().getNodeInfoSnapshot(uniqueId);
    }

    /**
     * @see #getMessenger()
     * @see CloudMessenger#sendChannelMessage(String, String, JsonDocument)
     * @deprecated moved to {@link CloudMessenger#sendChannelMessage(String, String, JsonDocument)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void sendChannelMessage(String channel, String message, JsonDocument data) {
        this.getMessenger().sendChannelMessage(channel, message, data);
    }

    /**
     * @see #getMessenger()
     * @see CloudMessenger#sendChannelMessage(ServiceInfoSnapshot, String, String, JsonDocument)
     * @deprecated moved to {@link CloudMessenger#sendChannelMessage(ServiceInfoSnapshot, String, String, JsonDocument)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data) {
        this.getMessenger().sendChannelMessage(targetServiceInfoSnapshot, channel, message, data);
    }

    /**
     * @see #getMessenger()
     * @see CloudMessenger#sendChannelMessage(ServiceTask, String, String, JsonDocument)
     * @deprecated moved to {@link CloudMessenger#sendChannelMessage(ServiceTask, String, String, JsonDocument)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data) {
        this.getMessenger().sendChannelMessage(targetServiceTask, channel, message, data);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(ServiceTask)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(ServiceTask)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        return this.getCloudServiceFactory().createCloudService(serviceTask);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(ServiceConfiguration)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(ServiceConfiguration)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        return this.getCloudServiceFactory().createCloudService(serviceConfiguration);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot createCloudService(String name,
                                                  String runtime,
                                                  boolean autoDeleteOnStop,
                                                  boolean staticService,
                                                  Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  Integer port) {
        return this.getCloudServiceFactory().createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot createCloudService(String name,
                                                  String runtime,
                                                  boolean autoDeleteOnStop,
                                                  boolean staticService,
                                                  Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  JsonDocument properties,
                                                  Integer port) {
        return this.getCloudServiceFactory().createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(String, int, String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(String, int, String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId,
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
                                                              Integer port) {
        return this.getCloudServiceFactory().createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(String, int, String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(String, int, String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId,
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
                                                              Integer port) {
        return this.getCloudServiceFactory().createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(ServiceTask)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(ServiceTask)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return this.getCloudServiceFactory().createCloudServiceAsync(serviceTask);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(ServiceConfiguration)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(ServiceConfiguration)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        return this.getCloudServiceFactory().createCloudServiceAsync(serviceConfiguration);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                              String runtime,
                                                              boolean autoDeleteOnStop,
                                                              boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              Integer port) {
        return this.getCloudServiceFactory().createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                              String runtime,
                                                              boolean autoDeleteOnStop,
                                                              boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties,
                                                              Integer port) {
        return this.getCloudServiceFactory().createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
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
                                                                          Integer port) {
        return this.getCloudServiceFactory().createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, JsonDocument, Integer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
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
                                                                          Integer port) {
        return this.getCloudServiceFactory().createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#runCommand(String)
     * @deprecated moved to {@link SpecificCloudServiceProvider#runCommand(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine) {
        SpecificCloudServiceProvider cloudServiceProvider = this.getCloudServiceProvider(uniqueId);
        cloudServiceProvider.runCommand(commandLine);
        return cloudServiceProvider.getServiceInfoSnapshot();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceTemplate(ServiceTemplate)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceTemplate(ServiceTemplate)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate) {
        this.getCloudServiceProvider(uniqueId).addServiceTemplate(serviceTemplate);
        return this.getCloudService(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceRemoteInclusion(ServiceRemoteInclusion)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceRemoteInclusion(ServiceRemoteInclusion)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        this.getCloudServiceProvider(uniqueId).addServiceRemoteInclusion(serviceRemoteInclusion);
        return this.getCloudService(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceDeployment(ServiceDeployment)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceDeployment(ServiceDeployment)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment) {
        this.getCloudServiceProvider(uniqueId).addServiceDeployment(serviceDeployment);
        return this.getCloudService(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#getCachedLogMessages()
     * @deprecated moved to {@link SpecificCloudServiceProvider#getCachedLogMessages()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Queue<String> getCachedLogMessagesFromService(UUID uniqueId) {
        return this.getCloudServiceProvider(uniqueId).getCachedLogMessages();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#stop()
     * @deprecated moved to {@link SpecificCloudServiceProvider#stop()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void stopCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.STOPPED);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#start()
     * @deprecated moved to {@link SpecificCloudServiceProvider#start()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void startCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#delete()
     * @deprecated moved to {@link SpecificCloudServiceProvider#delete()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deleteCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.DELETED);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#setCloudServiceLifeCycle(ServiceLifeCycle)
     * @deprecated moved to {@link SpecificCloudServiceProvider#setCloudServiceLifeCycle(ServiceLifeCycle)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.provider().setCloudServiceLifeCycle(lifeCycle);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#restart()
     * @deprecated moved to {@link SpecificCloudServiceProvider#restart()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.provider().restart();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#kill()
     * @deprecated moved to {@link SpecificCloudServiceProvider#kill()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.provider().kill();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#runCommand(String)
     * @deprecated moved to {@link SpecificCloudServiceProvider#runCommand(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.provider().runCommand(command);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#runCommand(String)
     * @deprecated moved to {@link SpecificCloudServiceProvider#runCommand(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> sendCommandLineToCloudServiceAsync(UUID uniqueId, String commandLine) {
        this.getCloudServiceProvider(uniqueId).runCommandAsync(commandLine);
        return this.getCloudServicesAsync(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceTemplateAsync(ServiceTemplate)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceTemplateAsync(ServiceTemplate)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> addServiceTemplateToCloudServiceAsync(UUID uniqueId, ServiceTemplate serviceTemplate) {
        this.getCloudServiceProvider(uniqueId).addServiceTemplateAsync(serviceTemplate);
        return this.getCloudServicesAsync(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceRemoteInclusionAsync(ServiceRemoteInclusion)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceRemoteInclusionAsync(ServiceRemoteInclusion)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> addServiceRemoteInclusionToCloudServiceAsync(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        this.getCloudServiceProvider(uniqueId).addServiceRemoteInclusionAsync(serviceRemoteInclusion);
        return this.getCloudServicesAsync(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#addServiceDeploymentAsync(ServiceDeployment)
     * @deprecated moved to {@link SpecificCloudServiceProvider#addServiceDeploymentAsync(ServiceDeployment)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> addServiceDeploymentToCloudServiceAsync(UUID uniqueId, ServiceDeployment serviceDeployment) {
        this.getCloudServiceProvider(uniqueId).addServiceDeploymentAsync(serviceDeployment);
        return this.getCloudServicesAsync(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#getCachedLogMessages()
     * @deprecated moved to {@link SpecificCloudServiceProvider#getCachedLogMessages()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Queue<String>> getCachedLogMessagesFromServiceAsync(UUID uniqueId) {
        return this.getCloudServiceProvider(uniqueId).getCachedLogMessagesAsync();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#includeWaitingServiceTemplates()
     * @deprecated moved to {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void includeWaitingServiceTemplates(UUID uniqueId) {
        this.getCloudServiceProvider(uniqueId).includeWaitingServiceTemplates();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#includeWaitingServiceInclusions()
     * @deprecated moved to {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void includeWaitingServiceInclusions(UUID uniqueId) {
        this.getCloudServiceProvider(uniqueId).includeWaitingServiceInclusions();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#deployResources(boolean)
     * @deprecated moved to {@link SpecificCloudServiceProvider#deployResources(boolean)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deployResources(UUID uniqueId, boolean removeDeployments) {
        this.getCloudServiceProvider(uniqueId).deployResources(removeDeployments);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#deployResources()
     * @deprecated moved to {@link SpecificCloudServiceProvider#deployResources()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deployResources(UUID uniqueId) {
        this.deployResources(uniqueId, true);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesAsUniqueId()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesAsUniqueId()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<UUID> getServicesAsUniqueId() {
        return this.getCloudServiceProvider().getServicesAsUniqueId();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServiceByName(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServiceByName(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        return this.getCloudServiceProvider().getCloudServiceByName(name);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServices()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServices()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        return this.getCloudServiceProvider().getCloudServices();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getStartedCloudServices()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getStartedCloudServices()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        return this.getCloudServiceProvider().getStartedCloudServices();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServices(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServices(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> getCloudService(String taskName) {
        return this.getCloudServiceProvider().getCloudServices(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServices(ServiceEnvironmentType)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServices(ServiceEnvironmentType)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        return this.getCloudServiceProvider().getCloudServices(environment);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesByGroup(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesByGroup(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceInfoSnapshot> getCloudServiceByGroup(String group) {
        return this.getCloudServiceProvider().getCloudServicesByGroup(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudService(UUID)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudService(UUID)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        return this.getCloudServiceProvider().getCloudService(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCount()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCount()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Integer getServicesCount() {
        return this.getCloudServiceProvider().getServicesCount();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByGroup(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByGroup(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Integer getServicesCountByGroup(String group) {
        return this.getCloudServiceProvider().getServicesCountByGroup(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByTask(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByTask(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Integer getServicesCountByTask(String taskName) {
        return this.getCloudServiceProvider().getServicesCountByTask(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesAsUniqueIdAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesAsUniqueIdAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.getCloudServiceProvider().getServicesAsUniqueIdAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServiceByNameAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServiceByName(String)}
     */
    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.getCloudServiceProvider().getCloudServiceByNameAsync(name);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync()}
     */
    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.getCloudServiceProvider().getCloudServicesAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getStartedCloudServicesAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getStartedCloudServicesAsync()}
     */
    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServiceInfoSnapshotsAsync() {
        return this.getCloudServiceProvider().getStartedCloudServicesAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        return this.getCloudServiceProvider().getCloudServicesAsync(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesByGroupAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesByGroupAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        return this.getCloudServiceProvider().getCloudServicesByGroupAsync(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Integer> getServicesCountAsync() {
        return this.getCloudServiceProvider().getServicesCountAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByGroupAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByGroupAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        return this.getCloudServiceProvider().getServicesCountByGroupAsync(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByTaskAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByTaskAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        return this.getCloudServiceProvider().getServicesCountByTaskAsync(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceInfoSnapshot> getCloudServicesAsync(UUID uniqueId) {
        return this.getCloudServiceProvider().getCloudServiceAsync(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync(ServiceEnvironmentType)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync(ServiceEnvironmentType)}
     */
    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        return this.getCloudServiceProvider().getCloudServicesAsync(environment);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#getPermanentServiceTasks()
     * @deprecated moved to {@link ServiceTaskProvider#getPermanentServiceTasks()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return this.getServiceTaskProvider().getPermanentServiceTasks();
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#getServiceTask(String)
     * @deprecated moved to {@link ServiceTaskProvider#getServiceTask(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public ServiceTask getServiceTask(String name) {
        return this.getServiceTaskProvider().getServiceTask(name);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#isServiceTaskPresent(String)
     * @deprecated moved to {@link ServiceTaskProvider#isServiceTaskPresent(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public boolean isServiceTaskPresent(String name) {
        return this.getServiceTaskProvider().isServiceTaskPresent(name);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#addPermanentServiceTask(ServiceTask)
     * @deprecated moved to {@link ServiceTaskProvider#addPermanentServiceTask(ServiceTask)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        this.getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#removePermanentServiceTask(String)
     * @deprecated moved to {@link ServiceTaskProvider#removePermanentServiceTask(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void removePermanentServiceTask(String name) {
        this.getServiceTaskProvider().removePermanentServiceTask(name);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#removePermanentServiceTask(ServiceTask)
     * @deprecated moved to {@link ServiceTaskProvider#removePermanentServiceTask(ServiceTask)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        this.getServiceTaskProvider().removePermanentServiceTask(serviceTask);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#getPermanentServiceTasksAsync()
     * @deprecated moved to {@link ServiceTaskProvider#getPermanentServiceTasksAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.getServiceTaskProvider().getPermanentServiceTasksAsync();
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#getServiceTaskAsync(String)
     * @deprecated moved to {@link ServiceTaskProvider#getServiceTaskAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        return this.getServiceTaskProvider().getServiceTaskAsync(name);
    }

    /**
     * @see #getServiceTaskProvider()
     * @see ServiceTaskProvider#isServiceTaskPresentAsync(String)
     * @deprecated moved to {@link ServiceTaskProvider#isServiceTaskPresentAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        return this.getServiceTaskProvider().isServiceTaskPresentAsync(name);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#getGroupConfigurations()
     * @deprecated moved to {@link GroupConfigurationProvider#getGroupConfigurations()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return this.getGroupConfigurationProvider().getGroupConfigurations();
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#getGroupConfiguration(String)
     * @deprecated moved to {@link GroupConfigurationProvider#getGroupConfiguration(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public GroupConfiguration getGroupConfiguration(String name) {
        return this.getGroupConfigurationProvider().getGroupConfiguration(name);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#isGroupConfigurationPresent(String)
     * @deprecated moved to {@link GroupConfigurationProvider#isGroupConfigurationPresent(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public boolean isGroupConfigurationPresent(String name) {
        return this.getGroupConfigurationProvider().isGroupConfigurationPresent(name);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#addGroupConfiguration(GroupConfiguration)
     * @deprecated moved to {@link GroupConfigurationProvider#addGroupConfiguration(GroupConfiguration)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.getGroupConfigurationProvider().addGroupConfiguration(groupConfiguration);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#removeGroupConfiguration(String)
     * @deprecated moved to {@link GroupConfigurationProvider#removeGroupConfiguration(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void removeGroupConfiguration(String name) {
        this.getGroupConfigurationProvider().removeGroupConfiguration(name);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#removeGroupConfiguration(GroupConfiguration)
     * @deprecated moved to {@link GroupConfigurationProvider#removeGroupConfiguration(GroupConfiguration)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.getGroupConfigurationProvider().removeGroupConfiguration(groupConfiguration);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#getGroupConfigurationsAsync()
     * @deprecated moved to {@link GroupConfigurationProvider#getGroupConfigurationsAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.getGroupConfigurationProvider().getGroupConfigurationsAsync();
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#getGroupConfigurationAsync(String)
     * @deprecated moved to {@link GroupConfigurationProvider#getGroupConfigurationAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        return this.getGroupConfigurationProvider().getGroupConfigurationAsync(name);
    }

    /**
     * @see #getGroupConfigurationProvider()
     * @see GroupConfigurationProvider#isGroupConfigurationPresentAsync(String)
     * @deprecated moved to {@link GroupConfigurationProvider#isGroupConfigurationPresentAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        return this.getGroupConfigurationProvider().isGroupConfigurationPresentAsync(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#addUser(IPermissionUser)
     * @deprecated moved to {@link IPermissionManagement#addUser(IPermissionUser)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void addUser(IPermissionUser permissionUser) {
        this.getPermissionManagement().addUser(permissionUser);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#updateUser(IPermissionUser)
     * @deprecated moved to {@link IPermissionManagement#updateUser(IPermissionUser)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void updateUser(IPermissionUser permissionUser) {
        this.getPermissionManagement().updateUser(permissionUser);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#deleteGroup(String)
     * @deprecated moved to {@link IPermissionManagement#deleteGroup(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deleteUser(String name) {
        this.getPermissionManagement().deleteUser(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#deleteUser(IPermissionUser)
     * @deprecated moved to {@link IPermissionManagement#deleteUser(IPermissionUser)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deleteUser(IPermissionUser permissionUser) {
        this.getPermissionManagement().deleteUser(permissionUser);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsUser(UUID)
     * @deprecated moved to {@link IPermissionManagement#containsUser(UUID)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public boolean containsUser(UUID uniqueId) {
        return this.getPermissionManagement().containsUser(uniqueId);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsUser(String)
     * @deprecated moved to {@link IPermissionManagement#containsUser(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public boolean containsUser(String name) {
        return this.getPermissionManagement().containsUser(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUser(UUID)
     * @deprecated moved to {@link IPermissionManagement#getUser(UUID)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public IPermissionUser getUser(UUID uniqueId) {
        return this.getPermissionManagement().getUser(uniqueId);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsers(String)
     * @deprecated moved to {@link IPermissionManagement#getUsers(String)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public List<IPermissionUser> getUser(String name) {
        return this.getPermissionManagement().getUsers(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsers()
     * @deprecated moved to {@link IPermissionManagement#getUsers()}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public Collection<IPermissionUser> getUsers() {
        return this.getPermissionManagement().getUsers();
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#setUsers(Collection)
     * @deprecated moved to {@link IPermissionManagement#setUsers(Collection)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void setUsers(Collection<? extends IPermissionUser> users) {
        this.getPermissionManagement().setUsers(users);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsersByGroup(String)
     * @deprecated moved to {@link IPermissionManagement#getUsersByGroup(String)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public Collection<IPermissionUser> getUserByGroup(String group) {
        return this.getPermissionManagement().getUsersByGroup(group);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#addGroup(IPermissionGroup)
     * @deprecated moved to {@link IPermissionManagement#addGroup(IPermissionGroup)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void addGroup(IPermissionGroup permissionGroup) {
        this.getPermissionManagement().addGroup(permissionGroup);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#updateGroup(IPermissionGroup)
     * @deprecated moved to {@link IPermissionManagement#updateGroup(IPermissionGroup)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void updateGroup(IPermissionGroup permissionGroup) {
        this.getPermissionManagement().updateGroup(permissionGroup);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#deleteGroup(String)
     * @deprecated moved to {@link IPermissionManagement#deleteGroup(String)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public void deleteGroup(String group) {
        this.getPermissionManagement().deleteGroup(group);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#deleteGroup(IPermissionGroup)
     * @deprecated moved to {@link IPermissionManagement#deleteGroup(IPermissionGroup)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void deleteGroup(IPermissionGroup group) {
        this.getPermissionManagement().deleteGroup(group);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsGroup(String)
     * @deprecated moved to {@link IPermissionManagement#containsGroup(String)}
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @Deprecated
    public boolean containsGroup(String group) {
        return this.getPermissionManagement().containsGroup(group);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getGroup(String)
     * @deprecated moved to {@link IPermissionManagement#getGroup(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public IPermissionGroup getGroup(String name) {
        return this.getPermissionManagement().getGroup(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getGroups()
     * @deprecated moved to {@link IPermissionManagement#getGroups()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public Collection<IPermissionGroup> getGroups() {
        return this.getPermissionManagement().getGroups();
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#setGroups(Collection)
     * @deprecated moved to {@link IPermissionManagement#setGroups(Collection)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        this.getPermissionManagement().setGroups(groups);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#addUserAsync(IPermissionUser)
     * @deprecated moved to {@link IPermissionManagement#addUserAsync(IPermissionUser)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Void> addUserAsync(IPermissionUser permissionUser) {
        return ((ListenableTask<IPermissionUser>) this.getPermissionManagement().addUserAsync(permissionUser)).map(user -> null);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsUserAsync(UUID)
     * @deprecated moved to {@link IPermissionManagement#containsUserAsync(UUID)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Boolean> containsUserAsync(UUID uniqueId) {
        return this.getPermissionManagement().containsUserAsync(uniqueId);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsUserAsync(String)
     * @deprecated moved to {@link IPermissionManagement#containsUserAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Boolean> containsUserAsync(String name) {
        return this.getPermissionManagement().containsUserAsync(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUserAsync(UUID)
     * @deprecated moved to {@link IPermissionManagement#getUserAsync(UUID)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<IPermissionUser> getUserAsync(UUID uniqueId) {
        return this.getPermissionManagement().getUserAsync(uniqueId);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsersAsync(String)
     * @deprecated moved to {@link IPermissionManagement#getUsersAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<List<IPermissionUser>> getUserAsync(String name) {
        return this.getPermissionManagement().getUsersAsync(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsersAsync()
     * @deprecated moved to {@link IPermissionManagement#getUsersAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.getPermissionManagement().getUsersAsync();
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getUsersByGroupAsync(String)
     * @deprecated moved to {@link IPermissionManagement#getUsersByGroupAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<IPermissionUser>> getUserByGroupAsync(String group) {
        return this.getPermissionManagement().getUsersByGroupAsync(group);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#containsGroupAsync(String)
     * @deprecated moved to {@link IPermissionManagement#containsGroupAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Boolean> containsGroupAsync(String name) {
        return this.getPermissionManagement().containsGroupAsync(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getGroupAsync(String)
     * @deprecated moved to {@link IPermissionManagement#getGroupAsync(String)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<IPermissionGroup> getGroupAsync(String name) {
        return this.getPermissionManagement().getGroupAsync(name);
    }

    /**
     * @see #getPermissionManagement()
     * @see IPermissionManagement#getGroupsAsync()
     * @deprecated moved to {@link IPermissionManagement#getGroupsAsync()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.getPermissionManagement().getGroupsAsync();
    }


    public abstract Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine);

    @NotNull
    public abstract ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine);

    /**
     * @see #getPacketQueryProvider()
     * @see PacketQueryProvider#sendCallablePacket(INetworkChannel, String, String, JsonDocument, Function)
     * @deprecated moved to {@link PacketQueryProvider#sendCallablePacket(INetworkChannel, String, String, JsonDocument, Function)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, String id, JsonDocument data, Function<JsonDocument, R> function) {
        Preconditions.checkNotNull(networkChannel);
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(function);

        return this.getPacketQueryProvider().sendCallablePacket(networkChannel, channel, id, data, function);
    }

    /**
     * @see #getPacketQueryProvider()
     * @see PacketQueryProvider#sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(JsonDocument, byte[], Function)
     * @deprecated moved to {@link PacketQueryProvider#sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(JsonDocument, byte[], Function)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(header, body, function);
    }

    /**
     * @see #getPacketQueryProvider()
     * @see PacketQueryProvider#sendCallablePacketWithAsDriverSyncAPI(INetworkChannel, JsonDocument, byte[], Function)
     * @deprecated moved to {@link PacketQueryProvider#sendCallablePacketWithAsDriverSyncAPI(INetworkChannel, JsonDocument, byte[], Function)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPI(INetworkChannel channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(channel, header, body, function);
    }

    /**
     * @see #getPacketQueryProvider()
     * @see PacketQueryProvider#sendCallablePacket(INetworkChannel, String, JsonDocument, byte[], Function)
     * @deprecated moved to {@link PacketQueryProvider#sendCallablePacket(INetworkChannel, String, JsonDocument, byte[], Function)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.4")
    @NotNull
    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.getPacketQueryProvider().sendCallablePacket(networkChannel, channel, header, body, function);
    }

    @NotNull
    public PacketQueryProvider getPacketQueryProvider() {
        return packetQueryProvider;
    }

    @NotNull
    public IServicesRegistry getServicesRegistry() {
        return this.servicesRegistry;
    }

    @NotNull
    public IEventManager getEventManager() {
        return this.eventManager;
    }

    @NotNull
    public IModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    @NotNull
    public ITaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull
    public ILogger getLogger() {
        return this.logger;
    }

    @NotNull
    public DriverEnvironment getDriverEnvironment() {
        return this.driverEnvironment;
    }

}
