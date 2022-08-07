/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.module.DefaultModuleDependencyLoader;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import eu.cloudnetservice.driver.network.netty.server.NettyNetworkServer;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.util.ExecutorServiceUtil;
import eu.cloudnetservice.ext.updater.UpdaterRegistry;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.defaults.DefaultNodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.cluster.sync.DefaultDataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.defaults.DefaultCommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.JsonConfiguration;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.log.ColouredLogFormatter;
import eu.cloudnetservice.node.console.util.HeaderReader;
import eu.cloudnetservice.node.database.AbstractDatabaseProvider;
import eu.cloudnetservice.node.database.h2.H2DatabaseProvider;
import eu.cloudnetservice.node.database.xodus.XodusDatabaseProvider;
import eu.cloudnetservice.node.event.CloudNetNodePostInitializationEvent;
import eu.cloudnetservice.node.log.QueuedConsoleLogHandler;
import eu.cloudnetservice.node.module.ModulesHolder;
import eu.cloudnetservice.node.module.NodeModuleProviderHandler;
import eu.cloudnetservice.node.module.updater.ModuleUpdater;
import eu.cloudnetservice.node.module.updater.ModuleUpdaterContext;
import eu.cloudnetservice.node.module.updater.ModuleUpdaterRegistry;
import eu.cloudnetservice.node.module.util.ModuleJsonReader;
import eu.cloudnetservice.node.network.DefaultNetworkClientChannelHandler;
import eu.cloudnetservice.node.network.DefaultNetworkServerChannelHandler;
import eu.cloudnetservice.node.network.chunk.FileDeployCallbackListener;
import eu.cloudnetservice.node.permission.DefaultDatabasePermissionManagement;
import eu.cloudnetservice.node.permission.DefaultPermissionManagementHandler;
import eu.cloudnetservice.node.permission.NodePermissionManagement;
import eu.cloudnetservice.node.provider.NodeClusterNodeProvider;
import eu.cloudnetservice.node.provider.NodeGroupConfigurationProvider;
import eu.cloudnetservice.node.provider.NodeMessenger;
import eu.cloudnetservice.node.provider.NodeServiceTaskProvider;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.defaults.DefaultCloudServiceManager;
import eu.cloudnetservice.node.service.defaults.NodeCloudServiceFactory;
import eu.cloudnetservice.node.setup.DefaultInstallation;
import eu.cloudnetservice.node.template.LocalTemplateStorage;
import eu.cloudnetservice.node.template.NodeTemplateStorageProvider;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the implementation of the {@link CloudNetDriver} for nodes.
 */
public class Node extends CloudNetDriver {

  private static final Logger LOGGER = LogManager.logger(Node.class);
  private static final boolean DEV_MODE = Boolean.getBoolean("cloudnet.dev");
  private static final boolean AUTO_UPDATE = Boolean.getBoolean("cloudnet.auto.update");
  private static final Path LAUNCHER_DIR = Path.of(System.getProperty("cloudnet.launcherdir", "launcher"));

  private final Console console;
  private final CommandProvider commandProvider;

  private final HttpServer httpServer;
  private final NetworkClient networkClient;
  private final NetworkServer networkServer;

  private final DefaultNodeServerProvider nodeServerProvider;
  private final ServiceVersionProvider serviceVersionProvider;

  private final Configuration configuration;
  private final ModulesHolder modulesHolder;
  private final UpdaterRegistry<ModuleUpdaterContext, ModulesHolder> moduleUpdaterRegistry;

  private final TickLoop mainThread = new TickLoop(this);
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final DefaultInstallation installation = new DefaultInstallation();
  private final DataSyncRegistry dataSyncRegistry = new DefaultDataSyncRegistry();
  private final QueuedConsoleLogHandler logHandler = new QueuedConsoleLogHandler();

  private volatile AbstractDatabaseProvider databaseProvider;

  protected Node(@NonNull String[] args, @NonNull Console console, @NonNull Logger rootLogger) {
    super(CloudNetVersion.fromPackage(Node.class.getPackage()), Lists.newArrayList(args), DriverEnvironment.NODE);

    instance(this);

    // add the log handler here to capture all log lines of the startup
    this.logHandler.setFormatter(console.hasColorSupport()
      ? new ColouredLogFormatter()
      : DefaultLogFormatter.END_LINE_SEPARATOR);
    rootLogger.addHandler(this.logHandler);

    this.console = console;
    this.commandProvider = new DefaultCommandProvider(console, this.eventManager);

    this.modulesHolder = ModuleJsonReader.read(LAUNCHER_DIR);
    this.moduleUpdaterRegistry = new ModuleUpdaterRegistry();
    this.moduleUpdaterRegistry.registerUpdater(new ModuleUpdater());

    this.templateStorageProvider = new NodeTemplateStorageProvider(this);
    this.serviceVersionProvider = new ServiceVersionProvider(this.eventManager);

    this.configuration = JsonConfiguration.loadFromFile(this);
    this.nodeServerProvider = new DefaultNodeServerProvider(this);

    // language management init
    I18n.loadFromLangPath(Node.class);
    I18n.language(this.configuration.language());

    this.clusterNodeProvider = new NodeClusterNodeProvider(this);
    this.cloudServiceProvider = new DefaultCloudServiceManager(
      this,
      // passed down by the launcher, all arguments that we should append by default to service we're
      // starting - these are seperated by ;;
      Arrays.asList(this.commandLineArguments.remove(0).split(";;")));

    this.messenger = new NodeMessenger(this);
    this.cloudServiceFactory = new NodeCloudServiceFactory(this);

    this.serviceTaskProvider = new NodeServiceTaskProvider(this);
    this.groupConfigurationProvider = new NodeGroupConfigurationProvider(this);

    // permission management init
    this.permissionManagement(new DefaultDatabasePermissionManagement(this));
    this.permissionManagement().permissionManagementHandler(
      new DefaultPermissionManagementHandler(this.eventManager));

    this.moduleProvider.moduleProviderHandler(new NodeModuleProviderHandler(this));
    this.moduleProvider.moduleDependencyLoader(new DefaultModuleDependencyLoader(LAUNCHER_DIR.resolve("libs")));

    this.networkClient = new NettyNetworkClient(
      DefaultNetworkClientChannelHandler::new,
      this.configuration.clientSSLConfig());
    this.networkServer = new NettyNetworkServer(
      DefaultNetworkServerChannelHandler::new,
      this.configuration.serverSSLConfig());
    this.httpServer = new NettyHttpServer(this.configuration.webSSLConfig());

    // register all rpc handlers associated with methods of this class
    this.rpcFactory.newHandler(Database.class, null).registerToDefaultRegistry();
    this.rpcFactory.newHandler(CloudNetDriver.class, this).registerToDefaultRegistry();
    this.rpcFactory.newHandler(TemplateStorage.class, null).registerToDefaultRegistry();
  }

  public static @NonNull Node instance() {
    return CloudNetDriver.instance();
  }

  @Override
  protected void start(@NonNull Instant startInstant) throws Exception {
    HeaderReader.readAndPrintHeader(this.console);
    // load the service versions
    this.serviceVersionProvider.loadDefaultVersionTypes();
    LOGGER.info(I18n.trans("start-version-provider", this.serviceVersionProvider.serviceVersionTypes().size()));
    // init the default services
    this.serviceRegistry.registerProvider(
      TemplateStorage.class,
      "local",
      new LocalTemplateStorage(Path.of(System.getProperty("cloudnet.storage.local", "local/templates"))));
    // init the default database providers
    this.serviceRegistry.registerProvider(
      AbstractDatabaseProvider.class,
      "xodus",
      new XodusDatabaseProvider(
        new File(System.getProperty("cloudnet.database.xodus.path", "local/database/xodus")),
        !this.configuration.clusterConfig().nodes().isEmpty()));

    // convert from h2 to xodus if needed
    this.convertDatabase();

    // apply all module updates if we're not running in dev mode
    if (!DEV_MODE) {
      LOGGER.info(I18n.trans("start-module-updater"));
      this.moduleUpdaterRegistry.runUpdater(this.modulesHolder, !this.autoUpdate());
    }

    // load the modules before proceeding for example to allow the database provider init
    this.moduleProvider.loadAll();

    // initialize the default database provider
    this.databaseProvider(this.serviceRegistry.provider(
      AbstractDatabaseProvider.class,
      this.configuration.properties().getString("database_provider", "xodus")));

    // check if there is a database provider or initialize the default one
    if (this.databaseProvider == null || !this.databaseProvider.init()) {
      this.databaseProvider(this.serviceRegistry.provider(AbstractDatabaseProvider.class, "xodus"));
      if (this.databaseProvider == null || !this.databaseProvider.init()) {
        // unable to start without a database
        throw new IllegalStateException("No database provider selected for startup - Unable to proceed");
      }
    }
    // notify the user about the selected database
    LOGGER.info(I18n.trans("start-connect-database", this.databaseProvider.name()));

    // init the permission management
    this.permissionManagement.init();

    // execute the installation setup and load the config things after it
    this.installation.executeFirstStartSetup(this.console);

    // initialize the node server provider
    this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());
    this.nodeServerProvider.localNode().updateLocalSnapshot();
    this.nodeServerProvider.localNode().state(NodeServerState.READY);
    this.nodeServerProvider.selectHeadNode();

    // print out some network information, more for debug reasons in normal cases
    LOGGER.info(I18n.trans("network-selected-transport", NettyUtil.selectedNettyTransport().displayName()));
    LOGGER.info(I18n.trans(
      "network-selected-dispatch-thread-type",
      ExecutorServiceUtil.virtualThreadsAvailable() ? "virtual" : "platform"));

    // bind network listeners
    this.bindNetworkListeners();

    // connect to the other node servers
    this.establishNodeConnections();

    // we are now connected to all nodes - request the full cluster data set if the head node is not the current one
    if (!this.nodeServerProvider.localNode().head()) {
      LOGGER.info(I18n.trans("start-requesting-data"));
      ChannelMessage.builder()
        .message("request_initial_cluster_data")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .targetNode(this.nodeServerProvider.headNode().info().uniqueId())
        .build()
        .send();
    }

    // enable console command handling
    LOGGER.info(I18n.trans("start-commands"));
    this.commandProvider.registerDefaultCommands();
    this.commandProvider.registerConsoleHandler(this.console);
    // start modules
    this.moduleProvider.startAll();
    // register listeners & post node startup finish
    this.eventManager.registerListener(new FileDeployCallbackListener());
    this.eventManager.callEvent(new CloudNetNodePostInitializationEvent(this));

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));
    LOGGER.info(I18n.trans("start-done", Duration.between(startInstant, Instant.now()).toMillis()));

    // run the main loop
    this.mainThread.start();
  }

  @Override
  public void stop() {
    // check if the node is still running
    if (this.running.getAndSet(false)) {
      try {
        LOGGER.info(I18n.trans("stop-application"));

        // stop task execution
        this.scheduler.shutdownNow();
        this.serviceVersionProvider.interruptInstallSteps();

        // interrupt the connection to other nodes
        LOGGER.info(I18n.trans("stop-node-connections"));
        this.nodeServerProvider.close();

        // close all services
        LOGGER.info(I18n.trans("stop-services"));
        this.cloudServiceProvider().deleteAllCloudServices();

        // close all networking listeners
        LOGGER.info(I18n.trans("stop-network-components"));
        this.httpServer.close();
        this.networkClient.close();
        this.networkServer.close();

        // close all the other providers
        LOGGER.info(I18n.trans("stop-providers"));
        this.permissionManagement.close();
        this.databaseProvider.close();

        // stop & unload all modules
        this.moduleProvider.stopAll();
        this.moduleProvider.unloadAll();

        // remove temp directory
        LOGGER.info(I18n.trans("stop-delete-temp"));
        FileUtil.delete(FileUtil.TEMP_DIR);

        // close console
        this.console.close();

        // check if we are in the shutdown thread - execute a clean shutdown if not
        if (!Thread.currentThread().getName().equals("Shutdown Thread")) {
          System.exit(0);
        }
      } catch (Exception exception) {
        LOGGER.severe("Exception during node shutdown", exception);
      }
    }
  }

  @Override
  public @NonNull String componentName() {
    return this.configuration.identity().uniqueId();
  }

  @Override
  public @NonNull String nodeUniqueId() {
    return this.configuration.identity().uniqueId();
  }

  @Override
  public @NonNull AbstractDatabaseProvider databaseProvider() {
    return this.databaseProvider;
  }

  public void databaseProvider(@Nullable AbstractDatabaseProvider databaseProvider) {
    if (databaseProvider != null) {
      try {
        // check if we have an old database provider and close that one if the new database provider is ready and connected
        if (this.databaseProvider != null && databaseProvider.init()) {
          this.databaseProvider.close();
        }
        this.databaseProvider = databaseProvider;
        this.rpcFactory.newHandler(DatabaseProvider.class, databaseProvider).registerToDefaultRegistry();
      } catch (Exception exception) {
        LOGGER.severe("Unable to update current database provider", exception);
      }
    }
  }

  @Override
  public @NonNull NetworkClient networkClient() {
    return this.networkClient;
  }

  @Override
  public @NonNull NodeMessenger messenger() {
    return (NodeMessenger) super.messenger();
  }

  @Override
  public @NonNull CloudServiceManager cloudServiceProvider() {
    return (CloudServiceManager) super.cloudServiceProvider();
  }

  @Override
  public @NonNull NodePermissionManagement permissionManagement() {
    return (NodePermissionManagement) super.permissionManagement();
  }

  @Override
  public void permissionManagement(@NonNull PermissionManagement management) {
    // nodes can only use node permission managements
    Preconditions.checkArgument(management instanceof NodePermissionManagement);
    super.permissionManagement(management);
    // re-register the handler for the permission management - the call to super.setPermissionManagement will not exit
    // if the permission management is invalid
    this.rpcFactory.newHandler(PermissionManagement.class, management).registerToDefaultRegistry();
  }

  public @NonNull Configuration config() {
    return this.configuration;
  }

  public void reloadConfigFrom(@NonNull Configuration configuration) {
    this.configuration.reloadFrom(configuration.save());
  }

  public @NonNull NodeServerProvider nodeServerProvider() {
    return this.nodeServerProvider;
  }

  public @NonNull TickLoop mainThread() {
    return this.mainThread;
  }

  public @NonNull CommandProvider commandProvider() {
    return this.commandProvider;
  }

  public @NonNull Console console() {
    return this.console;
  }

  public @NonNull ServiceVersionProvider serviceVersionProvider() {
    return this.serviceVersionProvider;
  }

  public @NonNull NetworkServer networkServer() {
    return this.networkServer;
  }

  public @NonNull HttpServer httpServer() {
    return this.httpServer;
  }

  public @NonNull QueuedConsoleLogHandler logHandler() {
    return this.logHandler;
  }

  public @NonNull DefaultInstallation installation() {
    return this.installation;
  }

  public @NonNull DataSyncRegistry dataSyncRegistry() {
    return this.dataSyncRegistry;
  }

  public @NonNull ModulesHolder modulesHolder() {
    return this.modulesHolder;
  }

  public boolean dev() {
    return DEV_MODE;
  }

  public boolean autoUpdate() {
    return AUTO_UPDATE;
  }

  public boolean running() {
    return this.running.get();
  }

  private void bindNetworkListeners() throws InterruptedException {
    var connectionCounter = new AtomicInteger();

    // network server init
    for (var listener : this.configuration.identity().listeners()) {
      this.networkServer.addListener(listener).handle(($, exception) -> {
        // check if the bind failed
        if (exception != null) {
          LOGGER.info(I18n.trans("network-listener-bound-exceptionally", listener, exception.getMessage()));
        } else {
          connectionCounter.incrementAndGet();
          LOGGER.info(I18n.trans("network-listener-bound", listener));
        }

        // prevent the exception from being thrown
        return null;
      }).join();
    }

    // we can hard stop here if no network listener was bound - the wrappers will not be able to connect to the node
    if (connectionCounter.get() == 0) {
      LOGGER.severe(I18n.trans("startup-failed-no-network-listener-bound"));
      // wait a bit, then stop
      Thread.sleep(5000);
      System.exit(1);
    }

    // http server init
    for (var listener : this.configuration.httpListeners()) {
      this.httpServer.addListener(listener).handle(($, exception) -> {
        // check if the bind failed
        if (exception != null) {
          LOGGER.info(I18n.trans("http-listener-bound-exceptionally", listener, exception.getMessage()));
        } else {
          LOGGER.info(I18n.trans("http-listener-bound", listener));
        }

        // prevent the exception from being thrown
        return null;
      }).join();
    }
  }

  private void establishNodeConnections() {
    // network client init
    var nodeConnections = new Phaser(1);
    Set<CompletableFuture<Void>> futures = new HashSet<>(); // all futures of connections
    for (var node : this.nodeServerProvider.nodeServers()) {
      // skip all node servers which are already available (normally only the local node)
      if (node.available()) {
        continue;
      }

      // register the connection attempt
      nodeConnections.register();

      // try to connect to the node
      LOGGER.info(I18n.trans("start-node-connection-try", node.info().uniqueId()));
      node.connect().whenComplete(($, exception) -> {
        if (exception != null) {
          // the connection couldn't be established
          LOGGER.warning(I18n.trans("start-node-connection-failure", node.info().uniqueId(), exception.getMessage()));
        } else {
          // wait for the node connection to become available
          futures.add(Task.supply(() -> {
            // wait for the connection to establish, max 7 seconds
            for (int i = 0; i < 140 && !node.available(); i++) {
              //noinspection BusyWait
              Thread.sleep(50);
            }
            return null;
          }));
        }

        // count down by one arrival
        nodeConnections.arriveAndDeregister();
      });
    }

    // wait for all connections to establish (or fail during connect)
    nodeConnections.arriveAndAwaitAdvance();

    // now we can wait for all nodes to become available (if needed)
    if (!futures.isEmpty()) {
      try {
        LOGGER.info(I18n.trans("start-node-connection-waiting", futures.size()));
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(7, TimeUnit.SECONDS);
      } catch (Exception ignored) {
        // auth failed to a node in the cluster - ignore
      }
    }
  }

  // TODO: remove in 4.1
  private void convertDatabase() throws Exception {
    var configuredDatabase = this.configuration.properties().getString("database_provider", "xodus");
    // check if we need to migrate the old h2 database into a new xodus database
    if (configuredDatabase.equals("h2")) {
      // initialize the provider for the local h2 files
      var h2Provider = new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"));
      h2Provider.init();
      // initialize the provider for our new xodus database
      var xodusProvider = this.serviceRegistry.provider(AbstractDatabaseProvider.class, "xodus");
      xodusProvider.init();
      // run the migration on all tables in the h2 database
      for (var databaseName : h2Provider.databaseNames()) {
        var h2Database = h2Provider.database(databaseName);
        // create the new xodus storage
        var xodusDatabase = xodusProvider.database(databaseName);
        // insert the data of the h2 database into the xodus database
        // in chunks of 100 documents to prevent oom
        h2Database.iterate(xodusDatabase::insert, 100);
      }
      // we've run the conversion, the new provider is xodus
      configuredDatabase = "xodus";
      // close the old h2 provider as it is not needed anymore
      h2Provider.close();
      // close xodus too as it is initialized later on
      xodusProvider.close();
      // save the updated configuration
      this.configuration.properties().append("database_provider", configuredDatabase);
      this.configuration.save();
    }
  }
}
