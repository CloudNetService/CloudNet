package de.dytanic.cloudnet.wrapper;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.wrapper.conf.DocumentWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.conf.IWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import de.dytanic.cloudnet.wrapper.database.defaults.DefaultWrapperDatabaseProvider;
import de.dytanic.cloudnet.wrapper.event.ApplicationEnvironmentEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPostStartEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPreStartEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.module.WrapperModuleProviderHandler;
import de.dytanic.cloudnet.wrapper.network.NetworkClientChannelHandler;
import de.dytanic.cloudnet.wrapper.network.listener.*;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientServiceInfoUpdate;
import de.dytanic.cloudnet.wrapper.runtime.RuntimeApplicationClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * This class is the main class of the application wrapper, which performs the basic
 * driver functions and the setup of the application to be wrapped.
 *
 * @see CloudNetDriver
 */
public final class Wrapper extends CloudNetDriver {

    private static final int TPS = 10;

    /**
     * The configuration of the wrapper, which was created from the CloudNet node.
     * The properties are mirrored from the configuration file.
     *
     * @see IWrapperConfiguration
     */
    private final IWrapperConfiguration config = new DocumentWrapperConfiguration();

    /**
     * The default workDirectory of this process as File instance
     */
    private final File workDirectory = new File(".");


    /**
     * The commandline arguments from the main() method of Main class by the application wrapper
     */
    private final List<String> commandLineArguments;

    /**
     * CloudNetDriver.getNetworkClient()
     *
     * @see CloudNetDriver
     */
    private final INetworkClient networkClient;

    private IDatabaseProvider databaseProvider = new DefaultWrapperDatabaseProvider();


    private final Queue<ITask<?>> processQueue = Iterables.newConcurrentLinkedQueue();


    /**
     * The single task thread of the scheduler of the wrapper application
     */
    private final Thread mainThread = Thread.currentThread();
    private final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    /*= ---------------------------------------------------------- =*/
    /**
     * The ServiceInfoSnapshot instances. The current ServiceInfoSnapshot instance is the last send object snapshot
     * from this process. The lastServiceInfoSnapshot is the element which was send before.
     */
    private ServiceInfoSnapshot
            lastServiceInfoSnapShot = this.config.getServiceInfoSnapshot(),
            currentServiceInfoSnapshot = this.config.getServiceInfoSnapshot();

    Wrapper(List<String> commandLineArguments, ILogger logger) {
        super(logger);
        setInstance(this);

        this.commandLineArguments = commandLineArguments;

        if (this.config.getSslConfig().getBoolean("enabled"))
            this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, new SSLConfiguration(
                    this.config.getSslConfig().getBoolean("clientAuth"),
                    this.config.getSslConfig().contains("trustCertificatePath") ?
                            new File(".wrapper/trustCertificate") :
                            null,
                    this.config.getSslConfig().contains("certificatePath") ?
                            new File(".wrapper/certificate") :
                            null,
                    this.config.getSslConfig().contains("privateKeyPath") ?
                            new File(".wrapper/privateKey") :
                            null
            ), taskScheduler);
        else
            this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new);

        //- Packet client registry
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerServiceInfoPublisherListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerUpdatePermissionsListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerChannelMessageListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerClusterNodeInfoUpdateListener());
        //-

        this.moduleProvider.setModuleProviderHandler(new WrapperModuleProviderHandler());
        this.driverEnvironment = DriverEnvironment.WRAPPER;
    }

    public static Wrapper getInstance() {
        return (Wrapper) CloudNetDriver.getInstance();
    }

    @Override
    public synchronized void start() throws Exception {
        this.enableModules();

        ReentrantLock lock = new ReentrantLock();
        PacketServerAuthorizationResponseListener listener;

        try {
            lock.lock();

            Condition condition = lock.newCondition();
            listener = new PacketServerAuthorizationResponseListener(lock, condition);

            this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, listener);
            this.networkClient.connect(this.config.getTargetListener());

            condition.await();

        } finally {
            lock.unlock();
        }

        this.networkClient.getPacketRegistry().removeListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL);

        if (!listener.isResult()) throw new IllegalStateException("authorization response is: denied");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        this.start0();
    }

    @Override
    public void stop() {
        try {
            this.networkClient.close();
            this.logger.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.taskScheduler.shutdown();
        this.moduleProvider.unloadAll();
        this.eventManager.unregisterAll();
        this.servicesRegistry.unregisterAll();
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public String[] sendCommandLine(String commandLine) {
        Validate.checkNotNull(commandLine);

        try {
            return this.sendCommandLineAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public String[] sendCommandLine(String nodeUniqueId, String commandLine) {
        Validate.checkNotNull(nodeUniqueId, commandLine);

        try {
            return this.sendCommandLineAsync(nodeUniqueId, commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void sendChannelMessage(String channel, String message, JsonDocument data) {
        Validate.checkNotNull(channel);
        Validate.checkNotNull(message);
        Validate.checkNotNull(data);

        this.networkClient.sendPacket(new PacketClientServerChannelMessage(channel, message, data));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        try {
            return this.createCloudServiceAsync(serviceTask).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    //port can be null

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Validate.checkNotNull(serviceConfiguration);

        try {
            return this.createCloudServiceAsync(serviceConfiguration).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot createCloudService(String name, String runtime, boolean autoDeleteOnStop, boolean staticService, Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments, Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        try {
            return this.createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService, Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments, Collection<String> groups,
                                                              ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        try {
            return this.createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        try {
            return this.sendCommandLineToCloudServiceAsync(uniqueId, commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        try {
            return this.addServiceTemplateToCloudServiceAsync(uniqueId, serviceTemplate).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        try {
            return this.addServiceRemoteInclusionToCloudServiceAsync(uniqueId, serviceRemoteInclusion).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        try {
            return this.addServiceDeploymentToCloudServiceAsync(uniqueId, serviceDeployment).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Queue<String> getCachedLogMessagesFromService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getCachedLogMessagesFromServiceAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(lifeCycle);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "set_service_life_cycle").append("serviceInfoSnapshot", serviceInfoSnapshot).append("lifeCycle", lifeCycle), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "restart_cloud_service").append("serviceInfoSnapshot", serviceInfoSnapshot),
                null,
                documentPair -> null
        );
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "kill_cloud_service").append("serviceInfoSnapshot", serviceInfoSnapshot),
                null,
                documentPair -> null
        );
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(command);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "run_command_cloud_service").append("serviceInfoSnapshot", serviceInfoSnapshot)
                        .append("command", command),
                null,
                documentPair -> null
        );
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<UUID> getServicesAsUniqueId() {
        try {
            return this.getServicesAsUniqueIdAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getCloudServiceByNameAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        try {
            return this.getCloudServicesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        try {
            return this.getStartedCloudServiceInfoSnapshotsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> getCloudService(String taskName) {
        Validate.checkNotNull(taskName);

        try {
            return this.getCloudServicesAsync(taskName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> getCloudServiceByGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return this.getCloudServicesByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getCloudServicesAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Integer getServicesCount() {
        try {
            return this.getServicesCountAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Integer getServicesCountByGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return this.getServicesCountByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Integer getServicesCountByTask(String taskName) {
        Validate.checkNotNull(taskName);

        try {
            return this.getServicesCountByTaskAsync(taskName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        try {
            return this.getPermanentServiceTasksAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
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

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
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

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_permanent_service_task").append("serviceTask", serviceTask), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void removePermanentServiceTask(String name) {
        Validate.checkNotNull(name);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "remove_permanent_service_task").append("name", name), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);
        this.removePermanentServiceTask(serviceTask.getName());
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        try {
            return this.getGroupConfigurationsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public GroupConfiguration getGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getGroupConfigurationAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public boolean isGroupConfigurationPresent(String name) {
        Validate.checkNotNull(name);

        try {
            return this.isGroupConfigurationPresentAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_group_configuration").append("groupConfiguration", groupConfiguration), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void removeGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "remove_group_configuration").append("name", name), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);
        this.removeGroupConfiguration(groupConfiguration.getName());
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public NetworkClusterNode[] getNodes() {
        try {
            return this.getNodesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public NetworkClusterNode getNode(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getNodeAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
        try {
            return this.getNodeInfoSnapshotsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getNodeInfoSnapshotAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
        try {
            return this.getLocalTemplateStorageTemplatesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        try {
            return this.getCloudServicesAsync(environment).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceTemplate> getTemplateStorageTemplates(String serviceName) {
        Validate.checkNotNull(serviceName);

        try {
            return this.getTemplateStorageTemplatesAsync(serviceName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        try {
            return this.sendCommandLineAsPermissionUserAsync(uniqueId, commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        addUserAsync(permissionUser);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void deleteUser(String name) {
        Validate.checkNotNull(name);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user_with_name").append("name", name), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return containsUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        try {
            return containsUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return getUserAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public List<IPermissionUser> getUser(String name) {
        Validate.checkNotNull(name);

        try {
            return getUserAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<IPermissionUser> getUsers() {
        try {
            return getUsersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void setUsers(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_users").append("permissionUsers", users), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<IPermissionUser> getUserByGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return getUserByGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_update_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void deleteGroup(String group) {
        Validate.checkNotNull(group);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group_with_name").append("name", group), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void deleteGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_delete_group").append("permissionGroup", permissionGroup), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public boolean containsGroup(String group) {
        Validate.checkNotNull(group);

        try {
            return containsGroupAsync(group).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        try {
            return getGroupAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<IPermissionGroup> getGroups() {
        try {
            return getGroupsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    //=- ------------------------------------------------------------------------------

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_set_groups").append("permissionGroups", groups), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<String[]> sendCommandLineAsync(String commandLine) {
        Validate.checkNotNull(commandLine);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine").append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine) {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine_on_node")
                        .append("nodeUniqueId", nodeUniqueId)
                        .append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceTask").append("serviceTask", serviceTask), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        Validate.checkNotNull(serviceConfiguration);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceConfiguration").append("serviceConfiguration", serviceConfiguration), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name, String runtime, boolean autoDeleteOnStop, boolean staticService, Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom")
                        .append("name", name)
                        .append("runtime", runtime)
                        .append("autoDeleteOnStop", autoDeleteOnStop)
                        .append("staticService", staticService)
                        .append("includes", includes)
                        .append("templates", templates)
                        .append("deployments", deployments)
                        .append("groups", groups)
                        .append("processConfiguration", processConfiguration)
                        .append("properties", properties)
                        .append("port", port),
                null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                                          Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                                                                          Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom_selected_node_and_amount")
                        .append("nodeUniqueId", nodeUniqueId)
                        .append("amount", amount)
                        .append("name", name)
                        .append("runtime", runtime)
                        .append("autoDeleteOnStop", autoDeleteOnStop)
                        .append("staticService", staticService)
                        .append("includes", includes)
                        .append("templates", templates)
                        .append("deployments", deployments)
                        .append("groups", groups)
                        .append("processConfiguration", processConfiguration)
                        .append("properties", properties)
                        .append("port", port),
                null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> sendCommandLineToCloudServiceAsync(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandline_to_cloud_service")
                        .append("uniqueId", uniqueId)
                        .append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> addServiceTemplateToCloudServiceAsync(UUID uniqueId, ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_template_to_cloud_service")
                        .append("uniqueId", uniqueId)
                        .append("serviceTemplate", serviceTemplate), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> addServiceRemoteInclusionToCloudServiceAsync(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_remote_inclusion_to_cloud_service")
                        .append("uniqueId", uniqueId)
                        .append("serviceRemoteInclusion", serviceRemoteInclusion), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> addServiceDeploymentToCloudServiceAsync(UUID uniqueId, ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_deployment_to_cloud_service")
                        .append("uniqueId", uniqueId)
                        .append("serviceDeployment", serviceDeployment), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Queue<String>> getCachedLogMessagesFromServiceAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cached_log_messages_from_service")
                        .append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("cachedLogMessages", new TypeToken<Queue<String>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void includeWaitingServiceTemplates(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_templates").append("uniqueId", uniqueId), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void includeWaitingServiceInclusions(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_inclusions").append("uniqueId", uniqueId), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public void deployResources(UUID uniqueId, boolean removeDeployments) {
        Validate.checkNotNull(uniqueId);

        sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "deploy_resources_from_service")
                        .append("uniqueId", uniqueId).append("removeDeployments", removeDeployments), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_as_uuid"), null,
                documentPair -> documentPair.getFirst().get("serviceUniqueIds", new TypeToken<Collection<UUID>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudService_by_name").append("name", name), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos"), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServiceInfoSnapshotsAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_started"), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_taskName").append("taskName", taskName), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_group").append("group", group), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Integer> getServicesCountAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count"), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count_by_group").append("group", group), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_services_count_by_task").append("taskName", taskName), null,
                documentPair -> documentPair.getFirst().getInt("servicesCount"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceInfoSnapshot> getCloudServicesAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloudServiceInfos_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_permanent_serviceTasks"), null,
                documentPair -> documentPair.getFirst().get("serviceTasks", new TypeToken<Collection<ServiceTask>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_service_task").append("name", name), null,
                documentPair -> documentPair.getFirst().get("serviceTask", new TypeToken<ServiceTask>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "is_service_task_present").append("name", name), null,
                documentPair -> documentPair.getFirst().get("result", new TypeToken<Boolean>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_groupConfigurations"), null,
                documentPair -> documentPair.getFirst().get("groupConfigurations", new TypeToken<Collection<GroupConfiguration>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_group_configuration").append("name", name), null,
                documentPair -> documentPair.getFirst().get("groupConfiguration", new TypeToken<GroupConfiguration>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "is_group_configuration_present").append("name", name), null,
                documentPair -> documentPair.getFirst().get("result", new TypeToken<Boolean>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_nodes"), null,
                documentPair -> documentPair.getFirst().get("nodes", new TypeToken<NetworkClusterNode[]>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<NetworkClusterNode> getNodeAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNode", new TypeToken<NetworkClusterNode>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshots"), null,
                documentPair -> documentPair.getFirst().get("nodeInfoSnapshots", new TypeToken<NetworkClusterNodeInfoSnapshot[]>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshot_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNodeInfoSnapshot", new TypeToken<NetworkClusterNodeInfoSnapshot>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_local_template_storage_templates"), null,
                documentPair -> documentPair.getFirst().get("templates", new TypeToken<Collection<ServiceTemplate>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cloud_services_with_environment").append("serviceEnvironment", environment), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(String serviceName) {
        Validate.checkNotNull(serviceName);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_template_storage_templates").append("serviceName", serviceName), null,
                documentPair -> documentPair.getFirst().get("templates", new TypeToken<Collection<ServiceTemplate>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandline_as_permission_user").append("uniqueId", uniqueId).append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("executionResponse", new TypeToken<Pair<Boolean, String[]>>() {
                }.getType()));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Void> addUserAsync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_add_user").append("permissionUser", permissionUser), null,
                VOID_FUNCTION);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Boolean> containsUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Boolean> containsUserAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_user_with_name").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<IPermissionUser> getUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_uuid").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("permissionUser", PermissionUser.TYPE));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<List<IPermissionUser>> getUserAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_user_by_name").append("name", name), null,
                documentPair -> {
                    List<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users"), null,
                documentPair -> {
                    Collection<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<IPermissionUser>> getUserByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_users_by_group").append("group", group), null,
                documentPair -> {
                    List<IPermissionUser> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                    }.getType()));

                    return collection;
                });
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Boolean> containsGroupAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_contains_group").append("name", name), null,
                documentPair -> documentPair.getFirst().getBoolean("result"));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<IPermissionGroup> getGroupAsync(String name) {
        Validate.checkNotNull(name);

        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_group").append("name", name), null,
                documentPair -> documentPair.getFirst().get("permissionGroup", PermissionGroup.TYPE));
    }


    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "permission_management_get_groups"), null,
                documentPair -> {
                    List<IPermissionGroup> collection = Iterables.newArrayList();
                    collection.addAll(documentPair.getFirst().get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
                    }.getType(), Iterables.newArrayList()));

                    return collection;
                });
    }

    /**
     * This method invokes a Runnable instance one the wrapper main thread at the next tick.
     *
     * @param runnable the task, that you want to invoke
     * @param <T>      the type of the result, that you invoke
     * @return a ITask object instance, which you can add additional listeners
     * @see ITask
     */
    public <T> ITask<T> runTask(Callable<T> runnable) {
        Validate.checkNotNull(runnable);

        ITask<T> task = new ListenableTask<>(runnable);

        this.processQueue.offer(task);
        return task;
    }

    /**
     * This method invokes a Runnable instance one the wrapper main thread at the next tick.
     *
     * @param runnable the task, that you want to invoke
     * @return a ITask object instance, which you can add additional listeners
     * @see ITask
     */
    public ITask<?> runTask(Runnable runnable) {
        Validate.checkNotNull(runnable);

        return this.runTask(Executors.callable(runnable));
    }

    /**
     * Is an shortcut for Wrapper.getConfig().getServiceId()
     *
     * @return the ServiceId instance which was set in the config by the node
     */
    public ServiceId getServiceId() {
        return config.getServiceConfiguration().getServiceId();
    }

    /**
     * Is an shortcut for Wrapper.getConfig().getServiceConfiguration()
     *
     * @return the first instance which was set in the config by the node
     */
    public ServiceConfiguration getServiceConfiguration() {
        return config.getServiceConfiguration();
    }

    /**
     * Creates a completed new ServiceInfoSnapshot instance based of the properties of the current ServiceInfoSnapshot instance
     *
     * @return the new ServiceInfoSnapshot instance
     */
    public ServiceInfoSnapshot createServiceInfoSnapshot() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        return new ServiceInfoSnapshot(
                System.currentTimeMillis(),
                this.getServiceId(),
                this.currentServiceInfoSnapshot.getAddress(),
                true,
                ServiceLifeCycle.RUNNING,
                new ProcessSnapshot(
                        memoryMXBean.getHeapMemoryUsage().getUsed(),
                        memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                        memoryMXBean.getHeapMemoryUsage().getMax(),
                        ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
                        ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
                        ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount(),
                        Iterables.map(Thread.getAllStackTraces().keySet(), thread -> new ThreadSnapshot(thread.getId(), thread.getName(), thread.getState(), thread.isDaemon(), thread.getPriority())),
                        CPUUsageResolver.getProcessCPUUsage()
                ),
                this.currentServiceInfoSnapshot.getProperties(),
                this.getServiceConfiguration()
        );
    }

    /**
     * This method should be used to send the current ServiceInfoSnapshot and all subscribers on the network and to update their information.
     * It calls the ServiceInfoSnapshotConfigureEvent before send the update to the node.
     *
     * @see ServiceInfoSnapshotConfigureEvent
     */
    public void publishServiceInfoUpdate() {
        publishServiceInfoUpdate(this.createServiceInfoSnapshot());
    }

    public void publishServiceInfoUpdate(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.eventManager.callEvent(new ServiceInfoSnapshotConfigureEvent(serviceInfoSnapshot));

        this.lastServiceInfoSnapShot = this.currentServiceInfoSnapshot;
        this.currentServiceInfoSnapshot = serviceInfoSnapshot;

        this.networkClient.sendPacket(new PacketClientServiceInfoUpdate(serviceInfoSnapshot));
    }


    /**
     * Removes all PacketListeners from all channels of the Network Connctor from a
     * specific ClassLoader. It is recommended to do this with the disables of your own plugin
     *
     * @param classLoader the ClassLoader from which the IPacketListener implementations derive.
     */
    public void unregisterPacketListenersByClassLoader(ClassLoader classLoader) {
        networkClient.getPacketRegistry().removeListeners(classLoader);

        for (INetworkChannel channel : networkClient.getChannels())
            channel.getPacketRegistry().removeListeners(classLoader);
    }

    private synchronized void start0() throws Exception {
        long value = System.currentTimeMillis();
        long millis = 1000 / TPS;
        int tps5 = TPS * 5, start1Tick = tps5;

        if (this.startApplication()) {
            Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    long diff = System.currentTimeMillis() - value;
                    if (diff < millis)
                        try {
                            Thread.sleep(millis - diff);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                    value = System.currentTimeMillis();

                    eventManager.callEvent(new CloudNetTickEvent());

                    while (!this.processQueue.isEmpty())
                        if (this.processQueue.peek() != null) Objects.requireNonNull(this.processQueue.poll()).call();
                        else this.processQueue.poll();

                    if (start1Tick++ >= tps5) {
                        this.start1();
                        start1Tick = 0;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        this.stop();
        System.exit(0);
    }

    private void start1() {
        this.publishServiceInfoUpdate();
    }

    private void enableModules() {
        File dir = new File(System.getProperty("cloudnet.module.dir", ".wrapper/modules"));
        dir.mkdirs();

        File[] files = dir.listFiles(pathname -> {
            String lowerName = pathname.getName().toLowerCase();
            return !pathname.isDirectory() && lowerName.endsWith(".jar") ||
                    lowerName.endsWith(".war") ||
                    lowerName.endsWith(".zip");
        });

        if (files != null)
            for (File file : files) {
                IModuleWrapper moduleWrapper = this.moduleProvider.loadModule(file);
                moduleWrapper.startModule();
            }
    }

    private boolean startApplication() throws Exception {
        ApplicationEnvironmentEvent preStartApplicationEvent = new ApplicationEnvironmentEvent(this, this.config.getServiceConfiguration().getServiceId().getEnvironment());
        this.eventManager.callEvent(preStartApplicationEvent);

        File[] files = this.workDirectory.listFiles();
        if (files == null) {
            this.logger.log(LogLevel.ERROR,
                    "",
                    new FileNotFoundException("no files found!"));
            return false;
        }

        File entry = null;
        ServiceEnvironment environment = null;

        for (ServiceEnvironment subEnvironment : preStartApplicationEvent.getEnvironmentType().getEnvironments()) {
            if (entry != null) {
                break;
            }

            entry = this.find(files, file -> {
                String lowerName = file.getName().toLowerCase();
                return file.exists() && !file.isDirectory() && lowerName.endsWith(".jar") &&
                        lowerName.contains(subEnvironment.getName().toLowerCase());
            });

            environment = subEnvironment;
        }

        try {
            return this.startApplication0(entry, environment);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
        return false;
    }

    private File find(File[] files, Predicate<File> predicate) throws Exception {
        return Iterables.first(files, predicate);
    }

    private boolean startApplication0(File file, ServiceEnvironment serviceEnvironment) throws Exception {
        if (file == null || !file.exists()) {
            Collection<String> availableFileNames = Arrays.stream(getServiceId().getEnvironment().getEnvironments())
                    .map(subEnvironment -> subEnvironment.getName() + ".jar")
                    .collect(Collectors.toSet());

            this.logger.log(LogLevel.ERROR,
                    "",
                    new FileNotFoundException("Application file for Runtime "
                            + getServiceId().getEnvironment()
                            + " COULD NOT BE FOUND! Please include a file with one of the following names: "
                            + String.join(", ", availableFileNames)));
            return false;
        }

        JarFile jarFile = new JarFile(file);
        Manifest manifest = jarFile.getManifest();
        String mainClazz = jarFile.getManifest().getMainAttributes().getValue("Main-Class");

        RuntimeApplicationClassLoader appClassLoader = new RuntimeApplicationClassLoader(
                ClassLoader.getSystemClassLoader(),
                file.toURI().toURL(),
                ClassLoader.getSystemClassLoader().getParent()
        );

        Field field = ClassLoader.class.getDeclaredField("scl");
        field.setAccessible(true);
        field.set(null, appClassLoader);

        Class<?> main = Class.forName(mainClazz, true, ClassLoader.getSystemClassLoader());
        Method method = main.getMethod("main", String[].class);
        method.setAccessible(true);

        Collection<String> arguments = Iterables.newArrayList(this.commandLineArguments);

        this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, manifest, arguments));

        Thread applicationThread = new Thread(() -> {
            try {
                logger.info("Starting Application-Thread based of " + Wrapper.this.getServiceConfiguration().getProcessConfig().getEnvironment() + ":" + serviceEnvironment + "\n");
                method.invoke(null, new Object[]{arguments.toArray(new String[0])});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Application-Thread");

        applicationThread.setContextClassLoader(appClassLoader);
        applicationThread.start();

        eventManager.callEvent(new ApplicationPostStartEvent(this, main, applicationThread, appClassLoader));
        return true;
    }

    public IWrapperConfiguration getConfig() {
        return this.config;
    }

    public File getWorkDirectory() {
        return this.workDirectory;
    }

    public List<String> getCommandLineArguments() {
        return this.commandLineArguments;
    }

    public INetworkClient getNetworkClient() {
        return this.networkClient;
    }

    public Thread getMainThread() {
        return this.mainThread;
    }

    public ServiceInfoSnapshot getLastServiceInfoSnapShot() {
        return this.lastServiceInfoSnapShot;
    }

    public ServiceInfoSnapshot getCurrentServiceInfoSnapshot() {
        return this.currentServiceInfoSnapshot;
    }

    public void setDatabaseProvider(IDatabaseProvider databaseProvider) {
        Validate.checkNotNull(databaseProvider);
        this.databaseProvider = databaseProvider;
    }

    public IDatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }
}