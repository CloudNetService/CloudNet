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

package de.dytanic.cloudnet.wrapper;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.CloudNetVersion;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.module.DefaultModuleProviderHandler;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.defaults.factory.EventChunkHandlerFactory;
import de.dytanic.cloudnet.driver.network.chunk.network.ChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.driver.template.defaults.RemoteTemplateStorage;
import de.dytanic.cloudnet.wrapper.configuration.DocumentWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.configuration.IWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.database.DefaultWrapperDatabaseProvider;
import de.dytanic.cloudnet.wrapper.event.ApplicationPostStartEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPreStartEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.network.NetworkClientChannelHandler;
import de.dytanic.cloudnet.wrapper.network.chunk.TemplateStorageCallbackListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketAuthorizationResponseListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerChannelMessageListener;
import de.dytanic.cloudnet.wrapper.permission.WrapperPermissionManagement;
import de.dytanic.cloudnet.wrapper.provider.WrapperGeneralCloudServiceProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperGroupConfigurationProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperMessenger;
import de.dytanic.cloudnet.wrapper.provider.WrapperNodeInfoProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperServiceTaskProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
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
   * @see IWrapperConfiguration
   */
  private final IWrapperConfiguration config = DocumentWrapperConfiguration.load();

  private final RPCSender rpcSender;

  /**
   * The single task thread of the scheduler of the wrapper application
   */
  private final Thread mainThread = Thread.currentThread();

  /**
   * The ServiceInfoSnapshot instances. The current ServiceInfoSnapshot instance is the last send object snapshot from
   * this process. The lastServiceInfoSnapshot is the element which was send before.
   */
  private ServiceInfoSnapshot lastServiceInfoSnapShot = this.config.getServiceInfoSnapshot();
  private ServiceInfoSnapshot currentServiceInfoSnapshot = this.config.getServiceInfoSnapshot();

  protected Wrapper(@NotNull String[] args) {
    super(new ArrayList<>(Arrays.asList(args)));

    instance(this);

    this.cloudNetVersion = CloudNetVersion.fromClassInformation(Wrapper.class.getPackage());

    super.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, this.config.getSSLConfig());
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

  public static @NotNull Wrapper getInstance() {
    return (Wrapper) CloudNetDriver.instance();
  }

  @Override
  public synchronized void start() throws Exception {
    // load & enable the modules
    this.moduleProvider.loadAll().startAll();

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
  public @NotNull String componentName() {
    return this.serviceId().name();
  }

  @Override
  public @NotNull TemplateStorage localTemplateStorage() {
    return this.templateStorage(ServiceTemplate.LOCAL_STORAGE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String nodeUniqueId() {
    return this.serviceId().nodeUniqueId();
  }

  @Override
  public @NotNull TemplateStorage templateStorage(@NotNull String storage) {
    return new RemoteTemplateStorage(storage, this.rpcSender.invokeMethod("templateStorage", storage));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<TemplateStorage> availableTemplateStorages() {
    return this.rpcSender.invokeMethod("availableTemplateStorages").fireSync();
  }

  /**
   * Application wrapper implementation of this method. See the full documentation at the CloudNetDriver class.
   *
   * @see CloudNetDriver#sendCommandLineAsPermissionUser(UUID, String)
   */
  @Override
  public @NotNull Collection<String> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String command) {
    return this.rpcSender.invokeMethod("sendCommandLineAsPermissionUser", uniqueId, command).fireSync();
  }

  /**
   * Is an shortcut for Wrapper.getConfig().getServiceId()
   *
   * @return the ServiceId instance which was set in the config by the node
   */
  public @NotNull ServiceId serviceId() {
    return this.serviceConfiguration().serviceId();
  }

  /**
   * Is an shortcut for Wrapper.getConfig().getServiceConfiguration()
   *
   * @return the first instance which was set in the config by the node
   */
  public @NotNull ServiceConfiguration serviceConfiguration() {
    return this.config.getServiceConfiguration();
  }

  /**
   * Creates a completed new ServiceInfoSnapshot instance based of the properties of the current ServiceInfoSnapshot
   * instance
   *
   * @return the new ServiceInfoSnapshot instance
   */
  @NotNull
  public ServiceInfoSnapshot createServiceInfoSnapshot() {
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
  public ServiceInfoSnapshot configureServiceInfoSnapshot() {
    var serviceInfoSnapshot = this.createServiceInfoSnapshot();
    this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    return serviceInfoSnapshot;
  }

  private void configureServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
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

  public void publishServiceInfoUpdate(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
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
  public void unregisterPacketListenersByClassLoader(@NotNull ClassLoader classLoader) {
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
      this.networkClient.connect(this.config.getTargetListener());

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

  public @NotNull IWrapperConfiguration config() {
    return this.config;
  }

  public @NotNull Path workingDirectory() {
    return WORKING_DIRECTORY;
  }

  @UnmodifiableView
  public @NotNull List<String> commandLineArguments() {
    return Collections.unmodifiableList(this.commandLineArguments);
  }

  public @NotNull Thread mainThread() {
    return this.mainThread;
  }

  public @NotNull ServiceInfoSnapshot lastServiceInfo() {
    return this.lastServiceInfoSnapShot;
  }

  public @NotNull ServiceInfoSnapshot currentServiceInfo() {
    return this.currentServiceInfoSnapshot;
  }
}
