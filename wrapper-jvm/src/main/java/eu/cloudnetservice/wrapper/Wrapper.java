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

package eu.cloudnetservice.wrapper;

import com.google.common.collect.Lists;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.module.DefaultModuleProviderHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.defaults.factory.EventChunkHandlerFactory;
import eu.cloudnetservice.driver.network.chunk.network.ChunkedPacketListener;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.wrapper.configuration.DocumentWrapperConfiguration;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.database.DefaultWrapperDatabaseProvider;
import eu.cloudnetservice.wrapper.event.ApplicationPostStartEvent;
import eu.cloudnetservice.wrapper.event.ApplicationPreStartEvent;
import eu.cloudnetservice.wrapper.event.ServiceInfoSnapshotConfigureEvent;
import eu.cloudnetservice.wrapper.network.NetworkClientChannelHandler;
import eu.cloudnetservice.wrapper.network.chunk.TemplateStorageCallbackListener;
import eu.cloudnetservice.wrapper.network.listener.PacketAuthorizationResponseListener;
import eu.cloudnetservice.wrapper.network.listener.PacketServerChannelMessageListener;
import eu.cloudnetservice.wrapper.network.listener.message.GroupChannelMessageListener;
import eu.cloudnetservice.wrapper.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.wrapper.network.listener.message.TaskChannelMessageListener;
import eu.cloudnetservice.wrapper.permission.WrapperPermissionManagement;
import eu.cloudnetservice.wrapper.provider.WrapperCloudServiceProvider;
import eu.cloudnetservice.wrapper.provider.WrapperMessenger;
import eu.cloudnetservice.wrapper.provider.WrapperTemplateStorageProvider;
import eu.cloudnetservice.wrapper.transform.TransformerRegistry;
import eu.cloudnetservice.wrapper.transform.bukkit.BukkitCommodoreTransformer;
import eu.cloudnetservice.wrapper.transform.bukkit.BukkitJavaVersionCheckTransformer;
import eu.cloudnetservice.wrapper.transform.bukkit.PaperConfigTransformer;
import eu.cloudnetservice.wrapper.transform.netty.OldEpollDisableTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;
import lombok.NonNull;

/**
 * This class is the main api point when trying to interact with CloudNet from a running service within the CloudNet
 * network.
 *
 * @since 4.0
 */
public class Wrapper extends CloudNetDriver {

  private static final Path WORKING_DIRECTORY = Path.of("");
  private static final Logger LOGGER = LogManager.logger(Wrapper.class);

  private final Thread mainThread = Thread.currentThread();
  private final WrapperConfiguration config = DocumentWrapperConfiguration.load();

  private ServiceInfoSnapshot lastServiceInfoSnapShot = this.config.serviceInfoSnapshot();
  private ServiceInfoSnapshot currentServiceInfoSnapshot = this.config.serviceInfoSnapshot();

  /**
   * Constructs a new CloudNet wrapper instance. This constructor shouldn't be used directly and is only accessible for
   * any class which provides a custom implementation of the wrapper. Use {@link #instance()} to obtain the current
   * wrapper instance of the environment instead.
   *
   * @param args the command line arguments which were supplied to the wrapper when starting.
   * @throws NullPointerException if the given arguments are null.
   */
  protected Wrapper(@NonNull String[] args) {
    super(CloudNetVersion.fromPackage(Wrapper.class.getPackage()), Lists.newArrayList(args), DriverEnvironment.WRAPPER);

    instance(this);

    super.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, this.config.sslConfiguration());
    super.messenger = new WrapperMessenger(this);

    // auto generated providers
    super.clusterNodeProvider = this.rpcFactory.generateRPCBasedApi(
      ClusterNodeProvider.class,
      GenerationContext.forClass(ClusterNodeProvider.class).component(this.networkClient).build());
    super.serviceTaskProvider = this.rpcFactory.generateRPCBasedApi(
      ServiceTaskProvider.class,
      GenerationContext.forClass(ServiceTaskProvider.class).component(this.networkClient).build());
    super.groupConfigurationProvider = this.rpcFactory.generateRPCBasedApi(
      GroupConfigurationProvider.class,
      GenerationContext.forClass(GroupConfigurationProvider.class).component(this.networkClient).build());
    super.cloudServiceFactory = this.rpcFactory().generateRPCBasedApi(
      CloudServiceFactory.class,
      GenerationContext.forClass(CloudServiceFactory.class).component(this.networkClient).build());

    // these contain some methods which we cannot auto generate directly
    super.templateStorageProvider = this.rpcFactory.generateRPCBasedApi(
      TemplateStorageProvider.class,
      GenerationContext.forClass(WrapperTemplateStorageProvider.class).component(this.networkClient).build());
    super.databaseProvider = this.rpcFactory.generateRPCBasedApi(
      DatabaseProvider.class,
      GenerationContext.forClass(DefaultWrapperDatabaseProvider.class).component(this.networkClient).build());
    super.cloudServiceProvider = this.rpcFactory.generateRPCBasedApi(
      CloudServiceProvider.class,
      GenerationContext.forClass(WrapperCloudServiceProvider.class).component(this.networkClient).build());

    // channel message listeners for downstream event calls
    this.eventManager.registerListener(new TaskChannelMessageListener(this.eventManager));
    this.eventManager.registerListener(new GroupChannelMessageListener(this.eventManager));
    this.eventManager.registerListener(new ServiceChannelMessageListener(this.eventManager));

    super.moduleProvider.moduleProviderHandler(new DefaultModuleProviderHandler());
    super.moduleProvider.moduleDirectoryPath(Path.of(".wrapper", "modules"));

    var management = this.rpcFactory.generateRPCBasedApi(
      PermissionManagement.class,
      GenerationContext.forClass(WrapperPermissionManagement.class).component(this.networkClient).build());
    super.permissionManagement(management);
  }

  /**
   * Get the singleton instance of the currently running CloudNet wrapper. This instance is initialized once when
   * starting the wrapper and will never change during the lifetime of the jvm. Therefore, the instance of the wrapper
   * can be safely cached.
   *
   * @return the current, jvm static wrapper instance.
   */
  public static @NonNull Wrapper instance() {
    return CloudNetDriver.instance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void start(@NonNull Instant startInstant) throws Exception {
    // load & enable the modules
    this.moduleProvider.loadAll().startAll();

    // register our default class transformers
    this.transformerRegistry().registerTransformer(
      "org/bukkit/craftbukkit",
      "Commodore",
      new BukkitCommodoreTransformer());
    this.transformerRegistry().registerTransformer(
      "org/bukkit/craftbukkit",
      "Main",
      new BukkitJavaVersionCheckTransformer());
    this.transformerRegistry().registerTransformer(
      "org/github/paperspigot",
      "PaperSpigotConfig",
      new PaperConfigTransformer());
    // This prevents shadow from renaming io/netty to eu/cloudnetservice/io/netty
    this.transformerRegistry().registerTransformer(
      String.join("/", "io", "netty", "channel", "epoll"),
      "Epoll",
      new OldEpollDisableTransformer());

    // connect to the node
    this.connectToNode();

    // initialize
    this.permissionManagement.init();
    this.eventManager.registerListener(new TemplateStorageCallbackListener());

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    // start the application
    if (!this.startApplication()) {
      System.exit(-1);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    try {
      this.networkClient.close();
    } catch (Exception exception) {
      LOGGER.severe("Exception while closing the network client", exception);
    }

    this.scheduler.shutdownNow();
    this.moduleProvider.unloadAll();
    this.serviceRegistry.unregisterAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String componentName() {
    return this.serviceId().name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String nodeUniqueId() {
    return this.serviceId().nodeUniqueId();
  }

  /**
   * Get the id of the service this wrapper is associated with.
   *
   * @return the id of the service this wrapper is associated with.
   */
  public @NonNull ServiceId serviceId() {
    return this.serviceConfiguration().serviceId();
  }

  /**
   * Get the service configuration which was used to create this service instance.
   *
   * @return the service configuration which was used to create this service instance.
   */
  public @NonNull ServiceConfiguration serviceConfiguration() {
    return this.config.serviceConfiguration();
  }

  /**
   * Creates a new service snapshot of this service, copying over the properties of the current service info snapshot.
   * Only the creation time and process snapshot of the new snapshot differ from the old snapshot.
   *
   * @return the newly created service snapshot for this service.
   */
  public @NonNull ServiceInfoSnapshot createServiceInfoSnapshot() {
    return this.createServiceInfoSnapshot(this.currentServiceInfoSnapshot.properties());
  }

  /**
   * Creates a new service snapshot of this service using the provided properties for the new snapshot.
   * Only the creation time and process snapshot and properties of the new snapshot differ from the old snapshot.
   * <p>
   * Changes made to the current service snapshot (like modifying the properties of it) will not reflect into the
   * snapshot returned by this method.
   *
   * @return the newly created service snapshot for this service.
   */
  public @NonNull ServiceInfoSnapshot createServiceInfoSnapshot(@NonNull JsonDocument properties) {
    return new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      this.currentServiceInfoSnapshot.address(),
      ProcessSnapshot.self(),
      this.serviceConfiguration(),
      this.currentServiceInfoSnapshot.connectedTime(),
      ServiceLifeCycle.RUNNING,
      properties.clone());
  }

  /**
   * Creates a new service snapshot of this service, copying over the properties of the current service info snapshot,
   * then configuring it using the registered listeners of the {@code ServiceInfoSnapshotConfigureEvent}.
   * <p>
   * Changes made to the current service snapshot (like modifying the properties of it) will not reflect into the
   * snapshot returned by this method.
   * <p>
   * This method will (unlike the {@code createServiceInfoSnapshot} method) change the current and old service snapshot
   * to the newly created and configured one.
   * <p>
   * This method call is equivalent to {@code wrapper.configureServiceInfoSnapshot(wrapper.createServiceInfoSnapshot)}.
   *
   * @return the newly created service snapshot for this service.
   */
  public @NonNull ServiceInfoSnapshot configureServiceInfoSnapshot() {
    var serviceInfoSnapshot = this.createServiceInfoSnapshot();
    this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    return serviceInfoSnapshot;
  }

  /**
   * Configures the given service info snapshot and updates the current and old service snapshot.
   *
   * @param serviceInfoSnapshot the service snapshot to configure.
   * @throws NullPointerException if the given snapshot is null.
   */
  private void configureServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.eventManager.callEvent(new ServiceInfoSnapshotConfigureEvent(serviceInfoSnapshot));

    this.lastServiceInfoSnapShot = this.currentServiceInfoSnapshot;
    this.currentServiceInfoSnapshot = serviceInfoSnapshot;
  }

  /**
   * Creates a new service snapshot, configures it, updates the current and old one and sends an update to all
   * components which are currently registered within the CloudNet network.
   */
  public void publishServiceInfoUpdate() {
    this.publishServiceInfoUpdate(this.createServiceInfoSnapshot());
  }

  /**
   * Updates the given service snapshot to all components which are currently registered within the CloudNet network.
   * This method will configure the given snapshot if it belongs to the current wrapper instance.
   *
   * @param serviceInfoSnapshot the service snapshot to update.
   * @throws NullPointerException if the given service snapshot is null.
   */
  public void publishServiceInfoUpdate(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    // add configuration stuff when updating the current service snapshot
    if (this.currentServiceInfoSnapshot.serviceId().equals(serviceInfoSnapshot.serviceId())) {
      this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    }

    // send the update to all nodes and services
    ChannelMessage.builder()
      .targetAll()
      .message("update_service_info")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(serviceInfoSnapshot))
      .build()
      .send();
  }

  /**
   * Connects this wrapper with the node which started this service, allowing a maximum of 30 seconds for the connection
   * to establish completely (including the node authorization process).
   * <p>
   * If the connection was successful a new service snapshot with the connection time will be created, and the channel
   * of the node connection will be setup to receive and send packets.
   *
   * @throws InterruptedException  if the calling thread was interrupted while waiting for the connection to be ready.
   * @throws IllegalStateException if the node authorization failed for some reason (including timeout).
   */
  private void connectToNode() throws InterruptedException {
    Lock lock = new ReentrantLock();

    try {
      // acquire the lock on the current thread
      lock.lock();
      // create a new condition and the auth listener
      var condition = lock.newCondition();
      var listener = new PacketAuthorizationResponseListener(lock, condition);
      // register the listener to the packet registry and connect to the target listener
      this.networkClient.packetRegistry().addListener(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL, listener);
      this.networkClient
        .connect(this.config.targetListener())
        .exceptionally(ex -> {
          // log and exit, we're not connected
          LOGGER.severe("Unable to connect", ex);
          System.exit(-1);
          // returns void
          return null;
        }).join();

      // wait for the authentication response
      var wasDone = condition.await(30, TimeUnit.SECONDS);
      // check if the auth was successful - explode if not
      if (!wasDone || !listener.wasAuthSuccessful()) {
        throw new IllegalStateException("Unable to authorize wrapper with node");
      }

      // set a new current snapshot with the connected time
      this.currentServiceInfoSnapshot = new ServiceInfoSnapshot(
        System.currentTimeMillis(),
        this.currentServiceInfoSnapshot.address(),
        ProcessSnapshot.self(),
        this.serviceConfiguration(),
        System.currentTimeMillis(),
        ServiceLifeCycle.RUNNING,
        this.currentServiceInfoSnapshot.properties());

      // remove the auth listener
      this.networkClient.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);

      // add the runtime packet listeners
      this.networkClient.packetRegistry().addListener(
        NetworkConstants.CHUNKED_PACKET_COM_CHANNEL,
        new ChunkedPacketListener(EventChunkHandlerFactory.withDefaultEventManager()));
      this.networkClient.packetRegistry().addListener(
        NetworkConstants.CHANNEL_MESSAGING_CHANNEL,
        new PacketServerChannelMessageListener());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Starts the underlying application file in a separate thread based on the command line arguments passed to wrapper.
   * These must include (in order):
   * <ol>
   *   <li>The main class of the application.
   *   <li>The premain class of the application.
   *   <li>The path of the application file to start.
   *   <li>If all files in the jar should get pre-loaded by a non-system class loader (boolean).
   * </ol>
   * <p>
   * The command line arguments will be removed from the known arguments and not passed to the application when starting.
   *
   * @return true if the application was started successfully, false otherwise.
   * @throws Exception if any exception occurs while starting the application.
   */
  private boolean startApplication() throws Exception {
    // get all the information provided through the command line
    var mainClass = this.commandLineArguments.remove(0);
    var premainClass = this.commandLineArguments.remove(0);
    var appFile = Path.of(this.commandLineArguments.remove(0));
    var preLoadAppJar = Boolean.parseBoolean(this.commandLineArguments.remove(0));

    var loader = ClassLoader.getSystemClassLoader();
    // preload all jars in the application if requested
    if (preLoadAppJar) {
      // create a custom class loader for loading the application resources
      loader = new URLClassLoader(
        new URL[]{appFile.toUri().toURL()},
        ClassLoader.getSystemClassLoader());
      // force our loader to load all classes in the jar
      Premain.preloadClasses(appFile, loader);
    }

    // append the application file to the system class path
    Premain.instrumentation.appendToSystemClassLoaderSearch(new JarFile(appFile.toFile()));

    // invoke the premain method if given
    Premain.invokePremain(premainClass, loader);

    // get the main method
    var main = Class.forName(mainClass, true, loader);
    var method = main.getMethod("main", String[].class);

    // inform the user about the pre-start
    Collection<String> arguments = new ArrayList<>(this.commandLineArguments);
    this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, arguments, loader));

    // start the application
    var applicationThread = new Thread(() -> {
      try {
        LOGGER.info(String.format("Starting application using class %s (pre-main: %s)", mainClass, premainClass));
        // start the application
        method.invoke(null, new Object[]{arguments.toArray(new String[0])});
      } catch (Exception exception) {
        LOGGER.severe("Exception while starting application", exception);
      }
    }, "Application-Thread");
    applicationThread.setContextClassLoader(loader);
    applicationThread.start();

    // inform the user about the post-start
    this.eventManager.callEvent(new ApplicationPostStartEvent(this, main, applicationThread, loader));
    return true;
  }

  /**
   * Get the configuration of the wrapper which is created based on the service information the node has available.
   *
   * @return the configuration of the wrapper.
   */
  public @NonNull WrapperConfiguration config() {
    return this.config;
  }

  /**
   * Get the directory in which the wrapper is currently running.
   *
   * @return the directory in which the wrapper is currently running.
   */
  public @NonNull Path workingDirectory() {
    return WORKING_DIRECTORY;
  }

  /**
   * Get the main thread of the wrapper. This is not the main thread of the application started by the wrapper!
   *
   * @return the main thread of the wrapper.
   */
  public @NonNull Thread mainThread() {
    return this.mainThread;
  }

  /**
   * Gets the last service info snapshot before the current one was created.
   *
   * @return the last service info snapshot.
   */
  public @NonNull ServiceInfoSnapshot lastServiceInfo() {
    return this.lastServiceInfoSnapShot;
  }

  /**
   * Get the current service info snapshot which is synced to all components in the network.
   *
   * @return the current service info snapshot.
   */
  public @NonNull ServiceInfoSnapshot currentServiceInfo() {
    return this.currentServiceInfoSnapshot;
  }

  /**
   * Get the transformer registry of the wrapper. This should mostly get used by CloudNet modules, as most (if not all)
   * classes of an application are already loaded when a plugin, extension, etc. on the application layer gets loaded.
   *
   * @return the transformer registry of the wrapper.
   */
  public @NonNull TransformerRegistry transformerRegistry() {
    return Premain.transformerRegistry;
  }
}
