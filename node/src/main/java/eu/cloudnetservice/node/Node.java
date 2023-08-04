/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import dev.derklaro.aerogel.Order;
import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.log.LoggingUtil;
import eu.cloudnetservice.common.log.defaults.AcceptingLogHandler;
import eu.cloudnetservice.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.common.log.io.LogOutputStream;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.DefaultModuleDependencyLoader;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.util.ExecutorServiceUtil;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.task.LocalNodeUpdateTask;
import eu.cloudnetservice.node.cluster.task.NodeDisconnectTrackerTask;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.log.ColoredLogFormatter;
import eu.cloudnetservice.node.console.util.HeaderReader;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.database.h2.H2DatabaseProvider;
import eu.cloudnetservice.node.database.xodus.XodusDatabaseProvider;
import eu.cloudnetservice.node.event.CloudNetNodePostInitializationEvent;
import eu.cloudnetservice.node.log.QueuedConsoleLogHandler;
import eu.cloudnetservice.node.module.ModulesHolder;
import eu.cloudnetservice.node.module.NodeModuleProviderHandler;
import eu.cloudnetservice.node.module.updater.ModuleUpdater;
import eu.cloudnetservice.node.module.updater.ModuleUpdaterRegistry;
import eu.cloudnetservice.node.network.chunk.FileDeployCallbackListener;
import eu.cloudnetservice.node.setup.DefaultInstallation;
import eu.cloudnetservice.node.template.LocalTemplateStorage;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import lombok.NonNull;

@Singleton
public final class Node {

  public static final boolean DEV_MODE = Boolean.getBoolean("cloudnet.dev");
  public static final boolean AUTO_UPDATE = Boolean.getBoolean("cloudnet.auto.update");
  private static final Logger LOGGER = LogManager.logger(Node.class);

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
  private void registerDefaultServices(@NonNull ServiceRegistry serviceRegistry, @NonNull Configuration configuration) {
    // local template storage
    var localStoragePath = Path.of(System.getProperty("cloudnet.storage.local", "local/templates"));
    serviceRegistry.registerProvider(TemplateStorage.class, "local", new LocalTemplateStorage(localStoragePath));

    // xodus database
    var runsInCluster = !configuration.clusterConfig().nodes().isEmpty();
    var dbDirectory = new File(System.getProperty("cloudnet.database.xodus.path", "local/database/xodus"));
    serviceRegistry.registerProvider(
      NodeDatabaseProvider.class,
      "xodus",
      new XodusDatabaseProvider(dbDirectory, runsInCluster));
  }

  @Inject
  @Order(300)
  private void convertDatabase(
    @NonNull Configuration configuration,
    @NonNull @Service(name = "xodus") NodeDatabaseProvider xodusProvider
  ) throws Exception { // TODO: remove in 4.1
    var configuredDatabase = configuration.properties().getString("database_provider", "xodus");
    // check if we need to migrate the old h2 database into a new xodus database
    if (configuredDatabase.equals("h2")) {
      // initialize the provider for the local h2 files
      var h2Provider = new H2DatabaseProvider(System.getProperty("cloudnet.database.h2.path", "local/database/h2"));
      h2Provider.init();
      // initialize the provider for our new xodus database
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
      configuration.properties(configuration.properties().mutableCopy().append("database_provider", "xodus"));
      configuration.save();
    }
  }

  @Inject
  @Order(350)
  private void updateAndLoadModules(
    @NonNull ModulesHolder modulesHolder,
    @NonNull ModuleProvider moduleProvider,
    @NonNull ModuleUpdater moduleUpdater,
    @NonNull ModuleUpdaterRegistry updaterRegistry
  ) throws Exception {
    // apply all module updates if we're not running in dev mode
    if (!DEV_MODE) {
      LOGGER.info(I18n.trans("start-module-updater"));
      updaterRegistry.registerUpdater(moduleUpdater);
      updaterRegistry.runUpdater(modulesHolder, !AUTO_UPDATE);
    }

    // load the modules before proceeding for example to allow the database provider init
    moduleProvider.loadAll();
  }

  @Inject
  @Order(400)
  private void initializeDatabaseProvider(
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull InjectionLayer<?> bootLayer,
    @NonNull RPCFactory rpcFactory,
    @NonNull RPCHandlerRegistry rpcHandlerRegistry
  ) throws Exception {
    // initialize the default database provider
    var configuredProvider = configuration.properties().getString("database_provider", "xodus");
    var provider = serviceRegistry.provider(NodeDatabaseProvider.class, configuredProvider);

    // check if the provider is present & can be initialized
    if (provider == null || !provider.init()) {
      provider = serviceRegistry.provider(NodeDatabaseProvider.class, "xodus");
      if (provider == null || !provider.init()) {
        // unable to start without a database
        throw new IllegalStateException("No database provider selected for startup - Unable to proceed");
      }
    }

    // bind the provider for dependency injection
    var binding = BindingBuilder.create()
      .bindAll(DatabaseProvider.class, NodeDatabaseProvider.class)
      .toInstance(provider);
    bootLayer.install(binding);

    // register the rpc handler for the database provider
    rpcFactory.newHandler(DatabaseProvider.class, provider).registerTo(rpcHandlerRegistry);

    // notify the user about the selected database
    LOGGER.info(I18n.trans("start-connect-database", provider.name()));
  }

  @Inject
  @Order(450)
  private void executeSetupIfRequired(
    @NonNull DefaultInstallation installation
  ) {
    // execute the setup if needed
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
    Collection<BooleanSupplier> waitingNodeAvailableSuppliers = new LinkedList<>();
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
          waitingNodeAvailableSuppliers.add(node::available);
        }

        // count down by one arrival
        nodeConnections.arriveAndDeregister();
      });
    }

    // wait for all connections to establish (or fail during connect)
    nodeConnections.arriveAndAwaitAdvance();

    // now we can wait for all nodes to become available (if needed)
    if (!waitingNodeAvailableSuppliers.isEmpty()) {
      // notify the user that we're waiting
      LOGGER.info(I18n.trans("start-node-connection-waiting", waitingNodeAvailableSuppliers.size()));

      var waitStartInstant = Instant.now();
      while (!waitingNodeAvailableSuppliers.isEmpty()) {
        // remove all boolean suppliers that were notified that the node is available
        waitingNodeAvailableSuppliers.removeIf(BooleanSupplier::getAsBoolean);

        // time-out this loop if we waited for more than 7 seconds
        var waitDuration = Duration.between(waitStartInstant, Instant.now());
        if (waitDuration.getSeconds() >= 7) {
          break;
        }

        try {
          // wait for a tiny bit before checking again
          //noinspection BusyWait
          Thread.sleep(50L);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt(); // reset the interrupted state of the thread
          throw new IllegalThreadStateException();
        }
      }
    }
  }

  @Inject
  @Order(650)
  private void registerDefaultCommands(@NonNull CommandProvider commandProvider, @NonNull Console console) {
    // register the default commands
    LOGGER.info(I18n.trans("start-commands"));
    commandProvider.registerDefaultCommands();
    commandProvider.registerConsoleHandler(console);
  }

  @Inject
  @Order(700)
  private void startModules(@NonNull ModuleProvider moduleProvider) {
    moduleProvider.startAll();
  }

  @Inject
  @Order(750)
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
  @Order(800)
  private void scheduleNodeUpdateTasks(
    @NonNull LocalNodeUpdateTask localNodeUpdateTask,
    @NonNull NodeDisconnectTrackerTask disconnectTrackerTask
  ) {
    // create a scheduled executor that we use to schedule the task, ensure that we shut it down when
    // the process terminates to ensure that no threads are preventing the shutdown process to complete
    var updateTaskExecutor = Executors.newSingleThreadScheduledExecutor();
    Runtime.getRuntime().addShutdownHook(new Thread(updateTaskExecutor::shutdownNow));

    // schedule both update tasks
    updateTaskExecutor.scheduleAtFixedRate(localNodeUpdateTask, 1, 1, TimeUnit.SECONDS);
    updateTaskExecutor.scheduleAtFixedRate(disconnectTrackerTask, 5, 5, TimeUnit.SECONDS);
  }

  @Inject
  @Order(10_000)
  private void installShutdownHook(@NonNull Provider<ShutdownHandler> shutdownHandlerProvider) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // get the shutdown handler instance & execute the shutdown process
      var shutdownHandler = shutdownHandlerProvider.get();
      shutdownHandler.shutdown();
    }, ShutdownHandler.SHUTDOWN_THREAD_NAME));
  }

  @Inject
  @Order(Integer.MAX_VALUE)
  private void finishStartup(
    @NonNull TickLoop tickLoop,
    @NonNull EventManager eventManager,
    @NonNull FileDeployCallbackListener callbackListener,
    @NonNull @Named("startInstant") Instant startInstant
  ) {
    // register listeners & post node startup finish
    eventManager.registerListener(callbackListener);
    eventManager.callEvent(new CloudNetNodePostInitializationEvent());

    // notify that we are done & start the main tick loop
    LOGGER.info(I18n.trans("start-done", Duration.between(startInstant, Instant.now()).toMillis()));
    tickLoop.start();
  }
}
