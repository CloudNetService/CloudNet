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
import de.dytanic.cloudnet.driver.service.provider.*;

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


    public abstract CloudServiceFactory getCloudServiceFactory();

    public abstract ServiceTaskProvider getServiceTaskProvider();

    public abstract GroupConfigurationProvider getGroupConfigurationProvider();

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param name the name of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(String name);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param uniqueId the uniqueId of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(UUID uniqueId);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param serviceInfoSnapshot the info of the service to create a provider for
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(ServiceInfoSnapshot serviceInfoSnapshot);

    /**
     * Returns the general CloudServiceProvider
     * @return the instance of the {@link GeneralCloudServiceProvider}
     */
    public abstract GeneralCloudServiceProvider getCloudServiceProvider();

    public abstract INetworkClient getNetworkClient();

    public abstract Collection<CommandInfo> getConsoleCommands();

    public abstract CommandInfo getConsoleCommand(String commandLine);

    public abstract String[] sendCommandLine(String commandLine);

    public abstract String[] sendCommandLine(String nodeUniqueId, String commandLine);

    public abstract void sendChannelMessage(String channel, String message, JsonDocument data);

    public abstract void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data);

    public abstract void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data);

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(ServiceTask)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(ServiceTask)}
     */
    @Deprecated
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        return this.getCloudServiceFactory().createCloudService(serviceTask);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(ServiceConfiguration)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(ServiceConfiguration)}
     */
    @Deprecated
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        return this.getCloudServiceFactory().createCloudService(serviceConfiguration);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudService(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
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
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return this.getCloudServiceFactory().createCloudServiceAsync(serviceTask);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(ServiceConfiguration)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(ServiceConfiguration)}
     */
    @Deprecated
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        return this.getCloudServiceFactory().createCloudServiceAsync(serviceConfiguration);
    }

    /**
     * @see #getCloudServiceFactory()
     * @see CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)
     * @deprecated moved to {@link CloudServiceFactory#createCloudServiceAsync(String, String, boolean, boolean, Collection, Collection, Collection, Collection, ProcessConfiguration, Integer)}
     */
    @Deprecated
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
    public Queue<String> getCachedLogMessagesFromService(UUID uniqueId) {
        return this.getCloudServiceProvider(uniqueId).getCachedLogMessages();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#stop()
     * @deprecated moved to {@link SpecificCloudServiceProvider#stop()}
     */
    @Deprecated
    public void stopCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.STOPPED);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#start()
     * @deprecated moved to {@link SpecificCloudServiceProvider#start()}
     */
    @Deprecated
    public void startCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#delete()
     * @deprecated moved to {@link SpecificCloudServiceProvider#delete()}
     */
    @Deprecated
    public void deleteCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.DELETED);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#setCloudServiceLifeCycle(ServiceLifeCycle)
     * @deprecated moved to {@link SpecificCloudServiceProvider#setCloudServiceLifeCycle(ServiceLifeCycle)}
     */
    @Deprecated
    public void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(serviceInfoSnapshot);

        this.getCloudServiceProvider(serviceInfoSnapshot.getServiceId().getUniqueId()).setCloudServiceLifeCycle(lifeCycle);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#restart()
     * @deprecated moved to {@link SpecificCloudServiceProvider#restart()}
     */
    @Deprecated
    public void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        this.getCloudServiceProvider(serviceInfoSnapshot.getServiceId().getUniqueId()).restart();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#kill()
     * @deprecated moved to {@link SpecificCloudServiceProvider#kill()}
     */
    @Deprecated
    public void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        this.getCloudServiceProvider(serviceInfoSnapshot.getServiceId().getUniqueId()).kill();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#runCommand(String)
     * @deprecated moved to {@link SpecificCloudServiceProvider#runCommand(String)}
     */
    @Deprecated
    public void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command) {
        Validate.checkNotNull(serviceInfoSnapshot);

        this.getCloudServiceProvider(serviceInfoSnapshot.getServiceId().getUniqueId()).runCommand(command);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#runCommand(String)
     * @deprecated moved to {@link SpecificCloudServiceProvider#runCommand(String)}
     */
    @Deprecated
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
    public ITask<Queue<String>> getCachedLogMessagesFromServiceAsync(UUID uniqueId) {
        return this.getCloudServiceProvider(uniqueId).getCachedLogMessagesAsync();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#includeWaitingServiceTemplates()
     * @deprecated moved to {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}
     */
    @Deprecated
    public void includeWaitingServiceTemplates(UUID uniqueId) {
        this.getCloudServiceProvider(uniqueId).includeWaitingServiceTemplates();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#includeWaitingServiceInclusions()
     * @deprecated moved to {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}
     */
    @Deprecated
    public void includeWaitingServiceInclusions(UUID uniqueId) {
        this.getCloudServiceProvider(uniqueId).includeWaitingServiceInclusions();
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#deployResources(boolean)
     * @deprecated moved to {@link SpecificCloudServiceProvider#deployResources(boolean)}
     */
    @Deprecated
    public void deployResources(UUID uniqueId, boolean removeDeployments) {
        this.getCloudServiceProvider(uniqueId).deployResources(removeDeployments);
    }

    /**
     * @see #getCloudServiceProvider(UUID)
     * @see SpecificCloudServiceProvider#deployResources()
     * @deprecated moved to {@link SpecificCloudServiceProvider#deployResources()}
     */
    @Deprecated
    public void deployResources(UUID uniqueId) {
        this.deployResources(uniqueId, true);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesAsUniqueId()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesAsUniqueId()}
     */
    @Deprecated
    public Collection<UUID> getServicesAsUniqueId() {
        return this.getCloudServiceProvider().getServicesAsUniqueId();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServiceByName(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServiceByName(String)}
     */
    @Deprecated
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        return this.getCloudServiceProvider().getCloudServiceByName(name);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServices()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServices()}
     */
    @Deprecated
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        return this.getCloudServiceProvider().getCloudServices();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getStartedCloudServices()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getStartedCloudServices()}
     */
    @Deprecated
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        return this.getCloudServiceProvider().getStartedCloudServices();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServices(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServices(String)}
     */
    @Deprecated
    public Collection<ServiceInfoSnapshot> getCloudService(String taskName) {
        return this.getCloudServiceProvider().getCloudServices(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesByGroup(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesByGroup(String)}
     */
    @Deprecated
    public Collection<ServiceInfoSnapshot> getCloudServiceByGroup(String group) {
        return this.getCloudServiceProvider().getCloudServicesByGroup(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudService(UUID)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudService(UUID)}
     */
    @Deprecated
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        return this.getCloudServiceProvider().getCloudService(uniqueId);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCount()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCount()}
     */
    @Deprecated
    public Integer getServicesCount() {
        return this.getCloudServiceProvider().getServicesCount();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByGroup(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByGroup(String)}
     */
    @Deprecated
    public Integer getServicesCountByGroup(String group) {
        return this.getCloudServiceProvider().getServicesCountByGroup(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByTask(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByTask(String)}
     */
    @Deprecated
    public Integer getServicesCountByTask(String taskName) {
        return this.getCloudServiceProvider().getServicesCountByTask(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesAsUniqueIdAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesAsUniqueIdAsync()}
     */
    @Deprecated
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.getCloudServiceProvider().getServicesAsUniqueIdAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServiceByNameAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServiceByName(String)}
     */
    @Deprecated
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.getCloudServiceProvider().getCloudServiceByNameAsync(name);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync()}
     */
    @Deprecated
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.getCloudServiceProvider().getCloudServicesAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getStartedCloudServicesAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getStartedCloudServicesAsync()}
     */
    @Deprecated
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServiceInfoSnapshotsAsync() {
        return this.getCloudServiceProvider().getStartedCloudServicesAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync(String)}
     */
    @Deprecated
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        return this.getCloudServiceProvider().getCloudServicesAsync(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesByGroupAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesByGroupAsync(String)}
     */
    @Deprecated
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        return this.getCloudServiceProvider().getCloudServicesByGroupAsync(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountAsync()
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountAsync()}
     */
    @Deprecated
    public ITask<Integer> getServicesCountAsync() {
        return this.getCloudServiceProvider().getServicesCountAsync();
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByGroupAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByGroupAsync(String)}
     */
    @Deprecated
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        return this.getCloudServiceProvider().getServicesCountByGroupAsync(group);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getServicesCountByTaskAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getServicesCountByTaskAsync(String)}
     */
    @Deprecated
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        return this.getCloudServiceProvider().getServicesCountByTaskAsync(taskName);
    }

    /**
     * @see #getCloudServiceProvider()
     * @see GeneralCloudServiceProvider#getCloudServicesAsync(String)
     * @deprecated moved to {@link GeneralCloudServiceProvider#getCloudServicesAsync(String)}
     */
    @Deprecated
    public ITask<ServiceInfoSnapshot> getCloudServicesAsync(UUID uniqueId) {
        return this.getCloudServiceProvider().getCloudServiceAsync(uniqueId);
    }

    @Deprecated
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return this.getServiceTaskProvider().getPermanentServiceTasks();
    }

    @Deprecated
    public ServiceTask getServiceTask(String name) {
        return this.getServiceTaskProvider().getServiceTask(name);
    }

    @Deprecated
    public boolean isServiceTaskPresent(String name) {
        return this.getServiceTaskProvider().isServiceTaskPresent(name);
    }

    @Deprecated
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        this.getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    }

    @Deprecated
    public void removePermanentServiceTask(String name) {
        this.getServiceTaskProvider().removePermanentServiceTask(name);
    }

    @Deprecated
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        this.getServiceTaskProvider().removePermanentServiceTask(serviceTask);
    }

    @Deprecated
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.getServiceTaskProvider().getPermanentServiceTasksAsync();
    }

    @Deprecated
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        return this.getServiceTaskProvider().getServiceTaskAsync(name);
    }

    @Deprecated
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        return this.getServiceTaskProvider().isServiceTaskPresentAsync(name);
    }

    @Deprecated
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return this.getGroupConfigurationProvider().getGroupConfigurations();
    }

    @Deprecated
    public GroupConfiguration getGroupConfiguration(String name) {
        return this.getGroupConfigurationProvider().getGroupConfiguration(name);
    }

    @Deprecated
    public boolean isGroupConfigurationPresent(String name) {
        return this.getGroupConfigurationProvider().isGroupConfigurationPresent(name);
    }

    @Deprecated
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.getGroupConfigurationProvider().addGroupConfiguration(groupConfiguration);
    }

    @Deprecated
    public void removeGroupConfiguration(String name) {
        this.getGroupConfigurationProvider().removeGroupConfiguration(name);
    }

    @Deprecated
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.getGroupConfigurationProvider().removeGroupConfiguration(groupConfiguration);
    }

    @Deprecated
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.getGroupConfigurationProvider().getGroupConfigurationsAsync();
    }

    @Deprecated
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        return this.getGroupConfigurationProvider().getGroupConfigurationAsync(name);
    }

    @Deprecated
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        return this.getGroupConfigurationProvider().isGroupConfigurationPresentAsync(name);
    }

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