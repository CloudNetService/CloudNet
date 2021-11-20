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
import de.dytanic.cloudnet.cluster.sync.DataSyncRegistry;
import de.dytanic.cloudnet.cluster.sync.DefaultDataSyncRegistry;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.defaults.DefaultCommandProvider;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.log.defaults.DefaultLogFormatter;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.config.JsonConfiguration;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.util.HeaderReader;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.database.xodus.XodusDatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.CloudNetVersion;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.module.DefaultPersistableModuleDependencyLoader;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.event.CloudNetNodePostInitializationEvent;
import de.dytanic.cloudnet.log.QueuedConsoleLogHandler;
import de.dytanic.cloudnet.module.NodeModuleProviderHandler;
import de.dytanic.cloudnet.network.DefaultNetworkClientChannelHandler;
import de.dytanic.cloudnet.network.DefaultNetworkServerChannelHandler;
import de.dytanic.cloudnet.network.chunk.FileDeployCallbackListener;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.permission.DefaultPermissionManagementHandler;
import de.dytanic.cloudnet.permission.NodePermissionManagement;
import de.dytanic.cloudnet.permission.command.PermissionUserCommandSource;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import de.dytanic.cloudnet.provider.NodeMessenger;
import de.dytanic.cloudnet.provider.NodeNodeInfoProvider;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.defaults.DefaultCloudServiceManager;
import de.dytanic.cloudnet.service.defaults.NodeCloudServiceFactory;
import de.dytanic.cloudnet.setup.DefaultInstallation;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the implementation of the {@link CloudNetDriver} for nodes.
 */
public class CloudNet extends CloudNetDriver {

  private static final Logger LOGGER = LogManager.getLogger(CloudNet.class);
  private static final Path LAUNCHER_DIR = Paths.get(System.getProperty("cloudnet.launcher.dir", "launcher"));

  private final IConsole console;
  private final CommandProvider commandProvider;

  private final IHttpServer httpServer;
  private final INetworkClient networkClient;
  private final INetworkServer networkServer;

  private final ServiceVersionProvider serviceVersionProvider;
  private final DefaultClusterNodeServerProvider nodeServerProvider;

  private final CloudNetTick mainThread = new CloudNetTick(this);
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final DefaultInstallation installation = new DefaultInstallation();
  private final DataSyncRegistry dataSyncRegistry = new DefaultDataSyncRegistry();
  private final QueuedConsoleLogHandler logHandler = new QueuedConsoleLogHandler();

  private volatile IConfiguration configuration;
  private volatile AbstractDatabaseProvider databaseProvider;

  protected CloudNet(@NotNull String[] args, @NotNull IConsole console, @NotNull Logger rootLogger) {
    super(Arrays.asList(args));

    setInstance(this);

    // add the log handler here to capture all log lines of the startup
    this.logHandler.setFormatter(DefaultLogFormatter.END_LINE_SEPARATOR);
    rootLogger.addHandler(this.logHandler);

    this.console = console;
    this.commandProvider = new DefaultCommandProvider(console);

    this.serviceVersionProvider = new ServiceVersionProvider();
    this.cloudNetVersion = CloudNetVersion.fromClassInformation(CloudNet.class.getPackage());

    this.configuration = JsonConfiguration.loadFromFile(this);

    this.nodeServerProvider = new DefaultClusterNodeServerProvider(this);

    this.nodeInfoProvider = new NodeNodeInfoProvider(this);
    this.generalCloudServiceProvider = new DefaultCloudServiceManager(this);

    this.messenger = new NodeMessenger(this);
    this.cloudServiceFactory = new NodeCloudServiceFactory(this);

    this.serviceTaskProvider = new NodeServiceTaskProvider(this);
    this.groupConfigurationProvider = new NodeGroupConfigurationProvider(this);

    // permission management init
    this.setPermissionManagement(new DefaultDatabasePermissionManagement(this));
    this.getPermissionManagement().setPermissionManagementHandler(
      new DefaultPermissionManagementHandler(this.eventManager));

    this.moduleProvider.setModuleDependencyLoader(
      new DefaultPersistableModuleDependencyLoader(LAUNCHER_DIR.resolve("libs")));
    this.moduleProvider.setModuleProviderHandler(new NodeModuleProviderHandler(this));

    this.networkClient = new NettyNetworkClient(
      DefaultNetworkClientChannelHandler::new,
      this.configuration.getClientSslConfig());
    this.networkServer = new NettyNetworkServer(
      DefaultNetworkServerChannelHandler::new,
      this.configuration.getServerSslConfig());
    this.httpServer = new NettyHttpServer(this.configuration.getWebSslConfig());

    // register all rpc handlers associated with methods of this class
    this.rpcProviderFactory.newHandler(Database.class, null).registerToDefaultRegistry();
    this.rpcProviderFactory.newHandler(CloudNetDriver.class, this).registerToDefaultRegistry();
    this.rpcProviderFactory.newHandler(TemplateStorage.class, null).registerToDefaultRegistry();

    this.driverEnvironment = DriverEnvironment.CLOUDNET;
  }

  public static @NotNull CloudNet getInstance() {
    return (CloudNet) CloudNetDriver.getInstance();
  }

  @Override
  public void start() throws Exception {
    HeaderReader.readAndPrintHeader(this.console);
    // load the service versions
    this.serviceVersionProvider.loadServiceVersionTypesOrDefaults(ServiceVersionProvider.DEFAULT_FILE_URL);

    // init the default services
    this.servicesRegistry.registerService(
      TemplateStorage.class,
      "local",
      new LocalTemplateStorage(Paths.get(System.getProperty("cloudnet.storage.local", "local/templates"))));
    // init the default database providers
    this.servicesRegistry.registerService(
      AbstractDatabaseProvider.class,
      "h2",
      new H2DatabaseProvider(
        System.getProperty("cloudnet.database.h2.path", "local/database/h2"),
        !this.configuration.getClusterConfig().getNodes().isEmpty()));
    this.servicesRegistry.registerService(
      AbstractDatabaseProvider.class,
      "xodus",
      new XodusDatabaseProvider(
        new File(System.getProperty("cloudnet.database.xodus.path", "local/database/xodus")),
        !this.configuration.getClusterConfig().getNodes().isEmpty()));

    // initialize the default database provider
    this.setDatabaseProvider(this.servicesRegistry.getService(
      AbstractDatabaseProvider.class,
      this.configuration.getProperties().getString("database_provider", "xodus")));

    // load the modules before proceeding for example to allow the database provider init
    this.moduleProvider.loadAll();

    // check if there is a database provider or initialize the default one
    if (this.databaseProvider == null || !this.databaseProvider.init()) {
      this.setDatabaseProvider(this.servicesRegistry.getService(AbstractDatabaseProvider.class, "xodus"));
      if (this.databaseProvider == null || !this.databaseProvider.init()) {
        // unable to start without a database
        throw new IllegalStateException("No database provider selected for startup - Unable to proceed");
      }
    }

    // init the permission management
    this.permissionManagement.init();

    // execute the installation setup and load the config things after it
    this.installation.executeFirstStartSetup(this.console);

    // init the local node server
    this.nodeServerProvider.setClusterServers(this.configuration.getClusterConfig());
    this.nodeServerProvider.getSelfNode().setNodeInfo(this.configuration.getIdentity());
    this.nodeServerProvider.getSelfNode().publishNodeInfoSnapshotUpdate();

    // network server init
    for (HostAndPort listener : this.configuration.getIdentity().getListeners()) {
      this.networkServer.addListener(listener);
    }
    // http server init
    for (HostAndPort httpListener : this.configuration.getHttpListeners()) {
      this.httpServer.addListener(httpListener);
    }
    // network client init
    Set<CompletableFuture<Void>> futures = new HashSet<>(); // all futures of connections
    for (IClusterNodeServer node : this.nodeServerProvider.getNodeServers()) {
      HostAndPort[] listeners = node.getNodeInfo().getListeners();
      // check if there are any listeners
      if (listeners.length > 0) {
        // get a random listener of the node
        HostAndPort listener = listeners[ThreadLocalRandom.current().nextInt(0, listeners.length)];
        if (this.networkClient.connect(listener)) {
          // register a future that waits for the node to become available
          futures.add(CompletableFuture.runAsync(() -> {
            while (!node.isAvailable()) {
              try {
                //noinspection BusyWait
                Thread.sleep(10);
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
              }
            }
          }));
        }
      }
    }

    // now we can wait for all nodes to become available (if needed)
    if (!futures.isEmpty()) {
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(7, TimeUnit.SECONDS);
      } catch (TimeoutException ignored) {
        // auth failed to a node in the cluster - ignore
      }
    }

    // we are now connected to all nodes - request the full cluster data set if the head node is not the current one
    if (!this.nodeServerProvider.getHeadNode().equals(this.nodeServerProvider.getSelfNode())) {
      ChannelMessage.builder()
        .message("request_initial_cluster_data")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .targetNode(this.nodeServerProvider.getHeadNode().getNodeInfo().getUniqueId())
        .build()
        .send();
    }

    // start modules
    this.moduleProvider.startAll();
    // enable console command handling
    this.commandProvider.registerDefaultCommands();
    this.commandProvider.registerConsoleHandler(this.console);

    // register listeners & post node startup finish
    this.eventManager.registerListener(new FileDeployCallbackListener());
    this.eventManager.callEvent(new CloudNetNodePostInitializationEvent(this));

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));

    // run the main loop
    this.mainThread.start();
  }

  @Override
  public void stop() {
    // check if we are in the shutdown thread - execute in the shutdown thread if not
    if (!Thread.currentThread().getName().equals("Shutdown Thread")) {
      System.exit(0);
      return;
    }
    // check if the node is still running
    if (this.running.getAndSet(false)) {
      try {
        // stop task execution
        this.scheduler.shutdownNow();
        this.serviceVersionProvider.interruptInstallSteps();

        // close all providers
        this.nodeServerProvider.close();
        this.permissionManagement.close();
        this.databaseProvider.close();
        this.moduleProvider.unloadAll();

        // close all services
        this.getCloudServiceProvider().deleteAllCloudServices();

        // close all networking listeners
        this.httpServer.close();
        this.networkClient.close();
        this.networkServer.close();

        // remove temp directory
        FileUtils.delete(FileUtils.TEMP_DIR);

        // close console
        this.console.close();
      } catch (Exception exception) {
        LOGGER.severe("Exception during node shutdown", exception);
      }
    }
  }

  @Override
  public @NotNull String getComponentName() {
    return this.configuration.getIdentity().getUniqueId();
  }

  @Override
  public @NotNull String getNodeUniqueId() {
    return this.configuration.getIdentity().getUniqueId();
  }

  @Override
  public @NotNull TemplateStorage getLocalTemplateStorage() {
    TemplateStorage localStorage = this.getTemplateStorage(ServiceTemplate.LOCAL_STORAGE);
    if (localStorage == null) {
      // this should never happen
      throw new UnsupportedOperationException("Local template storage is not present");
    }

    return localStorage;
  }

  @Override
  public @Nullable TemplateStorage getTemplateStorage(@NotNull String storage) {
    return this.servicesRegistry.getService(TemplateStorage.class, storage);
  }

  @Override
  public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
    return this.servicesRegistry.getServices(TemplateStorage.class);
  }

  @Override
  public @NotNull AbstractDatabaseProvider getDatabaseProvider() {
    return this.databaseProvider;
  }

  public void setDatabaseProvider(@Nullable AbstractDatabaseProvider databaseProvider) {
    if (databaseProvider != null) {
      try {
        // check if we have an old database provider and close that one if the new database provider is ready and connected
        if (this.databaseProvider != null && databaseProvider.init()) {
          this.databaseProvider.close();
        }
        this.databaseProvider = databaseProvider;
        this.rpcProviderFactory.newHandler(DatabaseProvider.class, databaseProvider).registerToDefaultRegistry();
      } catch (Exception exception) {
        LOGGER.severe("Unable to update current database provider", exception);
      }
    }
  }

  @Override
  public @NotNull INetworkClient getNetworkClient() {
    return this.networkClient;
  }

  @Override
  public @NotNull Collection<String> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId,
    @NotNull String commandLine) {
    // get the permission user
    PermissionUser user = this.permissionManagement.getUser(uniqueId);
    if (user == null) {
      return Collections.emptyList();
    } else {
      PermissionUserCommandSource source = new PermissionUserCommandSource(user, this.permissionManagement);
      this.commandProvider.execute(source, commandLine);

      return source.getMessages();
    }
  }

  @Override
  public @NotNull NodeMessenger getMessenger() {
    return (NodeMessenger) super.getMessenger();
  }

  @Override
  public @NotNull ICloudServiceManager getCloudServiceProvider() {
    return (ICloudServiceManager) super.getCloudServiceProvider();
  }

  @Override
  public @NotNull NodePermissionManagement getPermissionManagement() {
    return (NodePermissionManagement) super.getPermissionManagement();
  }

  @Override
  public void setPermissionManagement(@NotNull IPermissionManagement management) {
    // nodes can only use node permission managements
    Preconditions.checkArgument(management instanceof NodePermissionManagement);
    super.setPermissionManagement(management);
    // re-register the handler for the permission management - the call to super.setPermissionManagement will not exit
    // if the permission management is invalid
    this.rpcProviderFactory.newHandler(IPermissionManagement.class, management).registerToDefaultRegistry();
  }

  public @NotNull IConfiguration getConfig() {
    return this.configuration;
  }

  public void setConfig(@NotNull IConfiguration configuration) {
    Preconditions.checkNotNull(configuration);
    this.configuration = configuration;
  }

  public @NotNull IClusterNodeServerProvider getClusterNodeServerProvider() {
    return this.nodeServerProvider;
  }

  public @NotNull CloudNetTick getMainThread() {
    return this.mainThread;
  }

  public @NotNull CommandProvider getCommandProvider() {
    return this.commandProvider;
  }

  public @NotNull IConsole getConsole() {
    return this.console;
  }

  public @NotNull ServiceVersionProvider getServiceVersionProvider() {
    return this.serviceVersionProvider;
  }

  public @NotNull INetworkServer getNetworkServer() {
    return this.networkServer;
  }

  public @NotNull IHttpServer getHttpServer() {
    return this.httpServer;
  }

  public @NotNull QueuedConsoleLogHandler getLogHandler() {
    return this.logHandler;
  }

  public @NotNull DefaultInstallation getInstallation() {
    return this.installation;
  }

  public @NotNull DataSyncRegistry getDataSyncRegistry() {
    return this.dataSyncRegistry;
  }

  public boolean isRunning() {
    return this.running.get();
  }
}
