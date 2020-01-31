package de.dytanic.cloudnet.wrapper;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.PacketQueryProvider;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.provider.*;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.wrapper.conf.DocumentWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.conf.IWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import de.dytanic.cloudnet.wrapper.database.defaults.DefaultWrapperDatabaseProvider;
import de.dytanic.cloudnet.wrapper.event.ApplicationPostStartEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPreStartEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.module.WrapperModuleProviderHandler;
import de.dytanic.cloudnet.wrapper.network.NetworkClientChannelHandler;
import de.dytanic.cloudnet.wrapper.network.listener.*;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientServiceInfoUpdate;
import de.dytanic.cloudnet.wrapper.provider.*;
import de.dytanic.cloudnet.wrapper.provider.service.WrapperCloudServiceFactory;
import de.dytanic.cloudnet.wrapper.provider.service.WrapperGeneralCloudServiceProvider;
import de.dytanic.cloudnet.wrapper.provider.service.WrapperSpecificCloudServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class of the application wrapper, which performs the basic
 * driver functions and the setup of the application to be wrapped.
 *
 * @see CloudNetDriver
 */
public final class Wrapper extends CloudNetDriver {
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

    private CloudServiceFactory cloudServiceFactory = new WrapperCloudServiceFactory(this);
    private GeneralCloudServiceProvider generalCloudServiceProvider = new WrapperGeneralCloudServiceProvider(this);
    private ServiceTaskProvider serviceTaskProvider = new WrapperServiceTaskProvider(this);
    private GroupConfigurationProvider groupConfigurationProvider = new WrapperGroupConfigurationProvider(this);
    private PermissionProvider permissionProvider = new WrapperPermissionProvider(this);
    private NodeInfoProvider nodeInfoProvider = new WrapperNodeInfoProvider(this);
    private CloudMessenger messenger = new WrapperMessenger(this);

    /**
     * CloudNetDriver.getNetworkClient()
     *
     * @see CloudNetDriver
     */
    private final INetworkClient networkClient;
    /**
     * The single task thread of the scheduler of the wrapper application
     */
    private final Thread mainThread = Thread.currentThread();
    private IDatabaseProvider databaseProvider = new DefaultWrapperDatabaseProvider();
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

        if (this.config.getSslConfig().getBoolean("enabled")) {
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
        } else {
            this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new);
        }
        super.packetQueryProvider = new PacketQueryProvider(this.networkClient);

        //- Packet client registry
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerServiceInfoPublisherListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerUpdatePermissionsListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerChannelMessageListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerClusterNodeInfoUpdateListener());

        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_DEBUGGING_CHANNEL, new PacketServerSetGlobalLogLevelListener());
        //-

        this.moduleProvider.setModuleDirectory(new File(".wrapper/modules"));
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

        if (!listener.isResult()) {
            throw new IllegalStateException("authorization response is: denied");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        if (!this.startApplication()) {
            System.exit(-1);
        }
    }

    @Override
    public void stop() {
        try {
            this.networkClient.close();
            this.logger.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        this.taskScheduler.shutdown();
        this.moduleProvider.unloadAll();
        this.eventManager.unregisterAll();
        this.servicesRegistry.unregisterAll();
    }

    @NotNull
    @Override
    public PermissionProvider getPermissionProvider() {
        return this.permissionProvider;
    }

    @NotNull
    @Override
    public CloudServiceFactory getCloudServiceFactory() {
        return this.cloudServiceFactory;
    }

    @NotNull
    @Override
    public ServiceTaskProvider getServiceTaskProvider() {
        return this.serviceTaskProvider;
    }

    @NotNull
    @Override
    public NodeInfoProvider getNodeInfoProvider() {
        return this.nodeInfoProvider;
    }

    @NotNull
    @Override
    public GroupConfigurationProvider getGroupConfigurationProvider() {
        return this.groupConfigurationProvider;
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
        return new WrapperSpecificCloudServiceProvider(this, name);
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
        return new WrapperSpecificCloudServiceProvider(this, uniqueId);
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return new WrapperSpecificCloudServiceProvider(this, serviceInfoSnapshot);
    }

    @NotNull
    @Override
    public GeneralCloudServiceProvider getCloudServiceProvider() {
        return this.generalCloudServiceProvider;
    }

    @NotNull
    @Override
    public CloudMessenger getMessenger() {
        return this.messenger;
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
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
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
    public Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName) {
        Validate.checkNotNull(serviceName);

        try {
            return this.getTemplateStorageTemplatesAsync(serviceName).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public void setGlobalLogLevel(@NotNull LogLevel logLevel) {
        this.setGlobalLogLevel(logLevel.getLevel());
    }

    @Override
    public void setGlobalLogLevel(int logLevel) {
        this.networkClient.sendPacket(new PacketServerSetGlobalLogLevel(logLevel));
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        try {
            return this.sendCommandLineAsPermissionUserAsync(uniqueId, commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
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
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
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
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
        Validate.checkNotNull(serviceName);

        return getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
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
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        return getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandline_as_permission_user").append("uniqueId", uniqueId).append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("executionResponse", new TypeToken<Pair<Boolean, String[]>>() {
                }.getType()));
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
    @NotNull
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
                        CPUUsageResolver.getProcessCPUUsage(),
                        this.getOwnPID()
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
    public synchronized void publishServiceInfoUpdate() {
        this.publishServiceInfoUpdate(this.createServiceInfoSnapshot());
    }

    public synchronized void publishServiceInfoUpdate(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        if (currentServiceInfoSnapshot.getServiceId().equals(serviceInfoSnapshot.getServiceId())) {
            this.eventManager.callEvent(new ServiceInfoSnapshotConfigureEvent(serviceInfoSnapshot));

            this.lastServiceInfoSnapShot = this.currentServiceInfoSnapshot;
            this.currentServiceInfoSnapshot = serviceInfoSnapshot;
        }

        this.networkClient.sendPacket(new PacketClientServiceInfoUpdate(serviceInfoSnapshot));
    }


    /**
     * Removes all PacketListeners from all channels of the Network Connctor from a
     * specific ClassLoader. It is recommended to do this with the disables of your own plugin
     *
     * @param classLoader the ClassLoader from which the IPacketListener implementations derive.
     */
    public void unregisterPacketListenersByClassLoader(@NotNull ClassLoader classLoader) {
        networkClient.getPacketRegistry().removeListeners(classLoader);

        for (INetworkChannel channel : networkClient.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }
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

        if (files != null) {
            for (File file : files) {
                IModuleWrapper moduleWrapper = this.moduleProvider.loadModule(file);
                moduleWrapper.startModule();
            }
        }
    }

    private boolean startApplication() throws Exception {
        File applicationFile = new File(this.commandLineArguments.remove(0));
        String mainClass = this.commandLineArguments.remove(0);

        return applicationFile.exists() && this.startApplication(applicationFile, mainClass);
    }

    private boolean startApplication(@NotNull File applicationFile, @NotNull String mainClass) throws Exception {
        Class<?> main = Class.forName(mainClass);
        Method method = main.getMethod("main", String[].class);

        Collection<String> arguments = Iterables.newArrayList(this.commandLineArguments);

        this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, applicationFile, arguments));

        try {
            // checking if the application will be launched via the Minecraft LaunchWrapper
            Class.forName("net.minecraft.launchwrapper.Launch");

            // adds a tweak class to the LaunchWrapper which will prevent doubled loading of the CloudNet classes
            arguments.add("--tweakClass");
            arguments.add("de.dytanic.cloudnet.wrapper.tweak.CloudNetTweaker");
        } catch (ClassNotFoundException exception) {
            // the LaunchWrapper is not available, doing nothing
        }

        Thread applicationThread = new Thread(() -> {
            try {
                logger.info("Starting Application-Thread based of " + Wrapper.this.getServiceConfiguration().getProcessConfig().getEnvironment() + "\n");
                method.invoke(null, new Object[]{arguments.toArray(new String[0])});
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, "Application-Thread");
        applicationThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        applicationThread.start();

        eventManager.callEvent(new ApplicationPostStartEvent(this, main, applicationThread, ClassLoader.getSystemClassLoader()));
        return true;
    }

    @NotNull
    public IWrapperConfiguration getConfig() {
        return this.config;
    }

    @NotNull
    public File getWorkDirectory() {
        return this.workDirectory;
    }

    @NotNull
    public List<String> getCommandLineArguments() {
        return this.commandLineArguments;
    }

    @NotNull
    public INetworkClient getNetworkClient() {
        return this.networkClient;
    }

    @NotNull
    public Thread getMainThread() {
        return this.mainThread;
    }

    @NotNull
    public ServiceInfoSnapshot getLastServiceInfoSnapShot() {
        return this.lastServiceInfoSnapShot;
    }

    @NotNull
    public ServiceInfoSnapshot getCurrentServiceInfoSnapshot() {
        return this.currentServiceInfoSnapshot;
    }

    @NotNull
    public IDatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public void setDatabaseProvider(@NotNull IDatabaseProvider databaseProvider) {
        Validate.checkNotNull(databaseProvider);
        this.databaseProvider = databaseProvider;
    }
}