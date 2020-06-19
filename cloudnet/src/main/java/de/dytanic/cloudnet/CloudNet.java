package de.dytanic.cloudnet;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.cluster.DefaultClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.DefaultCommandMap;
import de.dytanic.cloudnet.command.ICommandMap;
import de.dytanic.cloudnet.command.commands.*;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.conf.IConfigurationRegistry;
import de.dytanic.cloudnet.conf.JsonConfiguration;
import de.dytanic.cloudnet.conf.JsonConfigurationRegistry;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.util.HeaderReader;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.DefaultDatabaseHandler;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.module.DefaultPersistableModuleDependencyLoader;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.event.CloudNetNodePostInitializationEvent;
import de.dytanic.cloudnet.event.cluster.NetworkClusterNodeInfoConfigureEvent;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;
import de.dytanic.cloudnet.event.command.CommandPostProcessEvent;
import de.dytanic.cloudnet.event.command.CommandPreProcessEvent;
import de.dytanic.cloudnet.event.permission.PermissionServiceSetEvent;
import de.dytanic.cloudnet.log.QueuedConsoleLogHandler;
import de.dytanic.cloudnet.module.NodeModuleProviderHandler;
import de.dytanic.cloudnet.network.NetworkClientChannelHandlerImpl;
import de.dytanic.cloudnet.network.NetworkServerChannelHandlerImpl;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.network.listener.PacketServerChannelMessageListener;
import de.dytanic.cloudnet.network.listener.auth.PacketClientAuthorizationListener;
import de.dytanic.cloudnet.network.listener.auth.PacketServerAuthorizationResponseListener;
import de.dytanic.cloudnet.network.listener.cluster.*;
import de.dytanic.cloudnet.network.listener.driver.PacketServerDriverAPIListener;
import de.dytanic.cloudnet.network.packet.*;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.permission.DefaultPermissionManagementHandler;
import de.dytanic.cloudnet.permission.NodePermissionManagement;
import de.dytanic.cloudnet.permission.command.DefaultPermissionUserCommandSender;
import de.dytanic.cloudnet.permission.command.IPermissionUserCommandSender;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import de.dytanic.cloudnet.provider.NodeMessenger;
import de.dytanic.cloudnet.provider.NodeNodeInfoProvider;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import de.dytanic.cloudnet.provider.service.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.provider.service.LocalNodeSpecificCloudServiceProvider;
import de.dytanic.cloudnet.provider.service.NodeCloudServiceFactory;
import de.dytanic.cloudnet.provider.service.NodeGeneralCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.defaults.DefaultCloudServiceManager;
import de.dytanic.cloudnet.setup.DefaultInstallation;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class CloudNet extends CloudNetDriver {

    public static final int TPS = 10;
    private static CloudNet instance;

    private final CloudNetTick mainLoop = new CloudNetTick(this);

    private final LogLevel defaultLogLevel = LogLevel.getDefaultLogLevel(System.getProperty("cloudnet.logging.defaultlevel")).orElse(LogLevel.FATAL);

    private final File moduleDirectory = new File(System.getProperty("cloudnet.modules.directory", "modules"));

    private final IConfiguration config = new JsonConfiguration();
    private final IConfigurationRegistry configurationRegistry = new JsonConfigurationRegistry(Paths.get(System.getProperty("cloudnet.registry.global.path", "local/registry")));

    private final IClusterNodeServerProvider clusterNodeServerProvider = new DefaultClusterNodeServerProvider();

    private final ITaskScheduler networkTaskScheduler = new DefaultTaskScheduler();

    private final List<String> commandLineArguments;
    private final Properties commandLineProperties;

    private final IConsole console;
    private final ICommandMap commandMap = new DefaultCommandMap();

    private final QueuedConsoleLogHandler queuedConsoleLogHandler;
    private final ConsoleCommandSender consoleCommandSender;

    private INetworkClient networkClient;
    private INetworkServer networkServer;
    private IHttpServer httpServer;
    private IPermissionManagement permissionManagement;

    private final ICloudServiceManager cloudServiceManager = new DefaultCloudServiceManager();
    private final CloudServiceFactory cloudServiceFactory = new NodeCloudServiceFactory(this);
    private final GeneralCloudServiceProvider generalCloudServiceProvider = new NodeGeneralCloudServiceProvider(this);

    private final ServiceTaskProvider serviceTaskProvider = new NodeServiceTaskProvider(this);
    private final GroupConfigurationProvider groupConfigurationProvider = new NodeGroupConfigurationProvider(this);
    private final NodeInfoProvider nodeInfoProvider = new NodeNodeInfoProvider(this);
    private final CloudMessenger messenger = new NodeMessenger(this);

    private final DefaultInstallation defaultInstallation = new DefaultInstallation();

    private final ServiceVersionProvider serviceVersionProvider = new ServiceVersionProvider();

    private AbstractDatabaseProvider databaseProvider;
    private volatile NetworkClusterNodeInfoSnapshot lastNetworkClusterNodeInfoSnapshot, currentNetworkClusterNodeInfoSnapshot;

    private volatile boolean running = true;

    CloudNet(List<String> commandLineArguments, ILogger logger, IConsole console) {
        super(logger);
        setInstance(this);

        logger.setLevel(this.defaultLogLevel);

        this.console = console;
        this.commandLineArguments = commandLineArguments;
        this.commandLineProperties = Properties.parseLine(commandLineArguments.toArray(new String[0]));

        this.consoleCommandSender = new ConsoleCommandSender(logger);

        logger.addLogHandler(this.queuedConsoleLogHandler = new QueuedConsoleLogHandler());

        this.serviceTaskProvider.reload();
        this.groupConfigurationProvider.reload();

        this.moduleProvider.setModuleProviderHandler(new NodeModuleProviderHandler());
        this.moduleProvider.setModuleDependencyLoader(new DefaultPersistableModuleDependencyLoader(Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"), "libs")));

        this.driverEnvironment = DriverEnvironment.CLOUDNET;
    }

    public static CloudNet getInstance() {
        if (instance == null) {
            instance = (CloudNet) CloudNetDriver.getInstance();
        }

        return instance;
    }

    @Override
    public synchronized void start() throws Exception {
        File tempDirectory = new File(System.getProperty("cloudnet.tempDir", "temp"));
        tempDirectory.mkdirs();

        new File(tempDirectory, "caches").mkdir();

        try (InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("wrapper.jar")) {
            Files.copy(inputStream, new File(tempDirectory, "caches/wrapper.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        this.initServiceVersions();

        boolean configFileAvailable = this.config.isFileExists();
        this.config.load();
        this.defaultInstallation.executeFirstStartSetup(this.console, configFileAvailable);

        HeaderReader.readAndPrintHeader(this.console);

        if (this.config.getMaxMemory() < 2048) {
            CloudNetDriver.getInstance().getLogger().warning(LanguageManager.getMessage("cloudnet-init-config-low-memory-warning"));
        }

        this.networkClient = new NettyNetworkClient(NetworkClientChannelHandlerImpl::new,
                this.config.getClientSslConfig().isEnabled() ? this.config.getClientSslConfig().toSslConfiguration() : null,
                this.networkTaskScheduler
        );

        this.networkServer = new NettyNetworkServer(NetworkServerChannelHandlerImpl::new,
                this.config.getClientSslConfig().isEnabled() ? this.config.getServerSslConfig().toSslConfiguration() : null,
                this.networkTaskScheduler
        );
        this.httpServer = new NettyHttpServer(this.config.getClientSslConfig().isEnabled() ? this.config.getWebSslConfig().toSslConfiguration() : null);

        this.initPacketRegistryListeners();
        this.clusterNodeServerProvider.setClusterServers(this.config.getClusterConfig());

        this.enableCommandCompleter();
        this.setDefaultRegistryEntries();

        this.registerDefaultCommands();
        this.registerDefaultServices();

        this.currentNetworkClusterNodeInfoSnapshot = this.createClusterNodeInfoSnapshot();
        this.lastNetworkClusterNodeInfoSnapshot = this.currentNetworkClusterNodeInfoSnapshot;

        this.loadModules();

        this.databaseProvider = this.servicesRegistry.getService(AbstractDatabaseProvider.class,
                this.configurationRegistry.getString("database_provider", "h2"));

        if (this.databaseProvider == null) {
            this.stop();
        }

        this.databaseProvider.setDatabaseHandler(new DefaultDatabaseHandler());

        if (!this.databaseProvider.init() && !(this.databaseProvider instanceof H2DatabaseProvider)) {
            this.databaseProvider = this.servicesRegistry.getService(AbstractDatabaseProvider.class, "h2");
            this.databaseProvider.init();
        }

        NodePermissionManagement permissionManagement = new DefaultDatabasePermissionManagement(this::getDatabaseProvider);
        permissionManagement.init();
        permissionManagement.setPermissionManagementHandler(new DefaultPermissionManagementHandler());
        this.permissionManagement = permissionManagement;

        this.startModules();
        this.eventManager.callEvent(new PermissionServiceSetEvent(this.permissionManagement));

        this.setNetworkListeners();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));

        //setup implementations
        this.defaultInstallation.initDefaultPermissionGroups();
        this.defaultInstallation.postExecute();

        this.eventManager.callEvent(new CloudNetNodePostInitializationEvent());

        this.runConsole();
        this.mainLoop.start();
    }

    private void setNetworkListeners() {
        Random random = new Random();
        for (NetworkClusterNode node : this.config.getClusterConfig().getNodes()) {
            if (!this.networkClient.connect(node.getListeners()[random.nextInt(node.getListeners().length)])) {
                this.logger.log(LogLevel.WARNING, LanguageManager.getMessage("cluster-server-networking-connection-refused"));
            }
        }

        for (HostAndPort hostAndPort : this.config.getIdentity().getListeners()) {
            this.logger.info(LanguageManager.getMessage("cloudnet-network-server-bind").replace("%address%",
                    hostAndPort.getHost() + ":" + hostAndPort.getPort()));

            this.networkServer.addListener(hostAndPort);
        }

        for (HostAndPort hostAndPort : this.config.getHttpListeners()) {
            this.logger.info(LanguageManager.getMessage("cloudnet-http-server-bind").replace("%address%",
                    hostAndPort.getHost() + ":" + hostAndPort.getPort()));

            this.httpServer.addListener(hostAndPort);
        }
    }

    public void reload() {
        this.logger.info(LanguageManager.getMessage("reload-start-message"));

        this.config.load();
        this.getConfigurationRegistry().load();
        this.clusterNodeServerProvider.setClusterServers(this.config.getClusterConfig());

        this.serviceTaskProvider.reload();
        this.groupConfigurationProvider.reload();

        this.unloadAll();

        this.initServiceVersions();

        this.enableModules();

        this.logger.info(LanguageManager.getMessage("reload-end-message"));
    }

    @Override
    public void stop() {
        if (this.running) {
            this.running = false;
        } else {
            return;
        }

        this.logger.info(LanguageManager.getMessage("stop-start-message"));

        this.serviceVersionProvider.shutdown();

        this.cloudServiceManager.deleteAllCloudServices();
        this.taskScheduler.shutdown();

        this.unloadAll();
        this.unloadAllModules0();

        try {
            if (this.databaseProvider != null) {
                try {
                    this.databaseProvider.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            this.logger.info(LanguageManager.getMessage("stop-network-client"));
            this.networkClient.close();

            this.logger.info(LanguageManager.getMessage("stop-network-server"));
            this.networkServer.close();

            this.logger.info(LanguageManager.getMessage("stop-http-server"));
            this.httpServer.close();

            this.networkTaskScheduler.shutdown();

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        FileUtils.delete(new File("temp"));

        if (!Thread.currentThread().getName().equals("Shutdown Thread")) {
            System.exit(0);
        }
    }

    @Override
    public @NotNull String getComponentName() {
        return this.config.getIdentity().getUniqueId();
    }

    public boolean isRunning() {
        return this.running;
    }

    public LogLevel getDefaultLogLevel() {
        return this.defaultLogLevel;
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
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
        ServiceInfoSnapshot snapshot = this.generalCloudServiceProvider.getCloudServiceByName(name);
        return this.selectCloudServiceProvider(snapshot);
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
        ServiceInfoSnapshot snapshot = this.generalCloudServiceProvider.getCloudService(uniqueId);
        return this.selectCloudServiceProvider(snapshot);
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return this.selectCloudServiceProvider(serviceInfoSnapshot);
    }

    @NotNull
    private SpecificCloudServiceProvider selectCloudServiceProvider(@Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot == null) {
            return EmptySpecificCloudServiceProvider.INSTANCE;
        }
        if (serviceInfoSnapshot.getServiceId().getNodeUniqueId().equals(this.getComponentName())) {
            ICloudService service = this.cloudServiceManager.getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());
            if (service != null) {
                return new LocalNodeSpecificCloudServiceProvider(this, service);
            }
            return EmptySpecificCloudServiceProvider.INSTANCE;
        }
        IClusterNodeServer server = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());
        if (server == null) {
            return EmptySpecificCloudServiceProvider.INSTANCE;
        }
        return server.getCloudServiceProvider(serviceInfoSnapshot);
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

    private void initServiceVersions() {
        String url = System.getProperty("cloudnet.versions.url", "https://cloudnetservice.eu/cloudnet/versions.json");
        System.out.println(LanguageManager.getMessage("versions-load").replace("%url%", url));

        try {
            if (this.serviceVersionProvider.loadServiceVersionTypes(url)) {
                System.out.println(LanguageManager.getMessage("versions-load-success")
                        .replace("%url%", url)
                        .replace("%versions%", Integer.toString(this.serviceVersionProvider.getServiceVersionTypes().size()))
                );
            } else {
                this.serviceVersionProvider.loadDefaultVersionTypes();

                System.err.println(LanguageManager.getMessage("versions-load-failed")
                        .replace("%url%", url)
                        .replace("%versions%", Integer.toString(this.serviceVersionProvider.getServiceVersionTypes().size()))
                        .replace("%error%", "invalid json")
                );
            }
        } catch (IOException exception) {
            this.serviceVersionProvider.loadDefaultVersionTypes();

            System.err.println(LanguageManager.getMessage("versions-load-failed")
                    .replace("%url%", url)
                    .replace("%versions%", Integer.toString(this.serviceVersionProvider.getServiceVersionTypes().size()))
                    .replace("%error%", exception.getClass().getName() + ": " + exception.getMessage())
            );
        }
    }

    public ServiceVersionProvider getServiceVersionProvider() {
        return this.serviceVersionProvider;
    }

    public ServiceInfoSnapshot getCloudServiceByNameOrUniqueId(String argument) {
        Preconditions.checkNotNull(argument);

        return this.getCloudServiceProvider().getCloudServices().stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getUniqueId().toString().toLowerCase().contains(argument.toLowerCase()))
                .findFirst()
                .orElseGet(() -> this.getCloudServiceProvider().getCloudServices().stream()
                        .filter(serviceInfoSnapshot ->
                                serviceInfoSnapshot.getServiceId().getName().toLowerCase().contains(argument.toLowerCase()))
                        .min(Comparator.comparingInt(serviceInfoSnapshot ->
                                serviceInfoSnapshot.getServiceId().getName().toLowerCase().replace(argument.toLowerCase(), "").length()))
                        .orElse(null)
                );
    }

    @Override
    public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
        return this.getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE).getTemplates();
    }

    @Override
    public Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        Collection<ServiceTemplate> collection = new ArrayList<>();

        if (this.servicesRegistry.containsService(ITemplateStorage.class, serviceName)) {
            collection.addAll(this.servicesRegistry.getService(ITemplateStorage.class, serviceName).getTemplates());
        }

        return collection;
    }

    @Override
    public void setGlobalLogLevel(@NotNull LogLevel logLevel) {
        this.setGlobalLogLevel(logLevel.getLevel());
    }

    @Override
    public void setGlobalLogLevel(int logLevel) {
        this.logger.setLevel(logLevel);
        this.sendAll(new PacketServerSetGlobalLogLevel(logLevel));
    }

    @Override
    public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(commandLine);

        IPermissionUser permissionUser = this.permissionManagement.getUser(uniqueId);
        if (permissionUser != null) {
            IPermissionUserCommandSender commandSender = new DefaultPermissionUserCommandSender(permissionUser, this.permissionManagement);
            boolean value = this.commandMap.dispatchCommand(commandSender, commandLine);

            return new Pair<>(value, commandSender.getWrittenMessages().toArray(new String[0]));
        } else {
            return new Pair<>(false, new String[0]);
        }
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return this.scheduleTask(CloudNet.this::getLocalTemplateStorageTemplates);
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        return this.scheduleTask(() -> CloudNet.this.getTemplateStorageTemplates(serviceName));
    }

    @Override
    @NotNull
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(commandLine);

        return this.scheduleTask(() -> CloudNet.this.sendCommandLineAsPermissionUser(uniqueId, commandLine));
    }

    @NotNull
    public <T> ITask<T> runTask(Callable<T> runnable) {
        return this.mainLoop.runTask(runnable);
    }

    @NotNull
    public ITask<?> runTask(Runnable runnable) {
        return this.runTask(Executors.callable(runnable));
    }

    public boolean isMainThread() {
        return Thread.currentThread().getName().equals("Application-Thread");
    }

    public void deployTemplateInCluster(ServiceTemplate serviceTemplate, byte[] resource) {
        Preconditions.checkNotNull(serviceTemplate);
        Preconditions.checkNotNull(resource);

        this.getClusterNodeServerProvider().deployTemplateInCluster(serviceTemplate, resource);
    }

    public void updateServiceTasksInCluster(Collection<ServiceTask> serviceTasks, NetworkUpdateType updateType) {
        this.getClusterNodeServerProvider().sendPacket(new PacketServerSetServiceTaskList(serviceTasks, updateType));
    }

    public void updateGroupConfigurationsInCluster(Collection<GroupConfiguration> groupConfigurations, NetworkUpdateType updateType) {
        this.getClusterNodeServerProvider().sendPacket(new PacketServerSetGroupConfigurationList(groupConfigurations, updateType));
    }

    @NotNull
    public ITask<Void> sendAllAsync(IPacket packet) {
        return this.scheduleTask(() -> {
            this.sendAll(packet);
            return null;
        });
    }

    public void sendAll(IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer clusterNodeServer : this.getClusterNodeServerProvider().getNodeServers()) {
            clusterNodeServer.saveSendPacket(packet);
        }

        for (ICloudService cloudService : this.getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }

    @NotNull
    public ITask<Void> sendAllAsync(IPacket... packets) {
        return this.scheduleTask(() -> {
            this.sendAll(packets);
            return null;
        });
    }

    public void sendAll(IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (IClusterNodeServer clusterNodeServer : this.getClusterNodeServerProvider().getNodeServers()) {
            for (IPacket packet : packets) {
                if (packet != null) {
                    clusterNodeServer.saveSendPacket(packet);
                }
            }
        }

        for (ICloudService cloudService : this.getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packets);
            }
        }
    }

    public NetworkClusterNodeInfoSnapshot createClusterNodeInfoSnapshot() {
        return new NetworkClusterNodeInfoSnapshot(
                System.currentTimeMillis(),
                this.config.getIdentity(),
                CloudNet.class.getPackage().getImplementationVersion(),
                this.cloudServiceManager.getCloudServices().size(),
                this.cloudServiceManager.getCurrentUsedHeapMemory(),
                this.cloudServiceManager.getCurrentReservedMemory(),
                this.config.getMaxMemory(),
                ProcessSnapshot.self(),
                this.moduleProvider.getModules().stream().map(IModuleWrapper::getModuleConfiguration).collect(Collectors.toList()),
                CPUUsageResolver.getSystemCPUUsage()
        );
    }

    public boolean canStartServices(ServiceTask serviceTask, String nodeUniqueId) {
        return this.canStartServices(serviceTask.getAssociatedNodes(), nodeUniqueId);
    }

    public boolean canStartServices(ServiceTask serviceTask) {
        return this.canStartServices(serviceTask.getAssociatedNodes());
    }

    public Collection<IClusterNodeServer> getValidClusterNodeServers(ServiceTask serviceTask) {
        return this.getValidClusterNodeServers(serviceTask.getAssociatedNodes());
    }

    @Nullable
    public NetworkClusterNodeInfoSnapshot searchLogicNode(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.searchLogicNode(serviceTask.getAssociatedNodes());
    }

    public boolean canStartServices(Collection<String> allowedNodes, String nodeUniqueId) {
        return allowedNodes != null && (allowedNodes.isEmpty() || allowedNodes.contains(nodeUniqueId));
    }

    public boolean canStartServices(Collection<String> allowedNodes) {
        return this.canStartServices(allowedNodes, this.getConfig().getIdentity().getUniqueId());
    }

    public Collection<IClusterNodeServer> getValidClusterNodeServers(Collection<String> allowedNodes) {
        return this.clusterNodeServerProvider.getNodeServers().stream()
                .filter(clusterNodeServer ->
                        clusterNodeServer.isConnected() && this.canStartServices(allowedNodes, clusterNodeServer.getNodeInfo().getUniqueId()))
                .collect(Collectors.toList());
    }

    @Nullable
    public NetworkClusterNodeInfoSnapshot searchLogicNode(Collection<String> allowedNodes) {
        Collection<NetworkClusterNodeInfoSnapshot> nodes = this.getValidClusterNodeServers(allowedNodes).stream()
                .map(IClusterNodeServer::getNodeInfoSnapshot)
                .collect(Collectors.toList());

        if (this.canStartServices(allowedNodes)) {
            nodes.add(this.currentNetworkClusterNodeInfoSnapshot);
        }

        return nodes.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(node ->
                        node.getSystemCpuUsage() + ((double) node.getReservedMemory() / node.getMaxMemory() * 100D)
                )).orElse(null);
    }

    public boolean competeWithCluster(ServiceTask serviceTask) {
        NetworkClusterNodeInfoSnapshot bestNode = this.searchLogicNode(serviceTask);
        return bestNode != null && bestNode.getNode().getUniqueId().equals(this.currentNetworkClusterNodeInfoSnapshot.getNode().getUniqueId());
    }

    public void unregisterPacketListenersByClassLoader(ClassLoader classLoader) {
        Preconditions.checkNotNull(classLoader);

        this.networkClient.getPacketRegistry().removeListeners(classLoader);
        this.networkServer.getPacketRegistry().removeListeners(classLoader);

        for (INetworkChannel channel : this.networkServer.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }

        for (INetworkChannel channel : this.networkClient.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }
    }

    public void publishNetworkClusterNodeInfoSnapshotUpdate() {
        this.lastNetworkClusterNodeInfoSnapshot = this.currentNetworkClusterNodeInfoSnapshot;
        this.currentNetworkClusterNodeInfoSnapshot = this.createClusterNodeInfoSnapshot();

        this.getEventManager().callEvent(new NetworkClusterNodeInfoConfigureEvent(this.currentNetworkClusterNodeInfoSnapshot));

        this.clusterNodeServerProvider.sendPacket(new PacketServerClusterNodeInfoUpdate(this.currentNetworkClusterNodeInfoSnapshot));
    }

    public void publishPermissionGroupUpdates(Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType) {
        this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(permissionGroups, updateType));
    }

    public void publishH2DatabaseDataToCluster(INetworkChannel channel) {
        if (channel != null) {
            if (this.databaseProvider instanceof H2DatabaseProvider) {
                Map<String, Map<String, JsonDocument>> map = this.allocateDatabaseData();

                channel.sendPacket(new PacketServerSetH2DatabaseData(map, NetworkUpdateType.ADD));

                for (Map.Entry<String, Map<String, JsonDocument>> entry : map.entrySet()) {
                    entry.getValue().clear();
                }

                map.clear();
            }
        }
    }

    private Map<String, Map<String, JsonDocument>> allocateDatabaseData() {
        Map<String, Map<String, JsonDocument>> map = new HashMap<>();

        for (String name : this.databaseProvider.getDatabaseNames()) {
            if (!map.containsKey(name)) {
                map.put(name, new HashMap<>());
            }
            IDatabase database = this.databaseProvider.getDatabase(name);
            map.get(name).putAll(database.entries());
        }

        return map;
    }


    private void initPacketRegistryListeners() {
        IPacketListenerRegistry registry = this.getNetworkClient().getPacketRegistry();

        // Packet client registry
        registry.addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketServerAuthorizationResponseListener());
        registry.addListener(PacketConstants.SERVICE_INFO_PUBLISH_CHANNEL, new PacketServerServiceInfoPublisherListener());
        registry.addListener(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL, new PacketServerUpdatePermissionsListener());
        registry.addListener(PacketConstants.CHANNEL_MESSAGING_CHANNEL, new PacketServerChannelMessageListener(false));

        registry.addListener(PacketConstants.CLUSTER_SERVICE_INFO_LIST_CHANNEL, new PacketServerSetGlobalServiceInfoListListener());
        registry.addListener(PacketConstants.CLUSTER_GROUP_CONFIG_LIST_CHANNEL, new PacketServerSetGroupConfigurationListListener());
        registry.addListener(PacketConstants.CLUSTER_TASK_LIST_CHANNEL, new PacketServerSetServiceTaskListListener());
        registry.addListener(PacketConstants.CLUSTER_PERMISSION_DATA_CHANNEL, new PacketServerSetPermissionDataListener());
        registry.addListener(PacketConstants.CLUSTER_TEMPLATE_DEPLOY_CHANNEL, new PacketServerDeployLocalTemplateListener());
        registry.addListener(PacketConstants.CLUSTER_NODE_INFO_CHANNEL, new PacketServerClusterNodeInfoUpdateListener());

        registry.addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerH2DatabaseListener());
        registry.addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerSetH2DatabaseDataListener());

        // Node server API
        registry.addListener(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, new PacketServerDriverAPIListener());

        // Packet server registry
        this.getNetworkServer().getPacketRegistry().addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketClientAuthorizationListener());
    }

    private void unloadAll() {
        this.unloadModules();
    }

    private void unloadModules() {
        for (IModuleWrapper moduleWrapper : this.moduleProvider.getModules()) {
            if (!moduleWrapper.getModuleConfiguration().isRuntimeModule()) {
                this.unloadModule(moduleWrapper);
            }
        }
    }

    private void unloadAllModules0() {
        for (IModuleWrapper moduleWrapper : this.moduleProvider.getModules()) {
            this.unloadModule(moduleWrapper);
        }
    }

    private void unloadModule(IModuleWrapper moduleWrapper) {
        this.unregisterPacketListenersByClassLoader(moduleWrapper.getClassLoader());
        this.eventManager.unregisterListeners(moduleWrapper.getClassLoader());
        this.commandMap.unregisterCommands(moduleWrapper.getClassLoader());
        this.servicesRegistry.unregisterAll(moduleWrapper.getClassLoader());
        this.httpServer.removeHandler(moduleWrapper.getClassLoader());
        this.getNetworkClient().getPacketRegistry().removeListeners(moduleWrapper.getClassLoader());
        this.getNetworkServer().getPacketRegistry().removeListeners(moduleWrapper.getClassLoader());

        moduleWrapper.unloadModule();
        this.logger.info(LanguageManager.getMessage("cloudnet-unload-module")
                .replace("%module_group%", moduleWrapper.getModuleConfiguration().getGroup())
                .replace("%module_name%", moduleWrapper.getModuleConfiguration().getName())
                .replace("%module_version%", moduleWrapper.getModuleConfiguration().getVersion())
                .replace("%module_author%", moduleWrapper.getModuleConfiguration().getAuthor()));
    }

    private void registerDefaultCommands() {
        this.logger.info(LanguageManager.getMessage("reload-register-defaultCommands"));

        this.commandMap.registerCommand(
                //Runtime commands
                new CommandHelp(),
                new CommandExit(),
                new CommandReload(),
                //Default commands
                new CommandClear(),
                new CommandTasks(),
                new CommandGroups(),
                new CommandService(),
                new CommandCreate(),
                new CommandCluster(),
                new CommandModules(),
                new CommandTemplate(),
                new CommandMe(),
                new CommandScreen(),
                new CommandPermissions(),
                new CommandCopy(),
                new CommandDebug()
        );
    }

    @NotNull
    public <T> ITask<T> scheduleTask(Callable<T> callable) {
        ITask<T> task = new ListenableTask<>(callable);

        this.taskScheduler.schedule(task);
        return task;
    }

    private void enableModules() {
        this.loadModules();
        this.startModules();
    }

    private void loadModules() {
        this.logger.info(LanguageManager.getMessage("cloudnet-load-modules-createDirectory"));
        this.moduleDirectory.mkdirs();

        this.logger.info(LanguageManager.getMessage("cloudnet-load-modules"));
        for (File file : Objects.requireNonNull(this.moduleDirectory.listFiles(pathname -> {
            String lowerName = pathname.getName().toLowerCase();
            return !pathname.isDirectory() && lowerName.endsWith(".jar") ||
                    lowerName.endsWith(".war") ||
                    lowerName.endsWith(".zip");
        }))) {
            this.logger.info(LanguageManager.getMessage("cloudnet-load-modules-found").replace("%file_name%", file.getName()));
            this.moduleProvider.loadModule(file);
        }
    }

    private void startModules() {
        for (IModuleWrapper moduleWrapper : this.moduleProvider.getModules()) {
            moduleWrapper.startModule();
        }
    }

    private void enableCommandCompleter() {
        this.console.addTabCompletionHandler(UUID.randomUUID(), (commandLine, args, properties) -> this.commandMap.tabCompleteCommand(commandLine));
    }

    private void setDefaultRegistryEntries() {
        this.configurationRegistry.getString("database_provider", "h2");

        this.configurationRegistry.save();
    }

    private void registerDefaultServices() {
        this.servicesRegistry.registerService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE,
                new LocalTemplateStorage(new File(System.getProperty("cloudnet.storage.local", "local/templates"))));

        this.servicesRegistry.registerService(AbstractDatabaseProvider.class, "h2",
                new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"),
                        !CloudNet.getInstance().getConfig().getClusterConfig().getNodes().isEmpty()));
    }

    private void runConsole() {
        this.logger.info(LanguageManager.getMessage("console-ready"));

        this.getConsole().addCommandHandler(UUID.randomUUID(), input -> {
            try {
                if (input.trim().isEmpty()) {
                    return;
                }

                CommandPreProcessEvent commandPreProcessEvent = new CommandPreProcessEvent(input, this.getConsoleCommandSender());
                this.getEventManager().callEvent(commandPreProcessEvent);

                if (commandPreProcessEvent.isCancelled()) {
                    return;
                }

                if (!this.getCommandMap().dispatchCommand(this.getConsoleCommandSender(), input)) {
                    this.getEventManager().callEvent(new CommandNotFoundEvent(input));
                    this.logger.warning(LanguageManager.getMessage("command-not-found"));

                    return;
                }

                this.getEventManager().callEvent(new CommandPostProcessEvent(input, this.getConsoleCommandSender()));

            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });
    }

    public ICommandMap getCommandMap() {
        return this.commandMap;
    }

    public File getModuleDirectory() {
        return this.moduleDirectory;
    }

    public IConfiguration getConfig() {
        return this.config;
    }

    public IConfigurationRegistry getConfigurationRegistry() {
        return this.configurationRegistry;
    }

    public ICloudServiceManager getCloudServiceManager() {
        return this.cloudServiceManager;
    }

    public IClusterNodeServerProvider getClusterNodeServerProvider() {
        return this.clusterNodeServerProvider;
    }

    public ITaskScheduler getNetworkTaskScheduler() {
        return this.networkTaskScheduler;
    }

    public List<String> getCommandLineArguments() {
        return this.commandLineArguments;
    }

    public Properties getCommandLineProperties() {
        return this.commandLineProperties;
    }

    public IConsole getConsole() {
        return this.console;
    }

    public QueuedConsoleLogHandler getQueuedConsoleLogHandler() {
        return this.queuedConsoleLogHandler;
    }

    public ConsoleCommandSender getConsoleCommandSender() {
        return this.consoleCommandSender;
    }

    @NotNull
    public INetworkClient getNetworkClient() {
        return this.networkClient;
    }

    public INetworkServer getNetworkServer() {
        return this.networkServer;
    }

    public IHttpServer getHttpServer() {
        return this.httpServer;
    }

    @NotNull
    public IPermissionManagement getPermissionManagement() {
        return this.permissionManagement;
    }

    public AbstractDatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    public NetworkClusterNodeInfoSnapshot getLastNetworkClusterNodeInfoSnapshot() {
        return this.lastNetworkClusterNodeInfoSnapshot;
    }

    public NetworkClusterNodeInfoSnapshot getCurrentNetworkClusterNodeInfoSnapshot() {
        return this.currentNetworkClusterNodeInfoSnapshot;
    }
}
