package de.dytanic.cloudnet;

import de.dytanic.cloudnet.cluster.DefaultClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.command.*;
import de.dytanic.cloudnet.command.commands.*;
import de.dytanic.cloudnet.command.jline2.JLine2CommandCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.command.CommandInfo;
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
import de.dytanic.cloudnet.console.ConsoleColor;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.JLine2Console;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.DefaultDatabaseHandler;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.module.DefaultPersistableModuleDependencyLoader;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeExtensionSnapshot;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.permission.*;
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
import de.dytanic.cloudnet.network.listener.*;
import de.dytanic.cloudnet.network.packet.*;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.permission.DefaultPermissionManagementHandler;
import de.dytanic.cloudnet.permission.command.DefaultPermissionUserCommandSender;
import de.dytanic.cloudnet.permission.command.IPermissionUserCommandSender;
import de.dytanic.cloudnet.service.DefaultCloudServiceManager;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class CloudNet extends CloudNetDriver {

    public static final int TPS = 10;
    public static volatile boolean RUNNING = true;
    private static CloudNet instance;


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


    private final Queue<ITask<?>> processQueue = Iterables.newConcurrentLinkedQueue();
    private INetworkClient networkClient;
    private INetworkServer networkServer;
    private IHttpServer httpServer;
    private IPermissionManagement permissionManagement;


    private AbstractDatabaseProvider databaseProvider;
    private volatile NetworkClusterNodeInfoSnapshot lastNetworkClusterNodeInfoSnapshot, currentNetworkClusterNodeInfoSnapshot;

    CloudNet(List<String> commandLineArguments, ILogger logger, IConsole console) {
        super(logger);
        setInstance(this);

        this.console = console;
        this.commandLineArguments = commandLineArguments;
        this.commandLineProperties = Properties.parseLine(commandLineArguments.toArray(new String[0]));

        this.consoleCommandSender = new ConsoleCommandSender(logger);

        logger.addLogHandler(queuedConsoleLogHandler = new QueuedConsoleLogHandler());

        this.cloudServiceManager.init();

        this.moduleProvider.setModuleProviderHandler(new NodeModuleProviderHandler());
        this.moduleProvider.setModuleDependencyLoader(new DefaultPersistableModuleDependencyLoader(new File(System.getProperty("cloudnet.launcher.dir", "launcher") + "/libs")));

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

        initDefaultConfigDefaultHostAddress();
        this.config.load();

        this.networkClient = new NettyNetworkClient(NetworkClientChannelHandlerImpl::new,
                this.config.getClientSslConfig().isEnabled() ? this.config.getClientSslConfig().toSslConfiguration() : null,
                networkTaskScheduler
        );
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

        this.permissionManagement = this.servicesRegistry.getService(IPermissionManagement.class, this.configurationRegistry.getString("permission_service", "json_database"));
        this.permissionManagement.setPermissionManagementHandler(new DefaultPermissionManagementHandler());

        this.startModules();
        this.eventManager.callEvent(new PermissionServiceSetEvent(this.permissionManagement));

        this.setNetworkListeners();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));

        //setup implementations
        this.initDefaultPermissionGroups();
        this.initDefaultTasks();

        eventManager.callEvent(new CloudNetNodePostInitializationEvent());

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

        this.cloudServiceManager.deleteAllCloudServices();
        this.taskScheduler.shutdown();

        this.unloadAll();
        this.unloadAllModules0();

        try {
            if (this.databaseProvider != null) {
                try {
                    this.databaseProvider.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            this.logger.info(LanguageManager.getMessage("stop-network-client"));
            this.networkClient.close();

            this.logger.info(LanguageManager.getMessage("stop-network-server"));
            this.networkServer.close();

            this.logger.info(LanguageManager.getMessage("stop-http-server"));
            this.httpServer.close();

            this.networkTaskScheduler.shutdown();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        FileUtils.delete(new File("temp"));

        if (!Thread.currentThread().getName().equals("Shutdown Thread")) {
            System.exit(0);
        }
    }

    @Override
    public String[] sendCommandLine(String commandLine) {
        Validate.checkNotNull(commandLine);

        Collection<String> collection = Iterables.newArrayList();

        if (this.isMainThread()) {
            this.sendCommandLine0(collection, commandLine);
        } else {
            try {
                runTask((Callable<Void>) () -> {
                    sendCommandLine0(collection, commandLine);
                    return null;
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return collection.toArray(new String[0]);
    }

    @Override
    public String[] sendCommandLine(String nodeUniqueId, String commandLine) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(commandLine);

        if (this.getConfig().getIdentity().getUniqueId().equals(nodeUniqueId)) {
            return this.sendCommandLine(commandLine);
        }

        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(nodeUniqueId);

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.sendCommandLine(commandLine);
        }

        return null;
    }

    private void sendCommandLine0(Collection<String> collection, String commandLine) {
        this.commandMap.dispatchCommand(new DriverCommandSender(collection), commandLine);
    }

    @Override
    public void sendChannelMessage(String channel, String message, JsonDocument data) {
        Validate.checkNotNull(channel);
        Validate.checkNotNull(message);
        Validate.checkNotNull(data);

        this.sendAll(new PacketClientServerChannelMessage(channel, message, data));
    }

    @Override
    public void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data) {
        if (targetServiceInfoSnapshot.getServiceId().getNodeUniqueId().equals(this.config.getIdentity().getUniqueId())) {
            ICloudService cloudService = this.getCloudServiceManager().getCloudService(targetServiceInfoSnapshot.getServiceId().getUniqueId());
            if (cloudService != null && cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(new PacketClientServerChannelMessage(channel, message, data));
            }
        } else {
            IClusterNodeServer nodeServer = this.clusterNodeServerProvider.getNodeServer(targetServiceInfoSnapshot.getServiceId().getNodeUniqueId());
            if (nodeServer != null) {
                nodeServer.saveSendPacket(new PacketClientServerChannelMessage(
                        targetServiceInfoSnapshot.getServiceId().getUniqueId(),
                        channel,
                        message,
                        data
                ));
            }
        }
    }

    @Override
    public void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data) {
        for (ServiceInfoSnapshot serviceInfoSnapshot : this.getCloudService(targetServiceTask.getName())) {
            this.sendChannelMessage(serviceInfoSnapshot, channel, message, data);
        }
    }

    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        try {
            NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot = searchLogicNode(serviceTask);
            if (networkClusterNodeInfoSnapshot == null) {
                return null;
            }

            if (getConfig().getIdentity().getUniqueId().equals(networkClusterNodeInfoSnapshot.getNode().getUniqueId())) {
                ICloudService cloudService = this.cloudServiceManager.runTask(serviceTask);
                return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
            } else {
                IClusterNodeServer clusterNodeServer = getClusterNodeServerProvider().getNodeServer(networkClusterNodeInfoSnapshot.getNode().getUniqueId());

                if (clusterNodeServer != null && clusterNodeServer.isConnected()) {
                    return clusterNodeServer.createCloudService(serviceTask);
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Validate.checkNotNull(serviceConfiguration);

        if (serviceConfiguration.getServiceId() == null || serviceConfiguration.getServiceId().getNodeUniqueId() == null) {
            return null;
        }

        if (getConfig().getIdentity().getUniqueId().equals(serviceConfiguration.getServiceId().getNodeUniqueId())) {
            ICloudService cloudService = this.cloudServiceManager.runTask(serviceConfiguration);
            return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
        } else {
            IClusterNodeServer clusterNodeServer = getClusterNodeServerProvider().getNodeServer(serviceConfiguration.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected()) {
                return clusterNodeServer.createCloudService(serviceConfiguration);
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot createCloudService(String name, String runtime, boolean autoDeleteOnStop, boolean staticService, Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  JsonDocument properties, Integer port) {
        ICloudService cloudService = this.cloudServiceManager.runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
        return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties, Integer port) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        if (this.getConfig().getIdentity().getUniqueId().equals(nodeUniqueId)) {
            Collection<ServiceInfoSnapshot> collection = Iterables.newArrayList();

            for (int i = 0; i < amount; i++) {
                ICloudService cloudService = this.cloudServiceManager.runTask(
                        name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port != null ? port++ : null
                );

                if (cloudService != null) {
                    collection.add(cloudService.getServiceInfoSnapshot());
                }
            }

            return collection;
        }

        IClusterNodeServer clusterNodeServer = getClusterNodeServerProvider().getNodeServer(nodeUniqueId);

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
        } else {
            return null;
        }
    }

    @Override
    public ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return null;
        }

        ICloudService cloudService = cloudServiceManager.getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.runCommand(commandLine);
            return cloudService.getServiceInfoSnapshot();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.sendCommandLineToCloudService(uniqueId, commandLine);
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return null;
        }

        ICloudService cloudService = cloudServiceManager.getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.getWaitingTemplates().offer(serviceTemplate);
            return cloudService.getServiceInfoSnapshot();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.addServiceTemplateToCloudService(uniqueId, serviceTemplate);
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return null;
        }

        ICloudService cloudService = cloudServiceManager.getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.getWaitingIncludes().offer(serviceRemoteInclusion);
            return cloudService.getServiceInfoSnapshot();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.addServiceRemoteInclusionToCloudService(uniqueId, serviceRemoteInclusion);
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return null;
        }

        ICloudService cloudService = cloudServiceManager.getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.getDeployments().add(serviceDeployment);
            return cloudService.getServiceInfoSnapshot();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.addServiceDeploymentToCloudService(uniqueId, serviceDeployment);
        }

        return null;
    }

    @Override
    public Queue<String> getCachedLogMessagesFromService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return null;
        }

        ICloudService cloudService = cloudServiceManager.getCloudService(uniqueId);

        if (cloudService != null) {
            return cloudService.getServiceConsoleLogCache().getCachedLogMessages();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.getCachedLogMessagesFromService(uniqueId);
        }

        return null;
    }

    @Override
    public void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(lifeCycle);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(serviceInfoSnapshot.getServiceId().getUniqueId())) {
            return;
        }

        ICloudService cloudService = this.cloudServiceManager.getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());
        if (cloudService != null) {
            switch (lifeCycle) {
                case RUNNING:
                    try {
                        cloudService.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case STOPPED:
                    scheduleTask((Callable<Void>) () -> {
                        cloudService.stop();
                        return null;
                    });
                    break;
                case DELETED:
                    scheduleTask((Callable<Void>) () -> {
                        cloudService.delete();
                        return null;
                    });
                    break;
            }
        } else {
            IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                clusterNodeServer.setCloudServiceLifeCycle(serviceInfoSnapshot, lifeCycle);
            }
        }
    }

    @Override
    public void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(serviceInfoSnapshot.getServiceId().getUniqueId())) {
            return;
        }

        ICloudService cloudService = this.getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

        if (cloudService != null) {
            try {
                cloudService.restart();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.restartCloudService(serviceInfoSnapshot);
        }
    }

    @Override
    public void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(serviceInfoSnapshot.getServiceId().getUniqueId())) {
            return;
        }

        ICloudService cloudService = this.getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

        if (cloudService != null) {
            try {
                cloudService.kill();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.killCloudService(serviceInfoSnapshot);
        }
    }

    @Override
    public void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(serviceInfoSnapshot.getServiceId().getUniqueId())) {
            return;
        }

        ICloudService cloudService = this.getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

        if (cloudService != null) {
            try {
                cloudService.runCommand(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.runCommand(serviceInfoSnapshot, command);
        }
    }

    @Override
    public Collection<UUID> getServicesAsUniqueId() {
        return Collections.unmodifiableCollection(this.cloudServiceManager.getGlobalServiceInfoSnapshots().keySet());
    }

    @Override
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        return this.cloudServiceManager.getGlobalServiceInfoSnapshots().values().stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        return this.cloudServiceManager.getServiceInfoSnapshots();
    }

    @Override
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        return Iterables.filter(this.getCloudServices(), serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudService(String taskName) {
        Validate.checkNotNull(taskName);

        return this.cloudServiceManager.getServiceInfoSnapshots(taskName);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServiceByGroup(String group) {
        Validate.checkNotNull(group);

        return Iterables.filter(this.cloudServiceManager.getGlobalServiceInfoSnapshots().values(), serviceInfoSnapshot -> Iterables.contains(group, serviceInfoSnapshot.getConfiguration().getGroups()));
    }

    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cloudServiceManager.getServiceInfoSnapshot(uniqueId);
    }

    @Override
    public Integer getServicesCount() {
        return this.getCloudServiceManager().getGlobalServiceInfoSnapshots().size();
    }

    @Override
    public Integer getServicesCountByGroup(String group) {
        Validate.checkNotNull(group);

        int amount = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()) {
            if (Iterables.contains(group, serviceInfoSnapshot.getConfiguration().getGroups())) {
                amount++;
            }
        }

        return amount;
    }

    @Override
    public Integer getServicesCountByTask(String taskName) {
        Validate.checkNotNull(taskName);

        int amount = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()) {
            if (serviceInfoSnapshot.getServiceId().getTaskName().equals(taskName)) {
                amount++;
            }
        }

        return amount;
    }

    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return this.cloudServiceManager.getServiceTasks();
    }

    @Override
    public ServiceTask getServiceTask(String name) {
        Validate.checkNotNull(name);

        return this.cloudServiceManager.getServiceTask(name);
    }

    @Override
    public boolean isServiceTaskPresent(String name) {
        Validate.checkNotNull(name);

        return this.cloudServiceManager.isTaskPresent(name);
    }

    @Override
    public void addPermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        this.cloudServiceManager.addPermanentServiceTask(serviceTask);
    }

    @Override
    public void removePermanentServiceTask(String name) {
        Validate.checkNotNull(name);

        this.cloudServiceManager.removePermanentServiceTask(name);
    }

    @Override
    public void removePermanentServiceTask(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        this.cloudServiceManager.removePermanentServiceTask(serviceTask);
    }

    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return this.cloudServiceManager.getGroupConfigurations();
    }

    @Override
    public GroupConfiguration getGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        return this.cloudServiceManager.getGroupConfiguration(name);
    }

    @Override
    public boolean isGroupConfigurationPresent(String name) {
        Validate.checkNotNull(name);

        return this.cloudServiceManager.isGroupConfigurationPresent(name);
    }

    @Override
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        this.cloudServiceManager.addGroupConfiguration(groupConfiguration);
    }

    @Override
    public void removeGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        this.cloudServiceManager.removeGroupConfiguration(name);
    }

    @Override
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        this.cloudServiceManager.removeGroupConfiguration(groupConfiguration);
    }

    @Override
    public NetworkClusterNode[] getNodes() {
        return this.config.getClusterConfig().getNodes().toArray(new NetworkClusterNode[0]);
    }

    @Override
    public NetworkClusterNode getNode(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (uniqueId.equals(this.config.getIdentity().getUniqueId())) {
            return this.config.getIdentity();
        }
        return Iterables.first(this.config.getClusterConfig().getNodes(), networkClusterNode -> networkClusterNode.getUniqueId().equals(uniqueId));
    }

    @Override
    public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
        Collection<NetworkClusterNodeInfoSnapshot> nodeInfoSnapshots = Iterables.newArrayList();

        for (IClusterNodeServer clusterNodeServer : this.clusterNodeServerProvider.getNodeServers()) {
            if (clusterNodeServer.isConnected() && clusterNodeServer.getNodeInfoSnapshot() != null) {
                nodeInfoSnapshots.add(clusterNodeServer.getNodeInfoSnapshot());
            }
        }

        return nodeInfoSnapshots.toArray(new NetworkClusterNodeInfoSnapshot[0]);
    }

    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId) {
        if (uniqueId.equals(this.config.getIdentity().getUniqueId())) {
            return this.currentNetworkClusterNodeInfoSnapshot;
        }

        for (IClusterNodeServer clusterNodeServer : this.clusterNodeServerProvider.getNodeServers()) {
            if (clusterNodeServer.getNodeInfo().getUniqueId().equals(uniqueId) && clusterNodeServer.isConnected() && clusterNodeServer.getNodeInfoSnapshot() != null) {
                return clusterNodeServer.getNodeInfoSnapshot();
            }
        }

        return null;
    }

    @Override
    public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
        return this.getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE).getTemplates();
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return cloudServiceManager.getServiceInfoSnapshots(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getEnvironment() == environment);
    }

    @Override
    public Collection<ServiceTemplate> getTemplateStorageTemplates(String serviceName) {
        Validate.checkNotNull(serviceName);

        Collection<ServiceTemplate> collection = Iterables.newArrayList();

        if (servicesRegistry.containsService(ITemplateStorage.class, serviceName)) {
            collection.addAll(servicesRegistry.getService(ITemplateStorage.class, serviceName).getTemplates());
        }

        return collection;
    }

    @Override
    public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

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
    public void addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getPermissionManagement().addUser(permissionUser);
    }

    @Override
    public void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getPermissionManagement().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(String name) {
        Validate.checkNotNull(name);

        getPermissionManagement().deleteUser(name);
    }

    @Override
    public void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getPermissionManagement().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return getPermissionManagement().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        return getPermissionManagement().containsUser(name);
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return getPermissionManagement().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUser(String name) {
        Validate.checkNotNull(name);

        return getPermissionManagement().getUser(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return getPermissionManagement().getUsers();
    }

    @Override
    public void setUsers(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        getPermissionManagement().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUserByGroup(String group) {
        Validate.checkNotNull(group);

        return getPermissionManagement().getUserByGroup(group);
    }

    @Override
    public void addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        getPermissionManagement().addGroup(permissionGroup);
    }

    @Override
    public void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        getPermissionManagement().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(String group) {
        Validate.checkNotNull(group);

        getPermissionManagement().deleteGroup(group);
    }

    @Override
    public void deleteGroup(IPermissionGroup group) {
        Validate.checkNotNull(group);

        getPermissionManagement().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(String group) {
        Validate.checkNotNull(group);

        return getPermissionManagement().containsGroup(group);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        return getPermissionManagement().getGroup(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return getPermissionManagement().getGroups();
    }

    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        getPermissionManagement().setGroups(groups);
    }

    @Override
    public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
        return this.scheduleTask(this::getConsoleCommands);
    }

    @Override
    public ITask<CommandInfo> getConsoleCommandAsync(String commandLine) {
        return this.scheduleTask(() -> this.getConsoleCommand(commandLine));
    }

    @Override
    public ITask<String[]> sendCommandLineAsync(String commandLine) {
        return scheduleTask(() -> CloudNet.this.sendCommandLine(commandLine));
    }

    @Override
    public ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine) {
        return scheduleTask(() -> CloudNet.this.sendCommandLine(nodeUniqueId, commandLine));
    }

    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return scheduleTask(() -> CloudNet.this.createCloudService(serviceTask));
    }

    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        return scheduleTask(() -> CloudNet.this.createCloudService(serviceConfiguration));
    }

    @Override
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        return scheduleTask(() -> CloudNet.this.createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        return scheduleTask(() -> CloudNet.this.createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port));
    }

    @Override
    public ITask<ServiceInfoSnapshot> sendCommandLineToCloudServiceAsync(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        return scheduleTask(() -> CloudNet.this.sendCommandLineToCloudService(uniqueId, commandLine));
    }

    @Override
    public ITask<ServiceInfoSnapshot> addServiceTemplateToCloudServiceAsync(UUID uniqueId, ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        return scheduleTask(() -> CloudNet.this.addServiceTemplateToCloudService(uniqueId, serviceTemplate));
    }

    @Override
    public ITask<ServiceInfoSnapshot> addServiceRemoteInclusionToCloudServiceAsync(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        return scheduleTask(() -> CloudNet.this.addServiceRemoteInclusionToCloudService(uniqueId, serviceRemoteInclusion));
    }

    @Override
    public ITask<ServiceInfoSnapshot> addServiceDeploymentToCloudServiceAsync(UUID uniqueId, ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        return scheduleTask(() -> CloudNet.this.addServiceDeploymentToCloudService(uniqueId, serviceDeployment));
    }

    @Override
    public ITask<Queue<String>> getCachedLogMessagesFromServiceAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.getCachedLogMessagesFromService(uniqueId));
    }

    @Override
    public void includeWaitingServiceTemplates(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return;
        }

        ICloudService cloudService = getCloudServiceManager().getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.includeTemplates();
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = getCloudServiceManager().getGlobalServiceInfoSnapshots().get(uniqueId);

        if (serviceInfoSnapshot != null) {
            IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                clusterNodeServer.includeWaitingServiceTemplates(uniqueId);
            }
        }
    }

    @Override
    public void includeWaitingServiceInclusions(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return;
        }

        ICloudService cloudService = getCloudServiceManager().getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.includeInclusions();
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = getCloudServiceManager().getGlobalServiceInfoSnapshots().get(uniqueId);

        if (serviceInfoSnapshot != null) {
            IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                clusterNodeServer.includeWaitingServiceInclusions(uniqueId);
            }
        }
    }

    @Override
    public void deployResources(UUID uniqueId, boolean removeDeployments) {
        Validate.checkNotNull(uniqueId);

        if (!getCloudServiceManager().getGlobalServiceInfoSnapshots().containsKey(uniqueId)) {
            return;
        }

        ICloudService cloudService = getCloudServiceManager().getCloudService(uniqueId);

        if (cloudService != null) {
            cloudService.deployResources(removeDeployments);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = getCloudServiceManager().getGlobalServiceInfoSnapshots().get(uniqueId);

        if (serviceInfoSnapshot != null) {
            IClusterNodeServer clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                clusterNodeServer.deployResources(uniqueId);
            }
        }
    }

    @Override
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return scheduleTask(CloudNet.this::getServicesAsUniqueId);
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return scheduleTask(() -> CloudNet.this.getCloudServiceByName(name));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return scheduleTask(CloudNet.this::getCloudServices);
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServiceInfoSnapshotsAsync() {
        return scheduleTask(CloudNet.this::getStartedCloudServices);
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return scheduleTask(() -> CloudNet.this.getCloudService(taskName));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return scheduleTask(() -> CloudNet.this.getCloudServiceByGroup(group));
    }

    @Override
    public ITask<Integer> getServicesCountAsync() {
        return scheduleTask(CloudNet.this::getServicesCount);
    }

    @Override
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return scheduleTask(() -> CloudNet.this.getServicesCountByGroup(group));
    }

    @Override
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return scheduleTask(() -> CloudNet.this.getServicesCountByTask(taskName));
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServicesAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.getCloudService(uniqueId));
    }

    @Override
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return scheduleTask(CloudNet.this::getPermanentServiceTasks);
    }

    @Override
    public ITask<ServiceTask> getServiceTaskAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.getServiceTask(name));
    }

    @Override
    public ITask<Boolean> isServiceTaskPresentAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.isServiceTaskPresent(name));
    }

    @Override
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return scheduleTask(CloudNet.this::getGroupConfigurations);
    }

    @Override
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.getGroupConfiguration(name));
    }

    @Override
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.isGroupConfigurationPresent(name));
    }

    @Override
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return scheduleTask(CloudNet.this::getNodes);
    }

    @Override
    public ITask<NetworkClusterNode> getNodeAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.getNode(uniqueId));
    }

    @Override
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return scheduleTask(CloudNet.this::getNodeInfoSnapshots);
    }

    @Override
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.getNodeInfoSnapshot(uniqueId));
    }

    @Override
    public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return scheduleTask(CloudNet.this::getLocalTemplateStorageTemplates);
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return scheduleTask(() -> CloudNet.this.getCloudServices(environment));
    }

    @Override
    public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(String serviceName) {
        Validate.checkNotNull(serviceName);

        return scheduleTask(() -> CloudNet.this.getTemplateStorageTemplates(serviceName));
    }

    @Override
    public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        return scheduleTask(() -> CloudNet.this.sendCommandLineAsPermissionUser(uniqueId, commandLine));
    }

    @Override
    public ITask<Void> addUserAsync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        return scheduleTask(() -> null);
    }

    @Override
    public ITask<Boolean> containsUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.containsUser(uniqueId));
    }

    @Override
    public ITask<Boolean> containsUserAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.containsUser(name));
    }

    @Override
    public ITask<IPermissionUser> getUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return scheduleTask(() -> CloudNet.this.getUser(uniqueId));
    }

    @Override
    public ITask<List<IPermissionUser>> getUserAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.getUser(name));
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return scheduleTask(CloudNet.this::getUsers);
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUserByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return scheduleTask(() -> CloudNet.this.getUserByGroup(group));
    }

    @Override
    public ITask<Boolean> containsGroupAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.containsGroup(name));
    }

    @Override
    public ITask<IPermissionGroup> getGroupAsync(String name) {
        Validate.checkNotNull(name);

        return scheduleTask(() -> CloudNet.this.getGroup(name));
    }

    @Override
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return scheduleTask(CloudNet.this::getGroups);
    }

    public <T> ITask<T> runTask(Callable<T> runnable) {
        ITask<T> task = new ListenableTask<>(runnable);

        this.processQueue.offer(task);
        return task;
    }

    public ITask<?> runTask(Runnable runnable) {
        return this.runTask(Executors.callable(runnable));
    }

    public boolean isMainThread() {
        return Thread.currentThread().getName().equals("Application-Thread");
    }

    public void deployTemplateInCluster(ServiceTemplate serviceTemplate, byte[] resource) {
        Validate.checkNotNull(serviceTemplate);
        Validate.checkNotNull(resource);

        this.getClusterNodeServerProvider().deployTemplateInCluster(serviceTemplate, resource);
    }

    public void updateServiceTasksInCluster(Collection<ServiceTask> serviceTasks, NetworkUpdateType updateType) {
        this.getClusterNodeServerProvider().sendPacket(new PacketServerSetServiceTaskList(serviceTasks, updateType));
    }

    public void updateGroupConfigurationsInCluster(Collection<GroupConfiguration> groupConfigurations, NetworkUpdateType updateType) {
        this.getClusterNodeServerProvider().sendPacket(new PacketServerSetGroupConfigurationList(groupConfigurations, updateType));
    }

    public ITask<Void> sendAllAsync(IPacket packet) {
        return scheduleTask(() -> {
            sendAll(packet);
            return null;
        });
    }

    public void sendAll(IPacket packet) {
        Validate.checkNotNull(packet);

        for (IClusterNodeServer clusterNodeServer : getClusterNodeServerProvider().getNodeServers()) {
            clusterNodeServer.saveSendPacket(packet);
        }

        for (ICloudService cloudService : getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }

    public ITask<Void> sendAllAsync(IPacket... packets) {
        return scheduleTask(() -> {
            sendAll(packets);
            return null;
        });
    }

    public void sendAll(IPacket... packets) {
        Validate.checkNotNull(packets);

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
                        Iterables.map(Thread.getAllStackTraces().keySet(), thread -> new ThreadSnapshot(thread.getId(), thread.getName(), thread.getState(), thread.isDaemon(), thread.getPriority())),
                        CPUUsageResolver.getProcessCPUUsage()
                ),
                Iterables.map(this.moduleProvider.getModules(), moduleWrapper -> new NetworkClusterNodeExtensionSnapshot(
                        moduleWrapper.getModuleConfiguration().getGroup(),
                        moduleWrapper.getModuleConfiguration().getName(),
                        moduleWrapper.getModuleConfiguration().getVersion(),
                        moduleWrapper.getModuleConfiguration().getAuthor(),
                        moduleWrapper.getModuleConfiguration().getWebsite(),
                        moduleWrapper.getModuleConfiguration().getDescription()
                )),
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()
        );
    }

    public Collection<IClusterNodeServer> getValidClusterNodeServers(ServiceTask serviceTask) {
        return Iterables.filter(clusterNodeServerProvider.getNodeServers(), clusterNodeServer -> clusterNodeServer.isConnected() && clusterNodeServer.getNodeInfoSnapshot() != null && (
                serviceTask.getAssociatedNodes().isEmpty() || serviceTask.getAssociatedNodes().contains(clusterNodeServer.getNodeInfo().getUniqueId())
        ));
    }

    public NetworkClusterNodeInfoSnapshot searchLogicNode(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        Collection<NetworkClusterNodeInfoSnapshot> nodes = this.getValidClusterNodeServers(serviceTask).stream().map(IClusterNodeServer::getNodeInfoSnapshot).filter(Objects::nonNull).collect(Collectors.toList());
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
            if (
                    clusterNodeServer.getNodeInfoSnapshot() != null &&
                            (clusterNodeServer.getNodeInfoSnapshot().getMaxMemory() - clusterNodeServer.getNodeInfoSnapshot().getReservedMemory())
                                    > (this.currentNetworkClusterNodeInfoSnapshot.getMaxMemory() - this.currentNetworkClusterNodeInfoSnapshot.getReservedMemory()) &&
                            (clusterNodeServer.getNodeInfoSnapshot().getProcessSnapshot().getCpuUsage() * clusterNodeServer.getNodeInfoSnapshot().getCurrentServicesCount())
                                    < (this.currentNetworkClusterNodeInfoSnapshot.getProcessSnapshot().getCpuUsage() *
                                    this.currentNetworkClusterNodeInfoSnapshot.getCurrentServicesCount())
            ) {
                allow = false;
            }
        }

        return clusterNodeServers.size() == 0 || allow;
    }

    public void unregisterPacketListenersByClassLoader(ClassLoader classLoader) {
        Validate.checkNotNull(classLoader);

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

    public void publishPermissionUserUpdates(Collection<IPermissionUser> permissionUsers, NetworkUpdateType updateType) {
        if (this.permissionManagement instanceof DefaultJsonFilePermissionManagement) {
            this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(permissionUsers, updateType));
        }
    }

    public void publishPermissionGroupUpdates(Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType) {
        this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(permissionGroups, updateType, true));
    }

    public void publishUpdateJsonPermissionManagement() {
        if (this.permissionManagement instanceof DefaultJsonFilePermissionManagement) {
            this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(
                    this.permissionManagement.getUsers(),
                    this.permissionManagement.getGroups(),
                    NetworkUpdateType.ADD
            ));
        }

        if (this.permissionManagement instanceof DefaultDatabasePermissionManagement) {
            this.clusterNodeServerProvider.sendPacket(new PacketServerSetPermissionData(
                    this.permissionManagement.getGroups(),
                    NetworkUpdateType.ADD,
                    true
            ));
        }
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

    public void publishH2DatabaseDataToCluster() {
        if (databaseProvider instanceof H2DatabaseProvider) {
            Map<String, Map<String, JsonDocument>> map = allocateDatabaseData();

            clusterNodeServerProvider.sendPacket(new PacketServerSetH2DatabaseData(map, NetworkUpdateType.ADD));

            for (Map.Entry<String, Map<String, JsonDocument>> entry : map.entrySet()) {
                entry.getValue().clear();
            }

            map.clear();
        }
    }

    private Map<String, Map<String, JsonDocument>> allocateDatabaseData() {
        Map<String, Map<String, JsonDocument>> map = Maps.newHashMap();

        for (String name : databaseProvider.getDatabaseNames()) {
            if (!map.containsKey(name)) {
                map.put(name, Maps.newHashMap());
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

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void launchServices() {
        for (ServiceTask serviceTask : cloudServiceManager.getServiceTasks()) {
            if (serviceTask.canStartServices()) {
                if ((serviceTask.getAssociatedNodes().isEmpty() || (serviceTask.getAssociatedNodes().contains(getConfig().getIdentity().getUniqueId()))) &&
                        serviceTask.getMinServiceCount() > cloudServiceManager.getServiceInfoSnapshots(serviceTask.getName()).size()) {
                    if (competeWithCluster(serviceTask)) {
                        ICloudService cloudService = cloudServiceManager.runTask(serviceTask);

                        if (cloudService != null) {
                            try {
                                cloudService.start();
                            } catch (Exception e) {
                                e.printStackTrace();
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

    private void initDefaultConfigDefaultHostAddress() throws Exception {
        if (!this.config.isFileExists()) {
            String input;

            do {
                if (System.getProperty("cloudnet.config.default-address") != null) {
                    this.config.setDefaultHostAddress(System.getProperty("cloudnet.config.default-address"));
                    break;
                }

                if (System.getenv("CLOUDNET_CONFIG_IP_ADDRESS") != null) {
                    this.config.setDefaultHostAddress(System.getenv("CLOUDNET_CONFIG_IP_ADDRESS"));
                    break;
                }

                logger.info(ConsoleColor.DARK_GRAY + LanguageManager.getMessage("cloudnet-init-config-hostaddress-input"));

                console.resetPrompt();
                console.setPrompt(ConsoleColor.WHITE.toString());
                input = console.readLineNoPrompt();
                console.setPrompt(ConsoleColor.DEFAULT.toString());
                console.resetPrompt();

                if (!input.equals("127.0.1.1") && input.split("\\.").length == 4) {
                    this.config.setDefaultHostAddress(input);
                    break;

                } else {
                    logger.warning(ConsoleColor.RED + LanguageManager.getMessage("cloudnet-init-config-hostaddress-input-invalid"));
                }

            } while (true);
        }
    }

    private void initDefaultPermissionGroups() {
        if (permissionManagement.getGroups().isEmpty() && System.getProperty("cloudnet.default.permissions.skip") == null) {
            IPermissionGroup adminPermissionGroup = new PermissionGroup("Admin", 100);
            adminPermissionGroup.addPermission("*");
            adminPermissionGroup.addPermission("Proxy", "*");
            adminPermissionGroup.setPrefix("&4Admin &8| &7");
            adminPermissionGroup.setColor("&7");
            adminPermissionGroup.setSuffix("&f");
            adminPermissionGroup.setDisplay("&4");
            adminPermissionGroup.setSortId(10);

            permissionManagement.addGroup(adminPermissionGroup);

            IPermissionGroup defaultPermissionGroup = new PermissionGroup("default", 100);
            defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
            defaultPermissionGroup.setDefaultGroup(true);
            defaultPermissionGroup.setPrefix("&7");
            defaultPermissionGroup.setColor("&7");
            defaultPermissionGroup.setSuffix("&f");
            defaultPermissionGroup.setDisplay("&7");
            defaultPermissionGroup.setSortId(10);

            permissionManagement.addGroup(defaultPermissionGroup);
        }
    }

    private void initDefaultTasks() throws Exception {
        if (cloudServiceManager.getGroupConfigurations().isEmpty() && cloudServiceManager.getServiceTasks().isEmpty() &&
                System.getProperty("cloudnet.default.tasks.skip") == null) {
            boolean value = false;
            String input;

            do {
                if (value) {
                    break;
                }

                if (System.getProperty("cloudnet.default.tasks.installation") != null) {
                    input = System.getProperty("cloudnet.default.tasks.installation");
                    value = true;

                } else if (System.getenv("CLOUDNET_DEFAULT_TASKS_INSTALLATION") != null) {
                    input = System.getenv("CLOUDNET_DEFAULT_TASKS_INSTALLATION");
                    value = true;

                } else {
                    logger.info(ConsoleColor.DARK_GRAY + LanguageManager.getMessage("cloudnet-init-default-tasks-input"));
                    logger.info(ConsoleColor.DARK_GRAY + LanguageManager.getMessage("cloudnet-init-default-tasks-input-list"));

                    console.resetPrompt();
                    console.setPrompt(ConsoleColor.WHITE.toString());
                    input = console.readLineNoPrompt();
                    console.setPrompt(ConsoleColor.DEFAULT.toString());
                    console.resetPrompt();
                }

                boolean doBreak = false;

                switch (input.trim().toLowerCase()) {
                    case "recommended":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy bungeecord");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");

                        //Create groups
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create group Global-Server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create group Global-Proxy");

                        //Add groups
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy add group Global-Proxy");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby add group Global-Server");

                        //Install
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt create Global bukkit minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Global bukkit minecraft_server paperspigot-1.12.2");

                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt create Global proxy bungeecord");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Global proxy bungeecord default");

                        //Add templates
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks group Global-Server add template local Global bukkit");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks group Global-Proxy add template local Global proxy");

                        //Set configurations
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");

                        doBreak = true;
                        break;
                    case "java-bungee-1.7.10":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy bungeecord");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default bungeecord travertine");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default minecraft_server spigot-1.7.10");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "java-bungee-1.8.8":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy bungeecord");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default bungeecord default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default minecraft_server spigot-1.8.8");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "java-bungee-1.13.2":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy bungeecord");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default bungeecord default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default minecraft_server spigot-1.13.2");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "java-velocity-1.8.8":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy velocity");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default velocity default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default minecraft_server spigot-1.8.8");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "java-velocity-1.13.2":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy velocity");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby minecraft_server");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default velocity default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default minecraft_server spigot-1.13.2");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "bedrock":
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Proxy waterdog");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks create task Lobby nukkit");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Proxy default waterdog default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "lt install Lobby default nukkit default");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Proxy set minServiceCount 1");
                        this.commandMap.dispatchCommand(this.consoleCommandSender, "tasks task Lobby set minServiceCount 1");
                        doBreak = true;
                        break;
                    case "nothing":
                        doBreak = true;
                        break;
                    default:
                        this.logger.warning(ConsoleColor.RED + LanguageManager.getMessage("cloudnet-init-default-tasks-input-invalid"));
                        break;
                }

                if (doBreak) {
                    break;
                }

            } while (true);
        }
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
                new CommandService(),
                new CommandCreate(),
                new CommandCluster(),
                new CommandModules(),
                new CommandLocalTemplate(),
                new CommandMe(),
                new CommandScreen(),
                new CommandPermissions(),
                new CommandCopy()
        );
    }

    private <T> ITask<T> scheduleTask(Callable<T> callable) {
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
        ((JLine2Console) console).getConsoleReader().addCompleter(new JLine2CommandCompleter(this.commandMap));
    }

    private void setDefaultRegistryEntries() {
        this.configurationRegistry.getString("permission_service", "json_database");
        this.configurationRegistry.getString("database_provider", "h2");

        this.configurationRegistry.save();
    }

    private void registerDefaultServices() {
        this.servicesRegistry.registerService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE,
                new LocalTemplateStorage(new File(System.getProperty("cloudnet.storage.local", "local/templates"))));

        this.servicesRegistry.registerService(IPermissionManagement.class, "json_file",
                new DefaultJsonFilePermissionManagement(new File(System.getProperty("cloudnet.permissions.json.path", "local/perms.json"))));

        this.servicesRegistry.registerService(IPermissionManagement.class, "json_database",
                new DefaultDatabasePermissionManagement(this::getDatabaseProvider));

        this.servicesRegistry.registerService(AbstractDatabaseProvider.class, "h2",
                new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"), taskScheduler));
    }

    private void runConsole() {
        Thread console = new Thread(() -> {
            try {
                if (!getCommandLineArguments().contains("--noconsole")) {
                    logger.info(LanguageManager.getMessage("console-ready"));

                    String input;
                    while ((input = getConsole().readLine()) != null) {
                        try {
                            if (input.trim().isEmpty()) {
                                continue;
                            }

                            CommandPreProcessEvent commandPreProcessEvent = new CommandPreProcessEvent(input, getConsoleCommandSender());
                            getEventManager().callEvent(commandPreProcessEvent);

                            if (commandPreProcessEvent.isCancelled()) {
                                continue;
                            }

                            if (!getCommandMap().dispatchCommand(getConsoleCommandSender(), input)) {
                                getEventManager().callEvent(new CommandNotFoundEvent(input));
                                logger.warning(LanguageManager.getMessage("command-not-found"));

                                continue;
                            }

                            getEventManager().callEvent(new CommandPostProcessEvent(input, getConsoleCommandSender()));

                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    while (RUNNING) {
                        Thread.sleep(1000);
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });

        console.setName("Console-Thread");
        console.setPriority(Thread.MIN_PRIORITY);
        console.setDaemon(true);
        console.start();
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

    public INetworkClient getNetworkClient() {
        return this.networkClient;
    }

    @Override
    public Collection<CommandInfo> getConsoleCommands() {
        return this.commandMap.getCommandInfos();
    }

    @Override
    public CommandInfo getConsoleCommand(String commandLine) {
        Command command = this.commandMap.getCommandFromLine(commandLine);
        return command != null ? command.getInfo() : null;
    }

    public INetworkServer getNetworkServer() {
        return this.networkServer;
    }

    public IHttpServer getHttpServer() {
        return this.httpServer;
    }

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
