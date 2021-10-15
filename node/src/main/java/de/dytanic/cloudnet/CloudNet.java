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
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
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
import de.dytanic.cloudnet.driver.module.DefaultPersistableModuleDependencyLoader;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.event.CloudNetNodePostInitializationEvent;
import de.dytanic.cloudnet.module.NodeModuleProviderHandler;
import de.dytanic.cloudnet.network.NetworkClientChannelHandlerImpl;
import de.dytanic.cloudnet.network.NetworkServerChannelHandlerImpl;
import de.dytanic.cloudnet.network.chunk.TemplateDeployCallbackListener;
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
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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

  private volatile IConfiguration configuration;
  private volatile AbstractDatabaseProvider databaseProvider;

  protected CloudNet(@NotNull String[] args, @NotNull IConsole console, CommandProvider commandProvider) {
    super(Arrays.asList(args));

    setInstance(this);

    this.commandProvider = commandProvider;
    this.console = console;
    this.serviceVersionProvider = new ServiceVersionProvider(console);
    this.cloudNetVersion = CloudNetVersion.fromClassInformation(CloudNet.class.getPackage());

    this.configuration = JsonConfiguration.loadFromFile();
    this.configuration.load();

    this.nodeServerProvider = new DefaultClusterNodeServerProvider(this);
    this.nodeServerProvider.setClusterServers(this.configuration.getClusterConfig());

    this.nodeInfoProvider = new NodeNodeInfoProvider(this.nodeServerProvider);
    this.serviceTaskProvider = new NodeServiceTaskProvider(this.eventManager);
    this.generalCloudServiceProvider = new DefaultCloudServiceManager(this);
    this.groupConfigurationProvider = new NodeGroupConfigurationProvider(this.eventManager);

    this.messenger = new NodeMessenger(this.getCloudServiceProvider(), this.nodeServerProvider);
    this.cloudServiceFactory = new NodeCloudServiceFactory(
      this.eventManager,
      this.getCloudServiceProvider(),
      this.nodeServerProvider);

    // permission management init
    this.setPermissionManagement(new DefaultDatabasePermissionManagement(this));
    this.getPermissionManagement().setPermissionManagementHandler(
      new DefaultPermissionManagementHandler(this.eventManager));

    this.moduleProvider.setModuleDependencyLoader(
      new DefaultPersistableModuleDependencyLoader(LAUNCHER_DIR.resolve("libs")));
    this.moduleProvider.setModuleProviderHandler(new NodeModuleProviderHandler(this));

    this.networkClient = new NettyNetworkClient(
      NetworkClientChannelHandlerImpl::new,
      this.configuration.getClientSslConfig());
    this.networkServer = new NettyNetworkServer(
      NetworkServerChannelHandlerImpl::new,
      this.configuration.getServerSslConfig());
    this.httpServer = new NettyHttpServer(this.configuration.getWebSslConfig());

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

    // load the modules before proceeding for example to allow the database provider init
    this.moduleProvider.loadAll();

    // check if there is a database provider or initialize the default one
    if (this.databaseProvider == null || !this.databaseProvider.init()) {
      this.databaseProvider = this.servicesRegistry.getService(AbstractDatabaseProvider.class, "xodus");
      this.databaseProvider.init();
    }

    // network server init
    for (HostAndPort listener : this.configuration.getIdentity().getListeners()) {
      this.networkServer.addListener(listener);
    }
    // http server init
    for (HostAndPort httpListener : this.configuration.getHttpListeners()) {
      this.httpServer.addListener(httpListener);
    }
    // network client init
    for (NetworkClusterNode node : this.configuration.getClusterConfig().getNodes()) {
      HostAndPort[] listeners = node.getListeners();
      // check if there are any listeners
      if (listeners.length > 0) {
        // get a random listener of the node
        HostAndPort listener = listeners[ThreadLocalRandom.current().nextInt(0, listeners.length)];
        this.networkClient.connect(listener);
      }
    }

    this.moduleProvider.startAll();

    this.eventManager.registerListener(new TemplateDeployCallbackListener());
    this.eventManager.callEvent(new CloudNetNodePostInitializationEvent(this));

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Shutdown Thread"));

    this.mainThread.start();
    // todo this.nodeServerProvider.getSelfNode().publishNodeInfoSnapshotUpdate();
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

  public void setDatabaseProvider(@NotNull AbstractDatabaseProvider databaseProvider) {
    this.databaseProvider = databaseProvider;
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

  @NotNull
  public CommandProvider getCommandProvider() {
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

  public boolean isRunning() {
    return this.running.get();
  }
}
