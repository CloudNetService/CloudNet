package de.dytanic.cloudnet.wrapper;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.module.DefaultModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.wrapper.conf.DocumentWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.conf.IWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import de.dytanic.cloudnet.wrapper.database.defaults.DefaultWrapperDatabaseProvider;
import de.dytanic.cloudnet.wrapper.event.ApplicationPostStartEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPreStartEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.network.NetworkClientChannelHandler;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerAuthorizationResponseListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerChannelMessageListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerServiceInfoPublisherListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerSetGlobalLogLevelListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerUpdatePermissionsListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerWrapperDriverAPIListener;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientServiceInfoUpdate;
import de.dytanic.cloudnet.wrapper.permission.WrapperPermissionManagement;
import de.dytanic.cloudnet.wrapper.provider.WrapperGroupConfigurationProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperMessenger;
import de.dytanic.cloudnet.wrapper.provider.WrapperNodeInfoProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperServiceTaskProvider;
import de.dytanic.cloudnet.wrapper.provider.service.WrapperGeneralCloudServiceProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class of the application wrapper, which performs the basic
 * driver functions and the setup of the application to be wrapped.
 *
 * @see CloudNetDriver
 */
public final class Wrapper extends CloudNetDriver implements DriverAPIUser {
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
    private final Path workDirectory = Paths.get("");

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

        super.cloudServiceFactory = new RemoteCloudServiceFactory(this::getNetworkChannel);
        super.generalCloudServiceProvider = new WrapperGeneralCloudServiceProvider(this);
        super.serviceTaskProvider = new WrapperServiceTaskProvider(this);
        super.groupConfigurationProvider = new WrapperGroupConfigurationProvider(this);
        super.nodeInfoProvider = new WrapperNodeInfoProvider(this);
        super.messenger = new WrapperMessenger(this);

        this.commandLineArguments = commandLineArguments;

        if (this.config.getSslConfig().getBoolean("enabled")) {
            this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, new SSLConfiguration(
                    this.config.getSslConfig().getBoolean("clientAuth"),
                    this.config.getSslConfig().contains("trustCertificatePath")
                            ? Paths.get(".wrapper", "trustCertificate")
                            : null,
                    this.config.getSslConfig().contains("certificatePath")
                            ? Paths.get(".wrapper", "certificate")
                            : null,
                    this.config.getSslConfig().contains("privateKeyPath")
                            ? Paths.get(".wrapper", "privateKey")
                            : null
            ), this.taskScheduler);
        } else {
            this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new);
        }

        super.setPermissionManagement(new WrapperPermissionManagement(this));

        //- Packet client registry
        this.networkClient.getPacketRegistry().addListener(PacketConstants.SERVICE_INFO_PUBLISH_CHANNEL, new PacketServerServiceInfoPublisherListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL, new PacketServerUpdatePermissionsListener());
        this.networkClient.getPacketRegistry().addListener(PacketConstants.CHANNEL_MESSAGING_CHANNEL, new PacketServerChannelMessageListener());

        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_DEBUGGING_CHANNEL, new PacketServerSetGlobalLogLevelListener());

        this.networkClient.getPacketRegistry().addListener(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, new PacketServerWrapperDriverAPIListener());
        //-

        this.moduleProvider.setModuleDirectoryPath(Paths.get(".wrapper", "modules"));
        this.moduleProvider.setModuleProviderHandler(new DefaultModuleProviderHandler());
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

        this.permissionManagement.init();

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

    @Override
    public @NotNull String getComponentName() {
        return this.getServiceId().getName();
    }

    @Override
    public @NotNull String getNodeUniqueId() {
        return this.getServiceId().getNodeUniqueId();
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
        return new RemoteSpecificCloudServiceProvider(this.getNetworkChannel(), this.generalCloudServiceProvider, name);
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
        return new RemoteSpecificCloudServiceProvider(this.getNetworkChannel(), this.generalCloudServiceProvider, uniqueId);
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return new RemoteSpecificCloudServiceProvider(this.getNetworkChannel(), serviceInfoSnapshot);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
        return this.getLocalTemplateStorageTemplatesAsync().get(5, TimeUnit.SECONDS, null);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    public Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        return this.getTemplateStorageTemplatesAsync(serviceName).get(5, TimeUnit.SECONDS, null);
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
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(commandLine);

        return this.sendCommandLineAsPermissionUserAsync(uniqueId, commandLine).get(5, TimeUnit.SECONDS, null);
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return this.getTemplateStorageTemplatesAsync("local");
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_TEMPLATE_STORAGE_TEMPLATES,
                buffer -> buffer.writeString(serviceName),
                packet -> packet.getBuffer().readObjectCollection(ServiceTemplate.class)
        );
    }

    /**
     * Application wrapper implementation of this method. See the full documentation at the
     * CloudNetDriver class.
     *
     * @see CloudNetDriver
     */
    @Override
    @NotNull
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(commandLine);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.SEND_COMMAND_LINE_AS_PERMISSION_USER,
                buffer -> buffer.writeUUID(uniqueId).writeString(commandLine),
                packet -> new Pair<>(packet.getBuffer().readBoolean(), packet.getBuffer().readStringArray())
        );
    }


    /**
     * Is an shortcut for Wrapper.getConfig().getServiceId()
     *
     * @return the ServiceId instance which was set in the config by the node
     */
    public ServiceId getServiceId() {
        return this.config.getServiceConfiguration().getServiceId();
    }

    /**
     * Is an shortcut for Wrapper.getConfig().getServiceConfiguration()
     *
     * @return the first instance which was set in the config by the node
     */
    public ServiceConfiguration getServiceConfiguration() {
        return this.config.getServiceConfiguration();
    }

    /**
     * Creates a completed new ServiceInfoSnapshot instance based of the properties of the current ServiceInfoSnapshot instance
     *
     * @return the new ServiceInfoSnapshot instance
     */
    @NotNull
    public ServiceInfoSnapshot createServiceInfoSnapshot() {
        return new ServiceInfoSnapshot(
                System.currentTimeMillis(),
                this.currentServiceInfoSnapshot.getAddress(),
                this.currentServiceInfoSnapshot.getConnectAddress(),
                this.networkClient.getConnectedTime(),
                ServiceLifeCycle.RUNNING,
                ProcessSnapshot.self(),
                this.currentServiceInfoSnapshot.getProperties(),
                this.getServiceConfiguration()
        );
    }

    @ApiStatus.Internal
    public ServiceInfoSnapshot configureServiceInfoSnapshot() {
        ServiceInfoSnapshot serviceInfoSnapshot = this.createServiceInfoSnapshot();
        this.configureServiceInfoSnapshot(serviceInfoSnapshot);
        return serviceInfoSnapshot;
    }

    private void configureServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.eventManager.callEvent(new ServiceInfoSnapshotConfigureEvent(serviceInfoSnapshot));

        this.lastServiceInfoSnapShot = this.currentServiceInfoSnapshot;
        this.currentServiceInfoSnapshot = serviceInfoSnapshot;
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
        if (this.currentServiceInfoSnapshot.getServiceId().equals(serviceInfoSnapshot.getServiceId())) {
            this.configureServiceInfoSnapshot(serviceInfoSnapshot);
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
        this.networkClient.getPacketRegistry().removeListeners(classLoader);

        for (INetworkChannel channel : this.networkClient.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }
    }

    private void enableModules() {
        Path moduleDirectory = Paths.get(System.getProperty("cloudnet.module.dir", ".wrapper/modules"));
        FileUtils.createDirectoryReported(moduleDirectory);
        FileUtils.walkFileTree(moduleDirectory, (root, current) -> {
            IModuleWrapper wrapper = this.moduleProvider.loadModule(current);
            wrapper.startModule();
        }, false, "*.{jar,war,zip}");
    }

    private boolean startApplication() throws Exception {
        String mainClass = this.commandLineArguments.remove(0);

        return this.startApplication(mainClass);
    }

    private boolean startApplication(@NotNull String mainClass) throws Exception {
        Class<?> main = Class.forName(mainClass);
        Method method = main.getMethod("main", String[].class);

        Collection<String> arguments = new ArrayList<>(this.commandLineArguments);

        this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, arguments));

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
                this.logger.info("Starting Application-Thread based of " + Wrapper.this.getServiceConfiguration().getProcessConfig().getEnvironment() + "\n");
                method.invoke(null, new Object[]{arguments.toArray(new String[0])});
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, "Application-Thread");
        applicationThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        applicationThread.start();

        this.eventManager.callEvent(new ApplicationPostStartEvent(this, main, applicationThread, ClassLoader.getSystemClassLoader()));
        return true;
    }

    @NotNull
    public IWrapperConfiguration getConfig() {
        return this.config;
    }

    @NotNull
    @Deprecated
    public File getWorkDirectory() {
        return this.workDirectory.toFile();
    }

    @NotNull
    public Path getWorkingDirectoryPath() {
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

    /**
     * @deprecated use {@link CloudNetDriver#getDatabaseProvider()} instead
     */
    @NotNull
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public IDatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public void setDatabaseProvider(@NotNull IDatabaseProvider databaseProvider) {
        Preconditions.checkNotNull(databaseProvider);
        this.databaseProvider = databaseProvider;
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.networkClient.getFirstChannel();
    }
}
