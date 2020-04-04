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
import de.dytanic.cloudnet.driver.network.*;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeExtensionSnapshot;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
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
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.event.permission.PermissionServiceSetEvent;
import de.dytanic.cloudnet.log.QueuedConsoleLogHandler;
import de.dytanic.cloudnet.module.NodeModuleProviderHandler;
import de.dytanic.cloudnet.network.NetworkClientChannelHandlerImpl;
import de.dytanic.cloudnet.network.NetworkServerChannelHandlerImpl;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.network.listener.*;
import de.dytanic.cloudnet.network.packet.*;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.permission.DefaultJsonFilePermissionManagement;
import de.dytanic.cloudnet.permission.DefaultPermissionManagementHandler;
import de.dytanic.cloudnet.permission.NodePermissionManagement;
import de.dytanic.cloudnet.permission.command.DefaultPermissionUserCommandSender;
import de.dytanic.cloudnet.permission.command.IPermissionUserCommandSender;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import de.dytanic.cloudnet.provider.NodeMessenger;
import de.dytanic.cloudnet.provider.NodeNodeInfoProvider;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import de.dytanic.cloudnet.provider.service.NodeCloudServiceFactory;
import de.dytanic.cloudnet.provider.service.NodeGeneralCloudServiceProvider;
import de.dytanic.cloudnet.provider.service.NodeSpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.DefaultCloudServiceManager;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.setup.DefaultInstallation;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class CloudNet extends CloudNetDriver {

    public static final int TPS = 10;
    public static volatile boolean RUNNING = true;
    private static CloudNet instance;


    private final LogLevel defaultLogLevel = LogLevel.getDefaultLogLevel(System.getProperty("cloudnet.logging.defaultlevel")).orElse(LogLevel.FATAL);

    private final ICommandMap commandMap = new DefaultCommandMap();

    private final File moduleDirectory = new File(System.getProperty("cloudnet.modules.directory", "modules"));

    private final IConfiguration config = new JsonConfiguration();

    private final IConfigurationRegistry configurationRegistry = new JsonConfigurationRegistry(Paths.get(System.getProperty("cloudnet.registry.global.path", "local/registry")));

    private final ICloudServiceManager cloudServiceManager = new DefaultCloudServiceManager();

    private final IClusterNodeServerProvider clusterNodeServerProvider = new DefaultClusterNodeServerProvider();

    private final ITaskScheduler networkTaskScheduler = new DefaultTaskScheduler();


    private final List<String> commandLineArguments;

    private final Properties commandLineProperties;

    private final IConsole console;

    private final QueuedConsoleLogHandler queuedConsoleLogHandler;

    private final ConsoleCommandSender consoleCommandSender;


    @NotNull
    private final Queue<ITask<?>> processQueue = new ConcurrentLinkedQueue<>();
    private INetworkClient networkClient;
    private INetworkServer networkServer;
    private IHttpServer httpServer;
    private IPermissionManagement permissionManagement;

    private CloudServiceFactory cloudServiceFactory = new NodeCloudServiceFactory(this);
    private GeneralCloudServiceProvider generalCloudServiceProvider = new NodeGeneralCloudServiceProvider(this);
    private ServiceTaskProvider serviceTaskProvider = new NodeServiceTaskProvider(this);
    private GroupConfigurationProvider groupConfigurationProvider = new NodeGroupConfigurationProvider(this);
    private NodeInfoProvider nodeInfoProvider = new NodeNodeInfoProvider(this);
    private CloudMessenger messenger = new NodeMessenger(this);

    private DefaultInstallation defaultInstallation = new DefaultInstallation();

    private ServiceVersionProvider serviceVersionProvider = new ServiceVersionProvider();

    private AbstractDatabaseProvider databaseProvider;
    private volatile NetworkClusterNodeInfoSnapshot lastNetworkClusterNodeInfoSnapshot, currentNetworkClusterNodeInfoSnapshot;

    CloudNet(List<String> commandLineArguments, ILogger logger, IConsole console) {
        super(logger);
        setInstance(this);

        logger.setLevel(this.defaultLogLevel);

        this.console = console;
        this.commandLineArguments = commandLineArguments;
        this.commandLineProperties = Properties.parseLine(commandLineArguments.toArray(new String[0]));

        this.consoleCommandSender = new ConsoleCommandSender(logger);

        logger.addLogHandler(queuedConsoleLogHandler = new QueuedConsoleLogHandler());

        this.cloudServiceManager.init();

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

        HeaderReader.readAndPrintHeader(console);

        if (this.config.getMaxMemory() < 2048) {
            CloudNetDriver.getInstance().getLogger().warning(LanguageManager.getMessage("cloudnet-init-config-low-memory-warning"));
        }

        this.networkClient = new NettyNetworkClient(NetworkClientChannelHandlerImpl::new,
                this.config.getClientSslConfig().isEnabled() ? this.config.getClientSslConfig().toSslConfiguration() : null,
                networkTaskScheduler
        );
        super.packetQueryProvider = new PacketQueryProvider(this.networkClient);

        this.networkServer = new NettyNetworkServer(NetworkServerChannelHandlerImpl::new,
                this.config.getClientSslConfig().isEnabled() ? this.config.getServerSslConfig().toSslConfiguration() : null,
                networkTaskScheduler
        );
        this.httpServer = new NettyHttpServer(this.config.getClientSslConfig().isEnabled() ? this.config.getWebSslConfig().toSslConfiguration() : null);

        this.initPacketRegistryListeners();
        this.clusterNodeServerProvider.setClusterServers(this.config.getClusterConfig());

        this.enableCommandCompleter();
        this.setDefaultRegistryEntries();

        this.registerDefaultCommands();
        this.registerDefaultServices();

        this.currentNetworkClusterNodeInfoSnapshot = createClusterNodeInfoSnapshot();
        this.lastNetworkClusterNodeInfoSnapshot = currentNetworkClusterNodeInfoSnapshot;

        this.loadModules();

        this.databaseProvider = this.servicesRegistry.getService(AbstractDatabaseProvider.class,
                this.configurationRegistry.getString("database_provider", "h2"));

        if (databaseProvider == null) {
            stop();
        }

        this.databaseProvider.setDatabaseHandler(new DefaultDatabaseHandler());

        if (!this.databaseProvider.init() && !(this.databaseProvider instanceof H2DatabaseProvider)) {
            this.databaseProvider = this.servicesRegistry.getService(AbstractDatabaseProvider.class, "h2");
            this.databaseProvider.init();
        }

        NodePermissionManagement permissionManagement = this.servicesRegistry.getService(NodePermissionManagement.class, this.configurationRegistry.getString("permission_service", "json_database"));
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
        this.mainloop();
    }

    private void setNetworkListeners() {
        Random random = new Random();
        for (NetworkClusterNode node : this.config.getClusterConfig().getNodes()) {
            if (!networkClient.connect(node.getListeners()[random.nextInt(node.getListeners().length)])) {
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

        this.cloudServiceManager.reload();

        this.unloadAll();

        this.initServiceVersions();

        this.enableModules();

        this.logger.info(LanguageManager.getMessage("reload-end-message"));
    }

    @Override
    public void stop() {
        if (RUNNING) {
            RUNNING = false;
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
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
        return new NodeSpecificCloudServiceProvider(this, name);
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
        return new NodeSpecificCloudServiceProvider(this, uniqueId);
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return new NodeSpecificCloudServiceProvider(this, serviceInfoSnapshot);
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

        if (servicesRegistry.containsService(ITemplateStorage.class, serviceName)) {
            collection.addAll(servicesRegistry.getService(ITemplateStorage.class, serviceName).getTemplates());
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

        IPermissionUser permissionUser = permissionManagement.getUser(uniqueId);
        if (permissionUser != null) {
            IPermissionUserCommandSender commandSender = new DefaultPermissionUserCommandSender(permissionUser, permissionManagement);
            boolean value = commandMap.dispatchCommand(commandSender, commandLine);

            return new Pair<>(value, commandSender.getWrittenMessages().toArray(new String[0]));
        } else {
            return new Pair<>(false, new String[0]);
        }
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return scheduleTask(CloudNet.this::getLocalTemplateStorageTemplates);
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        return scheduleTask(() -> CloudNet.this.getTemplateStorageTemplates(serviceName));
    }

    @Override
    @NotNull
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(commandLine);

        return scheduleTask(() -> CloudNet.this.sendCommandLineAsPermissionUser(uniqueId, commandLine));
    }

    @NotNull
    public <T> ITask<T> runTask(Callable<T> runnable) {
        ITask<T> task = new ListenableTask<>(runnable);

        this.processQueue.offer(task);
        return task;
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
        return scheduleTask(() -> {
            sendAll(packet);
            return null;
        });
    }

    public void sendAll(IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer clusterNodeServer : getClusterNodeServerProvider().getNodeServers()) {
            clusterNodeServer.saveSendPacket(packet);
        }

        for (ICloudService cloudService : getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }

    @NotNull
    public ITask<Void> sendAllAsync(IPacket... packets) {
        return scheduleTask(() -> {
            sendAll(packets);
            return null;
        });
    }

    public void sendAll(IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (IClusterNodeServer clusterNodeServer : getClusterNodeServerProvider().getNodeServers()) {
            for (IPacket packet : packets) {
                if (packet != null) {
                    clusterNodeServer.saveSendPacket(packet);
                }
            }
        }

        for (ICloudService cloudService : getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packets);
            }
        }
    }

    public NetworkClusterNodeInfoSnapshot createClusterNodeInfoSnapshot() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        return new NetworkClusterNodeInfoSnapshot(
                System.currentTimeMillis(),
                this.config.getIdentity(),
                CloudNet.class.getPackage().getImplementationVersion(),
                this.cloudServiceManager.getCloudServices().size(),
                this.cloudServiceManager.getCurrentUsedHeapMemory(),
                this.cloudServiceManager.getCurrentReservedMemory(),
                this.config.getMaxMemory(),
                new ProcessSnapshot(
                        memoryMXBean.getHeapMemoryUsage().getUsed(),
                        memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                        memoryMXBean.getHeapMemoryUsage().getMax(),
                        ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
                        ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
                        ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount(),
                        Thread.getAllStackTraces().keySet().stream()
                                .map(thread -> new ThreadSnapshot(thread.getId(), thread.getName(), thread.getState(), thread.isDaemon(), thread.getPriority()))
                                .collect(Collectors.toList()),
                        CPUUsageResolver.getProcessCPUUsage(),
                        this.getOwnPID()
                ),
                this.moduleProvider.getModules().stream().map(moduleWrapper -> new NetworkClusterNodeExtensionSnapshot(
                        moduleWrapper.getModuleConfiguration().getGroup(),
                        moduleWrapper.getModuleConfiguration().getName(),
                        moduleWrapper.getModuleConfiguration().getVersion(),
                        moduleWrapper.getModuleConfiguration().getAuthor(),
                        moduleWrapper.getModuleConfiguration().getWebsite(),
                        moduleWrapper.getModuleConfiguration().getDescription()
                )).collect(Collectors.toList()),
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()
        );
    }

    public Collection<IClusterNodeServer> getValidClusterNodeServers(ServiceTask serviceTask) {
        return clusterNodeServerProvider.getNodeServers().stream().filter(clusterNodeServer -> {
            if (!clusterNodeServer.isConnected()) {
                return false;
            }
            return serviceTask.getAssociatedNodes().isEmpty() || serviceTask.getAssociatedNodes().contains(clusterNodeServer.getNodeInfo().getUniqueId());
        }).collect(Collectors.toList());
    }

    public NetworkClusterNodeInfoSnapshot searchLogicNode(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        Collection<NetworkClusterNodeInfoSnapshot> nodes = this.getValidClusterNodeServers(serviceTask).stream().map(IClusterNodeServer::getNodeInfoSnapshot).collect(Collectors.toList());
        if (serviceTask.getAssociatedNodes().isEmpty() || serviceTask.getAssociatedNodes().contains(this.config.getIdentity().getUniqueId())) {
            nodes.add(this.currentNetworkClusterNodeInfoSnapshot);
        }
        boolean windows = nodes.stream().anyMatch(node -> node.getSystemCpuUsage() == -1); //on windows the systemCpuUsage will be always -1, so we cannot find the node with the lowest cpu usage
        return nodes.stream().max(Comparator.comparingDouble(
                value -> windows ?
                        value.getMaxMemory() - value.getReservedMemory() :
                        (value.getMaxMemory() - value.getReservedMemory()) + (100 - value.getSystemCpuUsage())
        )).orElse(null);
    }

    public boolean competeWithCluster(ServiceTask serviceTask) {
        Collection<IClusterNodeServer> clusterNodeServers = this.getValidClusterNodeServers(serviceTask);

        boolean allow = true;

        for (IClusterNodeServer clusterNodeServer : clusterNodeServers) {
            if (clusterNodeServer.getNodeInfoSnapshot() != null
                    && clusterNodeServer.getNodeInfoSnapshot().getMaxMemory() - clusterNodeServer.getNodeInfoSnapshot().getReservedMemory()
                    > this.currentNetworkClusterNodeInfoSnapshot.getMaxMemory() - this.currentNetworkClusterNodeInfoSnapshot.getReservedMemory()
                    && clusterNodeServer.getNodeInfoSnapshot().getProcessSnapshot().getCpuUsage() * clusterNodeServer.getNodeInfoSnapshot().getCurrentServicesCount()
                    < this.currentNetworkClusterNodeInfoSnapshot.getProcessSnapshot().getCpuUsage() * this.currentNetworkClusterNodeInfoSnapshot.getCurrentServicesCount()
            ) {
                allow = false;
            }
        }

        return clusterNodeServers.size() == 0 || allow;
    }

    public void unregisterPacketListenersByClassLoader(ClassLoader classLoader) {
        Preconditions.checkNotNull(classLoader);

        networkClient.getPacketRegistry().removeListeners(classLoader);
        networkServer.getPacketRegistry().removeListeners(classLoader);

        for (INetworkChannel channel : networkServer.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }

        for (INetworkChannel channel : networkClient.getChannels()) {
            channel.getPacketRegistry().removeListeners(classLoader);
        }
    }

    public void publishNetworkClusterNodeInfoSnapshotUpdate() {
        this.lastNetworkClusterNodeInfoSnapshot = this.currentNetworkClusterNodeInfoSnapshot;
        this.currentNetworkClusterNodeInfoSnapshot = this.createClusterNodeInfoSnapshot();

        this.getEventManager().callEvent(new NetworkClusterNodeInfoConfigureEvent(currentNetworkClusterNodeInfoSnapshot));
        this.sendAll(new PacketServerClusterNodeInfoUpdate(this.currentNetworkClusterNodeInfoSnapshot));
    }

    public void publishPermissionGroupUpdates(Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType) {
        this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(permissionGroups, updateType, true));
    }

    public void publishH2DatabaseDataToCluster(INetworkChannel channel) {
        if (channel != null) {
            if (databaseProvider instanceof H2DatabaseProvider) {
                Map<String, Map<String, JsonDocument>> map = allocateDatabaseData();

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

        for (String name : databaseProvider.getDatabaseNames()) {
            if (!map.containsKey(name)) {
                map.put(name, new HashMap<>());
            }
            IDatabase database = databaseProvider.getDatabase(name);
            map.get(name).putAll(database.entries());
        }

        return map;
    }


    private void initPacketRegistryListeners() {
        // Packet client registry
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketServerAuthorizationResponseListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerServiceInfoPublisherListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerUpdatePermissionsListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new PacketServerChannelMessageListener());

        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerSetGlobalServiceInfoListListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerSetGroupConfigurationListListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerSetPermissionDataListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerSetServiceTaskListListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerDeployLocalTemplateListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new PacketServerClusterNodeInfoUpdateListener());

        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerH2DatabaseListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerSetH2DatabaseDataListener());

        // Node server API
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL, new PacketClientCallablePacketReceiveListener());
        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL, new PacketClientSyncAPIPacketListener());

        this.getNetworkClient().getPacketRegistry().addListener(PacketConstants.INTERNAL_PACKET_CLUSTER_MESSAGE_CHANNEL, new PacketServerClusterChannelMessageListener());

        // Packet server registry
        this.getNetworkServer().getPacketRegistry().addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketClientAuthorizationListener());
    }

    private void mainloop() {
        long value = System.currentTimeMillis();
        long millis = 1000 / TPS;
        int start1Tick = 0, start3Tick = 0 / 2;

        while (RUNNING) {
            try {
                long diff = System.currentTimeMillis() - value;
                if (diff < millis) {
                    try {
                        Thread.sleep(millis - diff);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }

                value = System.currentTimeMillis();

                while (!this.processQueue.isEmpty()) {
                    if (this.processQueue.peek() != null) {
                        Objects.requireNonNull(this.processQueue.poll()).call();
                    } else {
                        this.processQueue.poll();
                    }
                }

                if (start1Tick++ >= TPS) {
                    this.launchServices();
                    start1Tick = 0;
                }

                this.stopDeadServices();

                if (start3Tick++ >= TPS) {
                    this.sendNodeUpdate();
                    start3Tick = 0;
                }

                this.updateServiceLogs();

                eventManager.callEvent(new CloudNetTickEvent());

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void launchServices() {
        for (ServiceTask serviceTask : cloudServiceManager.getServiceTasks()) {
            if (serviceTask.canStartServices()) {

                Collection<ServiceInfoSnapshot> taskServices = this.getCloudServiceProvider().getCloudServices(serviceTask.getName());

                long runningTaskServices = taskServices.stream()
                        .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.RUNNING)
                        .count();

                if ((serviceTask.getAssociatedNodes().isEmpty() || (serviceTask.getAssociatedNodes().contains(getConfig().getIdentity().getUniqueId()))) &&
                        serviceTask.getMinServiceCount() > runningTaskServices) {

                    // there are still less running services of this task than the specified minServiceCount, so looking for a local service which isn't started yet
                    Optional<ICloudService> nonStartedServiceOptional = this.getCloudServiceManager().getLocalCloudServices(serviceTask.getName())
                            .stream()
                            .filter(cloudService -> cloudService.getLifeCycle() == ServiceLifeCycle.DEFINED
                                    || cloudService.getLifeCycle() == ServiceLifeCycle.PREPARED)
                            .findFirst();

                    if (nonStartedServiceOptional.isPresent()) {
                        try {
                            nonStartedServiceOptional.get().start();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    } else if (serviceTask.getMinServiceCount() > taskServices.size() && this.competeWithCluster(serviceTask)) {
                        // There is no local existing service to start and there are less services existing of this task
                        // than the specified minServiceCount, so starting a new service, because this is the best node to do so

                        ICloudService cloudService = cloudServiceManager.runTask(serviceTask);

                        if (cloudService != null) {
                            try {
                                cloudService.start();
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    private void stopDeadServices() {
        for (ICloudService cloudService : this.cloudServiceManager.getCloudServices().values()) {
            if (!cloudService.isAlive()) {
                cloudService.stop();
            }
        }
    }

    private void sendNodeUpdate() {
        this.publishNetworkClusterNodeInfoSnapshotUpdate();
    }

    private void updateServiceLogs() {
        for (ICloudService cloudService : cloudServiceManager.getCloudServices().values()) {
            cloudService.getServiceConsoleLogCache().update();
        }
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

        taskScheduler.schedule(task);
        return task;
    }

    private void enableModules() {
        loadModules();
        startModules();
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
        this.configurationRegistry.getString("permission_service", "json_database");
        this.configurationRegistry.getString("database_provider", "h2");

        this.configurationRegistry.save();
    }

    private void registerDefaultServices() {
        this.servicesRegistry.registerService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE,
                new LocalTemplateStorage(new File(System.getProperty("cloudnet.storage.local", "local/templates"))));

        this.servicesRegistry.registerService(NodePermissionManagement.class, "json_file",
                new DefaultJsonFilePermissionManagement(new File(System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"))) {
                    @Override
                    public <V> ITask<V> scheduleTask(Callable<V> callable) {
                        return CloudNet.this.scheduleTask(callable);
                    }
                });

        this.servicesRegistry.registerService(NodePermissionManagement.class, "json_database",
                new DefaultDatabasePermissionManagement(this::getDatabaseProvider) {
                    @Override
                    public <V> ITask<V> scheduleTask(Callable<V> callable) {
                        return CloudNet.this.scheduleTask(callable);
                    }
                });

        this.servicesRegistry.registerService(AbstractDatabaseProvider.class, "h2",
                new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"),
                        !CloudNet.getInstance().getConfig().getClusterConfig().getNodes().isEmpty(), this.taskScheduler));
    }

    private void runConsole() {
        this.logger.info(LanguageManager.getMessage("console-ready"));

        this.getConsole().addCommandHandler(UUID.randomUUID(), input -> {
            try {
                if (input.trim().isEmpty()) {
                    return;
                }

                CommandPreProcessEvent commandPreProcessEvent = new CommandPreProcessEvent(input, getConsoleCommandSender());
                getEventManager().callEvent(commandPreProcessEvent);

                if (commandPreProcessEvent.isCancelled()) {
                    return;
                }

                if (!getCommandMap().dispatchCommand(getConsoleCommandSender(), input)) {
                    getEventManager().callEvent(new CommandNotFoundEvent(input));
                    this.logger.warning(LanguageManager.getMessage("command-not-found"));

                    return;
                }

                getEventManager().callEvent(new CommandPostProcessEvent(input, getConsoleCommandSender()));

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
