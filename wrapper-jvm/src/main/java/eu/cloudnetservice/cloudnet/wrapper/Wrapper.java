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

package eu.cloudnetservice.cloudnet.wrapper;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.CloudNetVersion;
import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.module.DefaultModuleProviderHandler;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.chunk.defaults.factory.EventChunkHandlerFactory;
import eu.cloudnetservice.cloudnet.driver.network.chunk.network.ChunkedPacketListener;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import eu.cloudnetservice.cloudnet.driver.service.ProcessSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceId;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.cloudnet.driver.template.defaults.RemoteTemplateStorage;
import eu.cloudnetservice.cloudnet.wrapper.configuration.DocumentWrapperConfiguration;
import eu.cloudnetservice.cloudnet.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.cloudnet.wrapper.database.DefaultWrapperDatabaseProvider;
import eu.cloudnetservice.cloudnet.wrapper.event.ApplicationPostStartEvent;
import eu.cloudnetservice.cloudnet.wrapper.event.ApplicationPreStartEvent;
import eu.cloudnetservice.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import eu.cloudnetservice.cloudnet.wrapper.network.NetworkClientChannelHandler;
import eu.cloudnetservice.cloudnet.wrapper.network.chunk.TemplateStorageCallbackListener;
import eu.cloudnetservice.cloudnet.wrapper.network.listener.PacketAuthorizationResponseListener;
import eu.cloudnetservice.cloudnet.wrapper.network.listener.PacketServerChannelMessageListener;
import eu.cloudnetservice.cloudnet.wrapper.permission.WrapperPermissionManagement;
import eu.cloudnetservice.cloudnet.wrapper.provider.WrapperGeneralCloudServiceProvider;
import eu.cloudnetservice.cloudnet.wrapper.provider.WrapperGroupConfigurationProvider;
import eu.cloudnetservice.cloudnet.wrapper.provider.WrapperMessenger;
import eu.cloudnetservice.cloudnet.wrapper.provider.WrapperNodeInfoProvider;
import eu.cloudnetservice.cloudnet.wrapper.provider.WrapperServiceTaskProvider;
import eu.cloudnetservice.cloudnet.wrapper.transform.TransformerRegistry;
import eu.cloudnetservice.cloudnet.wrapper.transform.bukkit.BukkitCommodoreTransformer;
import eu.cloudnetservice.cloudnet.wrapper.transform.bukkit.BukkitJavaVersionCheckTransformer;
import eu.cloudnetservice.cloudnet.wrapper.transform.bukkit.PaperConfigTransformer;
import eu.cloudnetservice.cloudnet.wrapper.transform.netty.OldEpollDisableTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class is the main class of the application wrapper, which performs the basic driver functions and the setup of
 * the application to be wrapped.
 *
 * @see CloudNetDriver
 */
public class Wrapper extends CloudNetDriver {

  private static final Path WORKING_DIRECTORY = Path.of("");
  private static final Logger LOGGER = LogManager.logger(Wrapper.class);

  /**
   * The configuration of the wrapper, which was created from the CloudNet node. The properties are mirrored from the
   * configuration file.
   *
   * @see WrapperConfiguration
   */
  private final WrapperConfiguration config = DocumentWrapperConfiguration.load();

  private final RPCSender rpcSender;

  /**
   * The single task thread of the scheduler of the wrapper application
   */
  private final Thread mainThread = Thread.currentThread();

  /**
   * The ServiceInfoSnapshot instances. The current ServiceInfoSnapshot instance is the last send object snapshot from
   * this process. The lastServiceInfoSnapshot is the element which was send before.
   */
  private ServiceInfoSnapshot lastServiceInfoSnapShot = this.config.serviceInfoSnapshot();
  private ServiceInfoSnapshot currentServiceInfoSnapshot = this.config.serviceInfoSnapshot();

  protected Wrapper(@NonNull String[] args) {
    super(new ArrayList<>(Arrays.asList(args)));

    instance(this);

    this.cloudNetVersion = CloudNetVersion.fromClassInformation(Wrapper.class.getPackage());

    super.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, this.config.sslConfiguration());
    this.rpcSender = this.rpcProviderFactory.providerForClass(this.networkClient, CloudNetDriver.class);

    super.databaseProvider = new DefaultWrapperDatabaseProvider(this);

    super.messenger = new WrapperMessenger(this);
    super.nodeInfoProvider = new WrapperNodeInfoProvider(this);
    super.serviceTaskProvider = new WrapperServiceTaskProvider(this);
    super.groupConfigurationProvider = new WrapperGroupConfigurationProvider(this);
    super.generalCloudServiceProvider = new WrapperGeneralCloudServiceProvider(this);

    super.cloudServiceFactory = new RemoteCloudServiceFactory(
      this.networkClient::firstChannel,
      this.networkClient,
      this.rpcProviderFactory);

    super.moduleProvider.moduleProviderHandler(new DefaultModuleProviderHandler());
    super.moduleProvider.moduleDirectoryPath(Path.of(".wrapper", "modules"));

    super.permissionManagement(new WrapperPermissionManagement(this));
    super.driverEnvironment = DriverEnvironment.WRAPPER;
  }

  public static @NonNull Wrapper instance() {
    return (Wrapper) CloudNetDriver.instance();
  }

  @Override
  public void start(@NonNull Instant startInstant) throws Exception {
    // load & enable the modules
    this.moduleProvider.loadAll().startAll();

    // register our default class transformers
    this.transformerRegistry()
      .registerTransformer("org/bukkit/craftbukkit", "Commodore", new BukkitCommodoreTransformer());
    this.transformerRegistry()
      .registerTransformer("org/bukkit/craftbukkit", "Main", new BukkitJavaVersionCheckTransformer());
    this.transformerRegistry()
      .registerTransformer("org/github/paperspigot", "PaperSpigotConfig", new PaperConfigTransformer());
    // This prevents shadow from renaming io/netty to eu/cloudnetservice/io/netty
    this.transformerRegistry().registerTransformer(
      name -> name.endsWith("Epoll") && name.startsWith("io") && name.contains("netty/channel/epoll/"),
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

  @Override
  public void stop() {
    try {
      this.networkClient.close();
    } catch (Exception exception) {
      LOGGER.severe("Exception while closing the network client", exception);
    }

    this.scheduler.shutdownNow();
    this.moduleProvider.unloadAll();
    this.servicesRegistry.unregisterAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String componentName() {
    return this.serviceId().name();
  }

  @Override
  public @NonNull TemplateStorage localTemplateStorage() {
    return this.templateStorage(ServiceTemplate.LOCAL_STORAGE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String nodeUniqueId() {
    return this.serviceId().nodeUniqueId();
  }

  @Override
  public @NonNull TemplateStorage templateStorage(@NonNull String storage) {
    return new RemoteTemplateStorage(storage, this.rpcSender.invokeMethod("templateStorage", storage));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<TemplateStorage> availableTemplateStorages() {
    return this.rpcSender.invokeMethod("availableTemplateStorages").fireSync();
  }

  /**
   * Is an shortcut for Wrapper.getConfig().getServiceId()
   *
   * @return the ServiceId instance which was set in the config by the node
   */
  public @NonNull ServiceId serviceId() {
    return this.serviceConfiguration().serviceId();
  }

  /**
   * Is an shortcut for Wrapper.getConfig().getServiceConfiguration()
   *
   * @return the first instance which was set in the config by the node
   */
  public @NonNull ServiceConfiguration serviceConfiguration() {
    return this.config.serviceConfiguration();
  }

  /**
   * Creates a completed new ServiceInfoSnapshot instance based of the properties of the current ServiceInfoSnapshot
   * instance
   *
   * @return the new ServiceInfoSnapshot instance
   */
  public @NonNull ServiceInfoSnapshot createServiceInfoSnapshot() {
    return new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      this.currentServiceInfoSnapshot.address(),
      this.currentServiceInfoSnapshot.connectAddress(),
      ProcessSnapshot.self(),
      this.serviceConfiguration(),
      this.currentServiceInfoSnapshot.connectedTime(),
      ServiceLifeCycle.RUNNING,
      this.currentServiceInfoSnapshot.properties());
  }

  @Internal
  public @NonNull ServiceInfoSnapshot configureServiceInfoSnapshot() {
    var serviceInfoSnapshot = this.createServiceInfoSnapshot();
    this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    return serviceInfoSnapshot;
  }

  private void configureServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.eventManager.callEvent(new ServiceInfoSnapshotConfigureEvent(serviceInfoSnapshot));

    this.lastServiceInfoSnapShot = this.currentServiceInfoSnapshot;
    this.currentServiceInfoSnapshot = serviceInfoSnapshot;
  }

  /**
   * This method should be used to send the current ServiceInfoSnapshot and all subscribers on the network and to update
   * their information. It calls the ServiceInfoSnapshotConfigureEvent before send the update to the node.
   *
   * @see ServiceInfoSnapshotConfigureEvent
   */
  public void publishServiceInfoUpdate() {
    this.publishServiceInfoUpdate(this.createServiceInfoSnapshot());
  }

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
   * Removes all PacketListeners from all channels of the Network Connctor from a specific ClassLoader. It is
   * recommended to do this with the disables of your own plugin
   *
   * @param classLoader the ClassLoader from which the IPacketListener implementations derive.
   */
  public void unregisterPacketListenersByClassLoader(@NonNull ClassLoader classLoader) {
    this.networkClient.packetRegistry().removeListeners(classLoader);

    for (var channel : this.networkClient.channels()) {
      channel.packetRegistry().removeListeners(classLoader);
    }
  }

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
      this.networkClient.connect(this.config.targetListener());

      // wait for the authentication response
      var wasDone = condition.await(30, TimeUnit.SECONDS);
      // check if the auth was successful - explode if not
      if (!wasDone || !listener.wasAuthSuccessful()) {
        throw new IllegalStateException("Unable to authorize wrapper with node");
      }

      // set connected time
      this.currentServiceInfoSnapshot.connectedTime(System.currentTimeMillis());

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
    this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, arguments));

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

  public @NonNull WrapperConfiguration config() {
    return this.config;
  }

  public @NonNull Path workingDirectory() {
    return WORKING_DIRECTORY;
  }

  @UnmodifiableView
  public @NonNull List<String> commandLineArguments() {
    return Collections.unmodifiableList(this.commandLineArguments);
  }

  public @NonNull Thread mainThread() {
    return this.mainThread;
  }

  public @NonNull ServiceInfoSnapshot lastServiceInfo() {
    return this.lastServiceInfoSnapShot;
  }

  public @NonNull ServiceInfoSnapshot currentServiceInfo() {
    return this.currentServiceInfoSnapshot;
  }

  public @NonNull TransformerRegistry transformerRegistry() {
    return Premain.transformerRegistry;
  }
}
