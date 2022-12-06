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
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Order;
import dev.derklaro.aerogel.internal.binding.ImmediateBindingHolder;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.log.LoggingUtil;
import eu.cloudnetservice.common.log.defaults.AcceptingLogHandler;
import eu.cloudnetservice.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.common.log.io.LogOutputStream;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.DefaultModuleDependencyLoader;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.util.ExecutorServiceUtil;
import eu.cloudnetservice.ext.updater.UpdaterRegistry;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.defaults.DefaultNodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.log.ColoredLogFormatter;
import eu.cloudnetservice.node.console.util.HeaderReader;
import eu.cloudnetservice.node.database.AbstractDatabaseProvider;
import eu.cloudnetservice.node.database.DefaultDatabaseHandler;
import eu.cloudnetservice.node.database.h2.H2DatabaseProvider;
import eu.cloudnetservice.node.database.xodus.XodusDatabaseProvider;
import eu.cloudnetservice.node.event.CloudNetNodePostInitializationEvent;
import eu.cloudnetservice.node.log.QueuedConsoleLogHandler;
import eu.cloudnetservice.node.module.ModulesHolder;
import eu.cloudnetservice.node.module.NodeModuleProviderHandler;
import eu.cloudnetservice.node.module.updater.ModuleUpdaterContext;
import eu.cloudnetservice.node.module.updater.ModuleUpdaterRegistry;
import eu.cloudnetservice.node.network.chunk.FileDeployCallbackListener;
import eu.cloudnetservice.node.permission.NodePermissionManagement;
import eu.cloudnetservice.node.provider.NodeMessenger;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.setup.DefaultInstallation;
import eu.cloudnetservice.node.template.LocalTemplateStorage;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;

/**
 * Represents the implementation of the {@link CloudNetDriver} for nodes.
 */
@Singleton
public class Node extends CloudNetDriver {

  public static final boolean DEV_MODE = Boolean.getBoolean("cloudnet.dev");
  public static final boolean AUTO_UPDATE = Boolean.getBoolean("cloudnet.auto.update");
  private static final Logger LOGGER = LogManager.logger(Node.class);
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

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final DefaultInstallation installation = null; //new DefaultInstallation();
  private final DataSyncRegistry dataSyncRegistry = null; //new DefaultDataSyncRegistry();
  private final QueuedConsoleLogHandler logHandler = null;// new QueuedConsoleLogHandler();

  private volatile AbstractDatabaseProvider databaseProvider;

  @Inject
  protected Node(@NonNull @Named("consoleArgs") List<String> args) {
    super(CloudNetVersion.fromPackage(Node.class.getPackage()), args, DriverEnvironment.NODE);

    instance(this);

    this.console = null; //console;
    this.commandProvider = null; //new DefaultCommandProvider(console, this.eventManager);

    this.modulesHolder = null; //ModuleUpdateUtil.readModuleJson(LAUNCHER_DIR);
    this.moduleUpdaterRegistry = null; //new ModuleUpdaterRegistry();
    //this.moduleUpdaterRegistry.registerUpdater(new ModuleUpdater());

    this.templateStorageProvider = null; //new NodeTemplateStorageProvider(this);
    this.serviceVersionProvider = null; //new ServiceVersionProvider(this.eventManager);

    this.configuration = null; //JsonConfiguration.loadFromFile(this);
    this.nodeServerProvider = null; //new DefaultNodeServerProvider(this);

    // language management init

    this.clusterNodeProvider = null; //new NodeClusterNodeProvider(this);
    this.cloudServiceProvider = null;
    //this.cloudServiceProvider = new DefaultCloudServiceManager(
    //  this,
    // passed down by the launcher, all arguments that we should append by default to service we're
    // starting - these are seperated by ;;
    //   Arrays.asList(this.commandLineArguments.remove(0).split(";;")));

    this.messenger = null; //new NodeMessenger(this);
    this.cloudServiceFactory = null; //new NodeCloudServiceFactory(this);

    this.serviceTaskProvider = null; //new NodeServiceTaskProvider(this);
    this.groupConfigurationProvider = null; //new NodeGroupConfigurationProvider(this);

    // permission management init
    //this.permissionManagement(new DefaultDatabasePermissionManagement(this));
    //this.permissionManagement().permissionManagementHandler(
    //  new DefaultPermissionManagementHandler(this.eventManager));

    this.networkClient = null;
    this.networkServer = null;
    this.httpServer = null;
  }

  @Deprecated(forRemoval = true) // TODO...
  public static @NonNull Node instance() {
    return CloudNetDriver.instance();
  }

  @Override
  public void start(@NonNull Instant startInstant) throws Exception {
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

  @Inject
  @Order(0)
  private void initializeLogging(
    @NonNull Console console,
    @NonNull @Named("root") Logger rootLogger,
    @NonNull QueuedConsoleLogHandler queuedConsoleLogHandler
  ) {
    var consoleFormatter = console.hasColorSupport() ? new ColoredLogFormatter() : DefaultLogFormatter.END_CLEAN;
    var logFilePattern = Path.of(System.getProperty("cloudnet.log.path", "local/logs"), "cloudnet.%g.log");

    // prepare the queued log handler
    var apiFormatter = console.hasColorSupport() ? new ColoredLogFormatter() : DefaultLogFormatter.END_LINE_SEPARATOR;
    queuedConsoleLogHandler.setFormatter(apiFormatter);

    // remove all initial handlers from the root logger
    LoggingUtil.removeHandlers(rootLogger);

    // set the default values for log record dispatches
    rootLogger.setLevel(LoggingUtil.defaultLogLevel());
    rootLogger.logRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(rootLogger));

    // add the default logging handlers
    rootLogger.addHandler(queuedConsoleLogHandler);
    rootLogger.addHandler(AcceptingLogHandler.newInstance(console::writeLine).withFormatter(consoleFormatter));
    rootLogger.addHandler(DefaultFileHandler
      .newInstance(logFilePattern, true)
      .withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));

    // override the system output streams, this isn't strictly required, but some modules might use them which
    // could look out of place in the normal logging context
    System.setErr(LogOutputStream.forSevere(rootLogger).toPrintStream());
    System.setOut(LogOutputStream.forInformative(rootLogger).toPrintStream());
  }

  @Inject
  @Order(0)
  private void initLanguage(@NonNull Configuration configuration) {
    I18n.loadFromLangPath(Node.class);
    I18n.language(configuration.language());
  }

  @Inject
  @Order(50)
  private void greetUser(@NonNull Console console) {
    HeaderReader.readAndPrintHeader(console);
  }

  @Inject
  @Order(100)
  private void registerDefaultRPCHandlers(@NonNull RPCFactory rpcFactory, @NonNull RPCHandlerRegistry handlerRegistry) {
    rpcFactory.newHandler(Database.class, null).registerTo(handlerRegistry);
    rpcFactory.newHandler(CloudNetDriver.class, this).registerTo(handlerRegistry); // TODO: HUH?
    rpcFactory.newHandler(TemplateStorage.class, null).registerTo(handlerRegistry);
  }

  @Inject
  @Order(150)
  private void loadServiceVersions(@NonNull ServiceVersionProvider serviceVersionProvider) {
    // load the service versions
    serviceVersionProvider.loadDefaultVersionTypes();
    LOGGER.info(I18n.trans("start-version-provider", serviceVersionProvider.serviceVersionTypes().size()));
  }

  @Inject
  @Order(200)
  private void setupModuleProvider(
    @NonNull ModuleProvider moduleProvider,
    @NonNull NodeModuleProviderHandler providerHandler,
    @NonNull @Named("launcherDir") Path launcherDirectory
  ) {
    moduleProvider.moduleProviderHandler(providerHandler);
    moduleProvider.moduleDependencyLoader(new DefaultModuleDependencyLoader(launcherDirectory.resolve("libs")));
  }

  @Inject
  @Order(250)
  private void registerDefaultServices(
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull Configuration configuration,
    @NonNull DefaultDatabaseHandler databaseHandler) {
    // local template storage
    var localStoragePath = Path.of(System.getProperty("cloudnet.storage.local", "local/templates"));
    serviceRegistry.registerProvider(TemplateStorage.class, "local", new LocalTemplateStorage(localStoragePath));

    // xodus database
    var runsInCluster = !configuration.clusterConfig().nodes().isEmpty();
    var dbDirectory = new File(System.getProperty("cloudnet.database.xodus.path", "local/database/xodus"));
    serviceRegistry.registerProvider(
      AbstractDatabaseProvider.class,
      "xodus",
      new XodusDatabaseProvider(dbDirectory, runsInCluster, databaseHandler));
  }

  @Inject
  @Order(300)
  private void convertDatabase(
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull DefaultDatabaseHandler databaseHandler
  ) throws Exception { // TODO: remove in 4.1
    var configuredDatabase = configuration.properties().getString("database_provider", "xodus");
    // check if we need to migrate the old h2 database into a new xodus database
    if (configuredDatabase.equals("h2")) {
      // initialize the provider for the local h2 files
      var h2Provider = new H2DatabaseProvider(
        System.getProperty("cloudnet.database.h2.path", "local/database/h2"),
        databaseHandler);
      h2Provider.init();
      // initialize the provider for our new xodus database
      var xodusProvider = serviceRegistry.provider(AbstractDatabaseProvider.class, "xodus");
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

      // close the database provider as they are not needed anymore
      h2Provider.close();
      xodusProvider.close();

      // save the updated configuration
      configuration.properties().append("database_provider", "xodus");
      configuration.save();
    }
  }

  @Inject
  @Order(350)
  private void updateAndLoadModules(
    @NonNull ModulesHolder modulesHolder,
    @NonNull ModuleProvider moduleProvider,
    @NonNull ModuleUpdaterRegistry updaterRegistry
  ) throws Exception {
    // apply all module updates if we're not running in dev mode
    if (!DEV_MODE) {
      LOGGER.info(I18n.trans("start-module-updater"));
      updaterRegistry.runUpdater(modulesHolder, !AUTO_UPDATE);
    }

    // load the modules before proceeding for example to allow the database provider init
    moduleProvider.loadAll();
  }

  @Inject
  @Order(400)
  private void initializeDatabaseProvider(
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry serviceRegistry
  ) throws Exception {
    // initialize the default database provider
    var configuredProvider = configuration.properties().getString("database_provider", "xodus");
    var provider = serviceRegistry.provider(AbstractDatabaseProvider.class, configuredProvider);

    // check if the provider is present & can be initialized
    if (provider == null || !provider.init()) {
      provider = serviceRegistry.provider(AbstractDatabaseProvider.class, "xodus");
      if (provider == null || !provider.init()) {
        // unable to start without a database
        throw new IllegalStateException("No database provider selected for startup - Unable to proceed");
      }
    }

    // bind the provider for dependency injection
    var selectedProvider = provider; // okay fine
    var providerElement = Element.forType(DatabaseProvider.class);
    InjectionLayer.boot().install(injector -> new ImmediateBindingHolder(
      providerElement,
      injector,
      selectedProvider,
      Element.forType(AbstractDatabaseProvider.class), providerElement));

    // notify the user about the selected database
    LOGGER.info(I18n.trans("start-connect-database", provider.name()));
  }

  @Inject
  @Order(450)
  private void executeSetupIfRequired(
    @NonNull DefaultInstallation installation,
    @NonNull NodePermissionManagement permissionManagement
  ) {
    // init the permission management before the setup
    permissionManagement.init();
    installation.executeFirstStartSetup();
  }

  @Inject
  @Order(500)
  private void registerConfiguredNodeServers(
    @NonNull Configuration configuration,
    @NonNull NodeServerProvider nodeProvider
  ) {
    nodeProvider.registerNodes(configuration.clusterConfig());
    nodeProvider.localNode().updateLocalSnapshot();
    nodeProvider.localNode().state(NodeServerState.READY);
    nodeProvider.selectHeadNode();
  }

  @Inject
  @Order(550)
  private void bindNetworkListeners(
    @NonNull HttpServer httpServer,
    @NonNull Configuration configuration,
    @NonNull NetworkServer networkServer
  ) throws InterruptedException {
    // print out some network information, more for debug reasons in normal cases
    LOGGER.info(I18n.trans("network-selected-transport", NettyUtil.selectedNettyTransport().displayName()));
    LOGGER.info(I18n.trans(
      "network-selected-dispatch-thread-type",
      ExecutorServiceUtil.virtualThreadsAvailable() ? "virtual" : "platform"));

    // network server init
    var connectionCounter = new AtomicInteger();
    for (var listener : configuration.identity().listeners()) {
      networkServer.addListener(listener).handle(($, exception) -> {
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
    for (var listener : configuration.httpListeners()) {
      httpServer.addListener(listener).handle(($, exception) -> {
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

  @Inject
  @Order(600)
  private void establishNodeConnections(@NonNull NodeServerProvider nodeServerProvider) {
    // network client init
    var nodeConnections = new Phaser(1);
    Set<CompletableFuture<Void>> futures = new HashSet<>(); // all futures of connections
    for (var node : nodeServerProvider.nodeServers()) {
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
            for (var i = 0; i < 140 && !node.available(); i++) {
              Thread.onSpinWait();
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

  @Inject
  @Order(650)
  private void requestClusterDataIfNeeded(@NonNull NodeServerProvider nodeServerProvider) {
    // we are now connected to all nodes - request the full cluster data set if the head node is not the current one
    if (!nodeServerProvider.localNode().head()) {
      LOGGER.info(I18n.trans("start-requesting-data"));
      ChannelMessage.builder()
        .message("request_initial_cluster_data")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .targetNode(nodeServerProvider.headNode().info().uniqueId())
        .build()
        .send();
    }
  }

  @Inject
  @Order(700)
  private void registerDefaultCommands(@NonNull CommandProvider commandProvider, @NonNull Console console) {
    // register the default commands
    LOGGER.info(I18n.trans("start-commands"));
    commandProvider.registerDefaultCommands();
    commandProvider.registerConsoleHandler(console);
  }

  @Inject
  @Order(Integer.MAX_VALUE)
  private void finishStartup(
    @NonNull TickLoop tickLoop,
    @NonNull EventManager eventManager,
    @NonNull ModuleProvider moduleProvider,
    @NonNull FileDeployCallbackListener callbackListener,
    @NonNull @Named("startInstant") Instant startInstant
  ) {
    // start all modules
    moduleProvider.startAll();

    // register listeners & post node startup finish
    eventManager.registerListener(callbackListener);
    eventManager.callEvent(new CloudNetNodePostInitializationEvent());

    // add shutdown hook & notify
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));
    LOGGER.info(I18n.trans("start-done", Duration.between(startInstant, Instant.now()).toMillis()));

    // start the main tick loop
    tickLoop.start();
  }

  @Override
  public @NonNull String componentName() {
    return "";//this.configuration.identity().uniqueId();
  }

  @Override
  public @NonNull String nodeUniqueId() {
    return ""; //this.configuration.identity().uniqueId();
  }

  @Override
  public @NonNull AbstractDatabaseProvider databaseProvider() {
    return this.databaseProvider;
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
  public void permissionManagement(@NonNull PermissionManagement management) { // TODO: remove
    // nodes can only use node permission managements
    Preconditions.checkArgument(management instanceof NodePermissionManagement);
    super.permissionManagement(management);
    // re-register the handler for the permission management - the call to super.setPermissionManagement will not exit
    // if the permission management is invalid
    //this.rpcFactory.newHandler(PermissionManagement.class, management).registerToDefaultRegistry();
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
    throw new UnsupportedOperationException();
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


}
