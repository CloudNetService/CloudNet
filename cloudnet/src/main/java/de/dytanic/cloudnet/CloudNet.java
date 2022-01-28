/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.cluster.DefaultClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.DefaultCommandMap;
import de.dytanic.cloudnet.command.ICommandMap;
import de.dytanic.cloudnet.command.commands.CommandClear;
import de.dytanic.cloudnet.command.commands.CommandCluster;
import de.dytanic.cloudnet.command.commands.CommandCopy;
import de.dytanic.cloudnet.command.commands.CommandCreate;
import de.dytanic.cloudnet.command.commands.CommandDebug;
import de.dytanic.cloudnet.command.commands.CommandExit;
import de.dytanic.cloudnet.command.commands.CommandGroups;
import de.dytanic.cloudnet.command.commands.CommandHelp;
import de.dytanic.cloudnet.command.commands.CommandMe;
import de.dytanic.cloudnet.command.commands.CommandModules;
import de.dytanic.cloudnet.command.commands.CommandPermissions;
import de.dytanic.cloudnet.command.commands.CommandReload;
import de.dytanic.cloudnet.command.commands.CommandScreen;
import de.dytanic.cloudnet.command.commands.CommandService;
import de.dytanic.cloudnet.command.commands.CommandTasks;
import de.dytanic.cloudnet.command.commands.CommandTemplate;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
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
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.database.Database;
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
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
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
import de.dytanic.cloudnet.network.listener.PacketServerSetGlobalLogLevelListener;
import de.dytanic.cloudnet.network.listener.auth.PacketClientAuthorizationListener;
import de.dytanic.cloudnet.network.listener.auth.PacketServerAuthorizationResponseListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerClusterNodeInfoUpdateListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerDeployLocalTemplateListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerH2DatabaseListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerServiceInfoPublisherListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSetGlobalServiceInfoListListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSetGroupConfigurationListListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSetH2DatabaseDataListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSetPermissionDataListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSetServiceTaskListListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSyncTemplateStorageChunkListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSyncTemplateStorageListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerUpdatePermissionsListener;
import de.dytanic.cloudnet.network.listener.driver.PacketServerDriverAPIListener;
import de.dytanic.cloudnet.network.packet.PacketServerClusterNodeInfoUpdate;
import de.dytanic.cloudnet.network.packet.PacketServerSetGroupConfigurationList;
import de.dytanic.cloudnet.network.packet.PacketServerSetH2DatabaseData;
import de.dytanic.cloudnet.network.packet.PacketServerSetPermissionData;
import de.dytanic.cloudnet.network.packet.PacketServerSetServiceTaskList;
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
import de.dytanic.cloudnet.provider.service.NodeCloudServiceFactory;
import de.dytanic.cloudnet.provider.service.NodeGeneralCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.defaults.DefaultCloudServiceManager;
import de.dytanic.cloudnet.setup.DefaultInstallation;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CloudNet extends CloudNetDriver {

  public static final int TPS = 10;
  private static CloudNet instance;

  private final long startupMillis = System.currentTimeMillis();

  private final CloudNetTick mainLoop = new CloudNetTick(this);
  private final ScheduledExecutorService prioritizedTaskScheduler = Executors.newScheduledThreadPool(4);

  private final LogLevel defaultLogLevel = LogLevel
    .getDefaultLogLevel(System.getProperty("cloudnet.logging.defaultlevel")).orElse(LogLevel.FATAL);

  private final Path moduleDirectory = Paths.get(System.getProperty("cloudnet.modules.directory", "modules"));

  private final IConfiguration config = new JsonConfiguration();
  private final IConfigurationRegistry configurationRegistry = new JsonConfigurationRegistry(
    Paths.get(System.getProperty("cloudnet.registry.global.path", "local/registry")));

  private final ITaskScheduler networkTaskScheduler = new DefaultTaskScheduler();

  private final List<String> commandLineArguments;
  private final Properties commandLineProperties;

  private final IConsole console;
  private final ICommandMap commandMap = new DefaultCommandMap();
  private final DefaultCloudServiceManager cloudServiceManager;

  private final QueuedConsoleLogHandler queuedConsoleLogHandler;
  private final ConsoleCommandSender consoleCommandSender;

  private final DefaultInstallation defaultInstallation = new DefaultInstallation();
  private final ServiceVersionProvider serviceVersionProvider = new ServiceVersionProvider();

  private INetworkClient networkClient;
  private INetworkServer networkServer;
  private IHttpServer httpServer;

  private AbstractDatabaseProvider databaseProvider;
  private IClusterNodeServerProvider clusterNodeServerProvider;

  private volatile boolean running = true;

  CloudNet(List<String> commandLineArguments, ILogger logger, IConsole console) {
    super(logger);
    setInstance(this);

    logger.setLevel(this.defaultLogLevel);

    this.cloudServiceManager = new DefaultCloudServiceManager(this.prioritizedTaskScheduler);

    super.cloudServiceFactory = new NodeCloudServiceFactory(this, this.cloudServiceManager);
    super.generalCloudServiceProvider = new NodeGeneralCloudServiceProvider(this);
    super.serviceTaskProvider = new NodeServiceTaskProvider(this);
    super.groupConfigurationProvider = new NodeGroupConfigurationProvider(this);
    super.nodeInfoProvider = new NodeNodeInfoProvider(this);
    super.messenger = new NodeMessenger(this);

    this.console = console;
    this.commandLineArguments = commandLineArguments;
    this.commandLineProperties = Properties.parseLine(commandLineArguments.toArray(new String[0]));

    this.consoleCommandSender = new ConsoleCommandSender(logger);

    logger.addLogHandler(this.queuedConsoleLogHandler = new QueuedConsoleLogHandler());

    this.serviceTaskProvider.reload();
    this.groupConfigurationProvider.reload();

    this.moduleProvider.setModuleProviderHandler(new NodeModuleProviderHandler());
    this.moduleProvider.setModuleDependencyLoader(new DefaultPersistableModuleDependencyLoader(
      Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"), "libs")));

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
    Path tempDirectory = Paths.get(System.getProperty("cloudnet.tempDir", "temp"));
    FileUtils.createDirectoryReported(tempDirectory);

    Path cachesDirectory = tempDirectory.resolve("caches");
    FileUtils.createDirectoryReported(cachesDirectory);

    try (InputStream inputStream = CloudNet.class.getClassLoader().getResourceAsStream("wrapper.jar")) {
      Preconditions.checkNotNull(inputStream, "Missing wrapper.jar");
      Files.copy(inputStream, cachesDirectory.resolve("wrapper.jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    this.initServiceVersions();

    boolean configFileAvailable = this.config.isFileExists();
    this.config.load();

    this.defaultInstallation.executeFirstStartSetup(this.console, configFileAvailable);
    this.clusterNodeServerProvider = new DefaultClusterNodeServerProvider(this, this.prioritizedTaskScheduler);

    HeaderReader.readAndPrintHeader(this.console);

    if (this.config.getMaxMemory() < 2048) {
      CloudNetDriver.getInstance().getLogger()
        .warning(LanguageManager.getMessage("cloudnet-init-config-low-memory-warning"));
    }

    this.networkClient = new NettyNetworkClient(
      NetworkClientChannelHandlerImpl::new,
      this.config.getClientSslConfig().isEnabled() ? this.config.getClientSslConfig().toSslConfiguration() : null
    );
    this.networkServer = new NettyNetworkServer(
      this.config.getServerSslConfig().isEnabled() ? this.config.getServerSslConfig().toSslConfiguration() : null,
      NetworkServerChannelHandlerImpl::new
    );
    this.httpServer = new NettyHttpServer(
      this.config.getWebSslConfig().isEnabled() ? this.config.getWebSslConfig().toSslConfiguration() : null);

    this.initPacketRegistryListeners();
    this.clusterNodeServerProvider.setClusterServers(this.config.getClusterConfig());

    this.enableCommandCompleter();
    this.setDefaultRegistryEntries();

    this.registerDefaultCommands();
    this.registerDefaultServices();

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

    this.setPermissionManagement(new DefaultDatabasePermissionManagement(this::getDatabaseProvider));

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
    for (HostAndPort hostAndPort : this.config.getIdentity().getListeners()) {
      this.logger.info(LanguageManager.getMessage("cloudnet-network-server-bind")
        .replace("%address%", hostAndPort.getHost() + ":" + hostAndPort.getPort()));

      this.networkServer.addListener(hostAndPort);
    }

    for (HostAndPort hostAndPort : this.config.getHttpListeners()) {
      this.logger.info(LanguageManager.getMessage("cloudnet-http-server-bind")
        .replace("%address%", hostAndPort.getHost() + ":" + hostAndPort.getPort()));

      this.httpServer.addListener(hostAndPort);
    }

    Random random = new Random();
    for (NetworkClusterNode node : this.config.getClusterConfig().getNodes()) {
      if (!this.networkClient.connect(node.getListeners()[random.nextInt(node.getListeners().length)])) {
        this.logger.log(LogLevel.WARNING, LanguageManager.getMessage("cluster-server-networking-connection-refused"));
      }
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

    this.serviceVersionProvider.interruptInstallSteps();

    this.cloudServiceManager.deleteAllCloudServices();
    this.scheduler.shutdownNow();
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

      FileUtils.delete(Paths.get(System.getProperty("cloudnet.tempDir", "temp")));

      this.logger.close();
      this.console.close();

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    if (!Thread.currentThread().getName().equals("Shutdown Thread")) {
      System.exit(0);
    }
  }

  @Override
  public @NotNull String getComponentName() {
    return this.config.getIdentity().getUniqueId();
  }

  @Override
  public @NotNull String getNodeUniqueId() {
    return this.getComponentName();
  }

  public boolean isRunning() {
    return this.running;
  }

  public LogLevel getDefaultLogLevel() {
    return this.defaultLogLevel;
  }

  @Override
  public void setPermissionManagement(@NotNull IPermissionManagement permissionManagement) {
    super.setPermissionManagement(permissionManagement);
    permissionManagement.init();
    if (permissionManagement instanceof NodePermissionManagement) {
      ((NodePermissionManagement) permissionManagement)
        .setPermissionManagementHandler(new DefaultPermissionManagementHandler());
    }
  }

  @Override
  public @NotNull TemplateStorage getLocalTemplateStorage() {
    TemplateStorage storage = this.getTemplateStorage(ServiceTemplate.LOCAL_STORAGE);
    if (storage == null) {
      throw new IllegalStateException("No local TemplateStorage registered");
    }
    return storage;
  }

  @Override
  public @Nullable TemplateStorage getTemplateStorage(String storage) {
    return this.servicesRegistry.getService(TemplateStorage.class, storage);
  }

  @Override
  public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
    return this.servicesRegistry.getServices(TemplateStorage.class);
  }

  @Override
  public @NotNull ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync() {
    return CompletedTask.create(this.getAvailableTemplateStorages());
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
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.selectCloudServiceProvider(serviceInfoSnapshot);
  }

  @NotNull
  private SpecificCloudServiceProvider selectCloudServiceProvider(@Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot == null) {
      return EmptySpecificCloudServiceProvider.INSTANCE;
    }

    if (serviceInfoSnapshot.getServiceId().getNodeUniqueId().equals(this.getComponentName())) {
      // can never be null
      return Objects
        .requireNonNull(this.clusterNodeServerProvider.getSelfNode().getCloudServiceProvider(serviceInfoSnapshot));
    }

    IClusterNodeServer server = this.clusterNodeServerProvider
      .getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());
    if (server == null) {
      return EmptySpecificCloudServiceProvider.INSTANCE;
    }

    SpecificCloudServiceProvider provider = server.getCloudServiceProvider(serviceInfoSnapshot);
    return provider == null ? EmptySpecificCloudServiceProvider.INSTANCE : provider;
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
          .replace("%error%", "invalid json or incompatible file version")
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
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getUniqueId().toString().toLowerCase()
        .contains(argument.toLowerCase()))
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
      IPermissionUserCommandSender commandSender = new DefaultPermissionUserCommandSender(permissionUser,
        this.permissionManagement);
      boolean value = this.commandMap.dispatchCommand(commandSender, commandLine);

      return new Pair<>(value, commandSender.getWrittenMessages().toArray(new String[0]));
    } else {
      return new Pair<>(false, new String[0]);
    }
  }

  @Override
  @NotNull
  public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId,
    @NotNull String commandLine) {
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

  public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull InputStream inputStream) {
    Preconditions.checkNotNull(serviceTemplate);
    Preconditions.checkNotNull(inputStream);

    this.getClusterNodeServerProvider().deployTemplateInCluster(serviceTemplate, inputStream);
  }

  public void updateServiceTasksInCluster(Collection<ServiceTask> serviceTasks, NetworkUpdateType updateType) {
    this.getClusterNodeServerProvider().sendPacket(new PacketServerSetServiceTaskList(serviceTasks, updateType));
  }

  public void updateGroupConfigurationsInCluster(Collection<GroupConfiguration> groupConfigurations,
    NetworkUpdateType updateType) {
    this.getClusterNodeServerProvider()
      .sendPacket(new PacketServerSetGroupConfigurationList(groupConfigurations, updateType));
  }

  public void sendAllSync(@NotNull IPacket... packets) {
    Preconditions.checkNotNull(packets);

    this.getClusterNodeServerProvider().sendPacketSync(packets);

    for (ICloudService cloudService : this.getCloudServiceManager().getCloudServices().values()) {
      if (cloudService.getNetworkChannel() != null) {
        cloudService.getNetworkChannel().sendPacketSync(packets);
      }
    }
  }

  public void sendAll(@NotNull IPacket... packets) {
    Preconditions.checkNotNull(packets);

    this.getClusterNodeServerProvider().sendPacket(packets);

    for (ICloudService cloudService : this.getCloudServiceManager().getCloudServices().values()) {
      if (cloudService.getNetworkChannel() != null) {
        cloudService.getNetworkChannel().sendPacket(packets);
      }
    }
  }

  public NetworkClusterNodeInfoSnapshot createClusterNodeInfoSnapshot() {
    return new NetworkClusterNodeInfoSnapshot(
      System.currentTimeMillis(),
      this.startupMillis,
      this.config.getIdentity(),
      CloudNet.class.getPackage().getImplementationVersion(),
      this.cloudServiceManager.getCloudServices().size(),
      this.cloudServiceManager.getCurrentUsedHeapMemory(),
      this.cloudServiceManager.getCurrentReservedMemory(),
      this.config.getMaxMemory(),
      this.config.getMaxCPUUsageToStartServices(),
      ProcessSnapshot.self(),
      this.moduleProvider.getModules().stream().map(IModuleWrapper::getModuleConfiguration)
        .collect(Collectors.toList()),
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

  @Nullable
  public NodeServer searchLogicNodeServer(ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);

    return this.searchLogicNodeServer(serviceTask.getAssociatedNodes(),
      serviceTask.getProcessConfiguration().getMaxHeapMemorySize());
  }

  @Nullable
  public NodeServer searchLogicNodeServer(Collection<String> allowedNodes, int maxHeapMemory) {
    Preconditions.checkNotNull(allowedNodes);

    Collection<NodeServer> nodes = new ArrayList<>(this.getValidClusterNodeServers(allowedNodes));
    if (this.canStartServices(allowedNodes)) {
      nodes.add(this.clusterNodeServerProvider.getSelfNode());
    }

    boolean includeSystemCpuUsage = nodes.stream()
      .noneMatch(server -> server.getNodeInfoSnapshot().getSystemCpuUsage() < 0);
    return nodes.stream()
      .filter(node -> {
        NetworkClusterNodeInfoSnapshot info = node.getNodeInfoSnapshot();
        return info.getUsedMemory() + maxHeapMemory <= info.getMaxMemory()
          && info.getMaxCPUUsageToStartServices() >= info.getSystemCpuUsage();
      })
      .min(Comparator.comparingDouble(node -> {
        NetworkClusterNodeInfoSnapshot info = node.getNodeInfoSnapshot();
        return (includeSystemCpuUsage ? info.getSystemCpuUsage() : 0) +
          ((double) info.getReservedMemory() / info.getMaxMemory() * 100);
      }))
      .orElse(null);
  }

  public boolean canStartServices(Collection<String> allowedNodes, String nodeUniqueId) {
    return allowedNodes != null && (allowedNodes.isEmpty() || allowedNodes.contains(nodeUniqueId));
  }

  public boolean canStartServices(Collection<String> allowedNodes) {
    return this.canStartServices(allowedNodes, this.getConfig().getIdentity().getUniqueId());
  }

  public Collection<IClusterNodeServer> getValidClusterNodeServers(Collection<String> allowedNodes) {
    return this.clusterNodeServerProvider.getNodeServers()
      .stream()
      .filter(IClusterNodeServer::isConnected)
      .filter(server -> server.getNodeInfoSnapshot() != null)
      .filter(clusterNodeServer -> this.canStartServices(allowedNodes, clusterNodeServer.getNodeInfo().getUniqueId()))
      .collect(Collectors.toList());
  }

  @Nullable
  public NetworkClusterNodeInfoSnapshot searchLogicNode(Collection<String> allowedNodes) {
    Collection<NetworkClusterNodeInfoSnapshot> nodes = this.getValidClusterNodeServers(allowedNodes).stream()
      .map(IClusterNodeServer::getNodeInfoSnapshot)
      .collect(Collectors.toList());

    if (this.canStartServices(allowedNodes)) {
      nodes.add(this.clusterNodeServerProvider.getSelfNode().getNodeInfoSnapshot());
    }

    return nodes.stream()
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingLong(NetworkClusterNodeInfoSnapshot::getStartupMillis))
      .min(Comparator.comparingDouble(node ->
        node.getSystemCpuUsage() + ((double) node.getReservedMemory() / node.getMaxMemory() * 100D)
      )).orElse(null);
  }

  @Nullable
  public Pair<NodeServer, Set<ServiceInfoSnapshot>> searchLogicNodeServer(
    Map<String, Set<ServiceInfoSnapshot>> services) {
    Collection<NodeServer> nodes = new ArrayList<>(this.getValidClusterNodeServers(services.keySet()));
    if (this.canStartServices(services.keySet())) {
      nodes.add(this.clusterNodeServerProvider.getSelfNode());
    }

    boolean includeSystemCpuUsage = nodes.stream()
      .noneMatch(server -> server.getNodeInfoSnapshot().getSystemCpuUsage() < 0);
    return nodes.stream()
      .filter(Objects::nonNull)
      .map(server -> new Pair<>(server, services.get(server.getNodeInfo().getUniqueId())))
      .peek(pair -> pair.setSecond(pair.getSecond().stream()
        .filter(info -> {
          NetworkClusterNodeInfoSnapshot snapshot = pair.getFirst().getNodeInfoSnapshot();
          int usedAfterStart =
            snapshot.getUsedMemory() + info.getConfiguration().getProcessConfig().getMaxHeapMemorySize();

          return snapshot.getMaxMemory() >= usedAfterStart
            && snapshot.getMaxCPUUsageToStartServices() >= snapshot.getSystemCpuUsage();
        })
        .collect(Collectors.toSet()))
      )
      .filter(pair -> !pair.getSecond().isEmpty())
      .min(Comparator.comparingDouble(pair -> {
        NetworkClusterNodeInfoSnapshot snapshot = pair.getFirst().getNodeInfoSnapshot();
        return (includeSystemCpuUsage ? snapshot.getSystemCpuUsage() : 0) +
          ((double) snapshot.getReservedMemory() / snapshot.getMaxMemory() * 100);
      }))
      .orElse(null);
  }

  @Deprecated
  public boolean competeWithCluster(ServiceTask serviceTask) {
    return this.competeWithCluster(serviceTask.getAssociatedNodes());
  }

  @Deprecated
  public boolean competeWithCluster(Collection<String> allowedNodes) {
    NetworkClusterNodeInfoSnapshot bestNode = this.searchLogicNode(allowedNodes);
    return bestNode != null && bestNode.getNode().getUniqueId().equals(this.config.getIdentity().getUniqueId());
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
    NetworkClusterNodeInfoSnapshot snapshot = this.createClusterNodeInfoSnapshot();

    this.getEventManager().callEvent(new NetworkClusterNodeInfoConfigureEvent(snapshot));
    this.clusterNodeServerProvider.getSelfNode().setNodeInfoSnapshot(snapshot);
    this.clusterNodeServerProvider.sendPacket(new PacketServerClusterNodeInfoUpdate(snapshot));
  }

  public void publishPermissionGroupUpdates(Collection<IPermissionGroup> permissionGroups,
    NetworkUpdateType updateType) {
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
      Database database = this.databaseProvider.getDatabase(name);
      map.get(name).putAll(database.entries());
    }

    return map;
  }

  public void registerClusterPacketRegistryListeners(IPacketListenerRegistry registry, boolean client) {
    if (client) {
      registry
        .addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketServerAuthorizationResponseListener());
    }

    registry.addListener(PacketConstants.SERVICE_INFO_PUBLISH_CHANNEL, new PacketServerServiceInfoPublisherListener());
    registry.addListener(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL, new PacketServerUpdatePermissionsListener());
    registry.addListener(PacketConstants.CHANNEL_MESSAGING_CHANNEL, new PacketServerChannelMessageListener(false));

    registry.addListener(PacketConstants.CLUSTER_SERVICE_INFO_LIST_CHANNEL,
      new PacketServerSetGlobalServiceInfoListListener());
    registry.addListener(PacketConstants.CLUSTER_GROUP_CONFIG_LIST_CHANNEL,
      new PacketServerSetGroupConfigurationListListener());
    registry.addListener(PacketConstants.CLUSTER_TASK_LIST_CHANNEL, new PacketServerSetServiceTaskListListener());
    registry.addListener(PacketConstants.CLUSTER_TEMPLATE_STORAGE_SYNC_CHANNEL,
      new PacketServerSyncTemplateStorageListener());
    registry.addListener(PacketConstants.CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL,
      new PacketServerSyncTemplateStorageChunkListener(false));
    registry.addListener(PacketConstants.CLUSTER_PERMISSION_DATA_CHANNEL, new PacketServerSetPermissionDataListener());
    registry
      .addListener(PacketConstants.CLUSTER_TEMPLATE_DEPLOY_CHANNEL, new PacketServerDeployLocalTemplateListener());
    registry.addListener(PacketConstants.CLUSTER_NODE_INFO_CHANNEL, new PacketServerClusterNodeInfoUpdateListener());

    registry.addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerH2DatabaseListener());
    registry
      .addListener(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new PacketServerSetH2DatabaseDataListener());

    registry.addListener(PacketConstants.INTERNAL_DEBUGGING_CHANNEL, new PacketServerSetGlobalLogLevelListener(false));

    // Node server API
    registry.addListener(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, new PacketServerDriverAPIListener());
  }

  private void initPacketRegistryListeners() {
    IPacketListenerRegistry registry = this.getNetworkClient().getPacketRegistry();

    this.registerClusterPacketRegistryListeners(registry, true);

    this.getNetworkServer().getPacketRegistry()
      .addListener(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new PacketClientAuthorizationListener());
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
    this.scheduler.submit(task);
    return task;
  }

  private void enableModules() {
    this.loadModules();
    this.startModules();
  }

  private void loadModules() {
    this.logger.info(LanguageManager.getMessage("cloudnet-load-modules-createDirectory"));
    FileUtils.createDirectoryReported(this.moduleDirectory);

    this.logger.info(LanguageManager.getMessage("cloudnet-load-modules"));
    FileUtils.walkFileTree(this.moduleDirectory, (root, current) -> {
      this.logger.info(LanguageManager.getMessage("cloudnet-load-modules-found")
        .replace("%file_name%", current.getFileName().toString()));
      this.moduleProvider.loadModule(current);
    }, false, "*.{jar,war,zip}");
  }

  private void startModules() {
    for (IModuleWrapper moduleWrapper : this.moduleProvider.getModules()) {
      moduleWrapper.startModule();
    }
  }

  private void enableCommandCompleter() {
    this.console.addTabCompletionHandler(UUID.randomUUID(),
      (commandLine, args, properties) -> this.commandMap.tabCompleteCommand(commandLine));
  }

  private void setDefaultRegistryEntries() {
    this.configurationRegistry.getString("database_provider", "h2");

    this.configurationRegistry.save();
  }

  private void registerDefaultServices() {
    this.servicesRegistry.registerService(
      TemplateStorage.class,
      LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE,
      new LocalTemplateStorage(Paths.get(System.getProperty("cloudnet.storage.local", "local/templates")))
    );

    this.servicesRegistry.registerService(
      AbstractDatabaseProvider.class,
      "h2",
      new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"),
        !this.config.getClusterConfig().getNodes().isEmpty())
    );
  }

  private void runConsole() {
    this.logger.info(LanguageManager.getMessage("console-ready"));

    this.getConsole().addCommandHandler(UUID.randomUUID(), input -> {
      try {
        if (input.trim().isEmpty()) {
          return;
        }

        CommandPreProcessEvent commandPreProcessEvent = new CommandPreProcessEvent(input,
          this.getConsoleCommandSender());
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

  @Deprecated
  public File getModuleDirectory() {
    return this.moduleDirectory.toFile();
  }

  public Path getModuleDirectoryPath() {
    return this.moduleDirectory;
  }

  public IConfiguration getConfig() {
    return this.config;
  }

  public IConfigurationRegistry getConfigurationRegistry() {
    return this.configurationRegistry;
  }

  public DefaultCloudServiceManager getCloudServiceManager() {
    return this.cloudServiceManager;
  }

  public IClusterNodeServerProvider getClusterNodeServerProvider() {
    return this.clusterNodeServerProvider;
  }

  @Deprecated
  @ApiStatus.ScheduledForRemoval
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

  public @NotNull AbstractDatabaseProvider getDatabaseProvider() {
    return this.databaseProvider;
  }

  public NetworkClusterNodeInfoSnapshot getLastNetworkClusterNodeInfoSnapshot() {
    return this.clusterNodeServerProvider.getSelfNode().getLastNodeInfoSnapshot();
  }

  public NetworkClusterNodeInfoSnapshot getCurrentNetworkClusterNodeInfoSnapshot() {
    return this.clusterNodeServerProvider.getSelfNode().getNodeInfoSnapshot();
  }
}
