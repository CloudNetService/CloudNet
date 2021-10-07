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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.module.DefaultModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.wrapper.conf.DocumentWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.conf.IWrapperConfiguration;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;
import de.dytanic.cloudnet.wrapper.database.defaults.DefaultWrapperDatabaseProvider;
import de.dytanic.cloudnet.wrapper.event.ApplicationPostStartEvent;
import de.dytanic.cloudnet.wrapper.event.ApplicationPreStartEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.network.NetworkClientChannelHandler;
import de.dytanic.cloudnet.wrapper.network.listener.ChannelMessageServerListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerAuthorizationResponseListener;
import de.dytanic.cloudnet.wrapper.network.listener.PacketServerChannelMessageListener;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientServiceInfoUpdate;
import de.dytanic.cloudnet.wrapper.permission.WrapperPermissionManagement;
import de.dytanic.cloudnet.wrapper.provider.WrapperGroupConfigurationProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperMessenger;
import de.dytanic.cloudnet.wrapper.provider.WrapperNodeInfoProvider;
import de.dytanic.cloudnet.wrapper.provider.WrapperServiceTaskProvider;
import de.dytanic.cloudnet.wrapper.provider.service.WrapperGeneralCloudServiceProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This class is the main class of the application wrapper, which performs the basic driver functions and the setup of
 * the application to be wrapped.
 *
 * @see CloudNetDriver
 */
public final class Wrapper extends CloudNetDriver {

  private static final Logger LOGGER = LogManager.getLogger(Wrapper.class);

  /**
   * The configuration of the wrapper, which was created from the CloudNet node. The properties are mirrored from the
   * configuration file.
   *
   * @see IWrapperConfiguration
   */
  private final IWrapperConfiguration config = new DocumentWrapperConfiguration();

  /**
   * The default workDirectory of this process as File instance
   */
  private final Path workDirectory = Paths.get("");

  /**
   * The commandline arguments from the main() method of Main class by the application wrapper
   */
  private final List<String> commandLineArguments;

  /**
   * CloudNetDriver.getNetworkClient()
   *
   * @see CloudNetDriver
   */
  private final INetworkClient networkClient;
  private final RPCSender rpcSender;
  /**
   * The single task thread of the scheduler of the wrapper application
   */
  private final Thread mainThread = Thread.currentThread();
  private IDatabaseProvider databaseProvider;
  /**
   * The ServiceInfoSnapshot instances. The current ServiceInfoSnapshot instance is the last send object snapshot from
   * this process. The lastServiceInfoSnapshot is the element which was send before.
   */
  private ServiceInfoSnapshot lastServiceInfoSnapShot = this.config.getServiceInfoSnapshot();
  private ServiceInfoSnapshot currentServiceInfoSnapshot = this.config.getServiceInfoSnapshot();

  Wrapper(List<String> commandLineArguments) {
    setInstance(this);

    this.networkClient = new NettyNetworkClient(NetworkClientChannelHandler::new, this.config.getSSLConfig());
    this.databaseProvider = new DefaultWrapperDatabaseProvider(this);
    this.rpcSender = this.rpcProviderFactory.providerForClass(this.getNetworkClient(), CloudNetDriver.class);

    super.cloudServiceFactory = new RemoteCloudServiceFactory(this::getNetworkChannel, this.networkClient,
      this.rpcProviderFactory);
    super.generalCloudServiceProvider = new WrapperGeneralCloudServiceProvider(this);
    super.serviceTaskProvider = new WrapperServiceTaskProvider(this);
    super.groupConfigurationProvider = new WrapperGroupConfigurationProvider(this);
    super.nodeInfoProvider = new WrapperNodeInfoProvider(this);
    super.messenger = new WrapperMessenger(this);

    this.commandLineArguments = commandLineArguments;

    super.setPermissionManagement(new WrapperPermissionManagement(this));

    //- Packet client registry
    this.networkClient.getPacketRegistry()
      .addListener(NetworkConstants.CHANNEL_MESSAGING_CHANNEL, new PacketServerChannelMessageListener());

    new ChannelMessageServerListener();

    this.moduleProvider.setModuleDirectoryPath(Paths.get(".wrapper", "modules"));
    this.moduleProvider.setModuleProviderHandler(new DefaultModuleProviderHandler());
    this.driverEnvironment = DriverEnvironment.WRAPPER;
  }

  public static @NotNull Wrapper getInstance() {
    return (Wrapper) CloudNetDriver.getInstance();
  }

  @Override
  public synchronized void start() throws Exception {
    this.enableModules();

    ReentrantLock lock = new ReentrantLock();
    PacketServerAuthorizationResponseListener listener;

    try {
      lock.lock();

      Condition condition = lock.newCondition();
      listener = new PacketServerAuthorizationResponseListener(lock, condition);

      this.networkClient.getPacketRegistry().addListener(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL, listener);
      this.networkClient.connect(this.config.getTargetListener());

      condition.await();

    } finally {
      lock.unlock();
    }

    this.networkClient.getPacketRegistry().removeListener(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);

    this.permissionManagement.init();

    if (!listener.isResult()) {
      throw new IllegalStateException("authorization response is: denied");
    }

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

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
    this.taskScheduler.shutdown();
    this.moduleProvider.unloadAll();
    this.eventManager.unregisterAll();
    this.servicesRegistry.unregisterAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String getComponentName() {
    return this.getServiceId().getName();
  }

  @Override
  public @NotNull TemplateStorage getLocalTemplateStorage() {
    return this.getTemplateStorage(ServiceTemplate.LOCAL_STORAGE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String getNodeUniqueId() {
    return this.getServiceId().getNodeUniqueId();
  }

  //TODO: add back when the TemplateStorage is implemented
  @Override
  public @NotNull TemplateStorage getTemplateStorage(String storage) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
    return this.rpcSender.invokeMethod("getAvailableTemplateStorages").fireSync();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync() {
    return CompletableTask.supplyAsync(this::getAvailableTemplateStorages);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
    return new RemoteSpecificCloudServiceProvider(this.getCloudServiceProvider(), this.rpcSender, name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
    return new RemoteSpecificCloudServiceProvider(this.getCloudServiceProvider(), this.rpcSender, uniqueId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return new RemoteSpecificCloudServiceProvider(this.getCloudServiceProvider(), this.rpcSender,
      serviceInfoSnapshot.getServiceId().getUniqueId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGlobalLogLevel(Level logLevel) {
    this.networkClient.sendPacket(new PacketServerSetGlobalLogLevel(logLevel.getName()));
  }

  /**
   * Application wrapper implementation of this method. See the full documentation at the CloudNetDriver class.
   *
   * @see CloudNetDriver
   */
  @Override
  public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
    Preconditions.checkNotNull(uniqueId);
    Preconditions.checkNotNull(commandLine);

    return this.rpcSender.invokeMethod("sendCommandLineAsPermissionUser", uniqueId, commandLine).fireSync();
  }

  /**
   * Application wrapper implementation of this method. See the full documentation at the CloudNetDriver class.
   *
   * @see CloudNetDriver
   */
  @Override
  @NotNull
  public ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId,
    @NotNull String commandLine) {
    return CompletableTask.supplyAsync(() -> this.sendCommandLineAsPermissionUser(uniqueId, commandLine));
  }


  /**
   * Is an shortcut for Wrapper.getConfig().getServiceId()
   *
   * @return the ServiceId instance which was set in the config by the node
   */
  public ServiceId getServiceId() {
    return this.config.getServiceConfiguration().getServiceId();
  }

  /**
   * Is an shortcut for Wrapper.getConfig().getServiceConfiguration()
   *
   * @return the first instance which was set in the config by the node
   */
  public ServiceConfiguration getServiceConfiguration() {
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
      this.currentServiceInfoSnapshot.getConnectedTime(),
      this.currentServiceInfoSnapshot.getAddress(),
      this.currentServiceInfoSnapshot.getConnectAddress(),
      ServiceLifeCycle.RUNNING,
      ProcessSnapshot.self(),
      this.getServiceConfiguration(),
      this.currentServiceInfoSnapshot.getProperties()
    );
  }

  @ApiStatus.Internal
  public ServiceInfoSnapshot configureServiceInfoSnapshot() {
    ServiceInfoSnapshot serviceInfoSnapshot = this.createServiceInfoSnapshot();
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
    if (this.currentServiceInfoSnapshot.getServiceId().equals(serviceInfoSnapshot.getServiceId())) {
      this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.networkClient.sendPacket(new PacketClientServiceInfoUpdate(serviceInfoSnapshot));
  }


  /**
   * Removes all PacketListeners from all channels of the Network Connctor from a specific ClassLoader. It is
   * recommended to do this with the disables of your own plugin
   *
   * @param classLoader the ClassLoader from which the IPacketListener implementations derive.
   */
  public void unregisterPacketListenersByClassLoader(@NotNull ClassLoader classLoader) {
    this.networkClient.getPacketRegistry().removeListeners(classLoader);

    for (INetworkChannel channel : this.networkClient.getChannels()) {
      channel.getPacketRegistry().removeListeners(classLoader);
    }
  }

  private void enableModules() {
    Path moduleDirectory = Paths.get(System.getProperty("cloudnet.module.dir", ".wrapper/modules"));
    FileUtils.createDirectoryReported(moduleDirectory);
    FileUtils.walkFileTree(moduleDirectory, (root, current) -> {
      IModuleWrapper wrapper = this.moduleProvider.loadModule(current);
      if (wrapper != null) {
        wrapper.startModule();
      }
    }, false, "*.{jar,war,zip}");
  }

  private boolean startApplication() throws Exception {
    String mainClass = this.commandLineArguments.remove(0);

    return this.startApplication(mainClass);
  }

  private boolean startApplication(@NotNull String mainClass) throws Exception {
    Class<?> main = Class.forName(mainClass);
    Method method = main.getMethod("main", String[].class);

    Collection<String> arguments = new ArrayList<>(this.commandLineArguments);

    this.eventManager.callEvent(new ApplicationPreStartEvent(this, main, arguments));

    try {
      // checking if the application will be launched via the Minecraft LaunchWrapper
      Class.forName("net.minecraft.launchwrapper.Launch");

      // adds a tweak class to the LaunchWrapper which will prevent doubled loading of the CloudNet classes
      arguments.add("--tweakClass");
      arguments.add("de.dytanic.cloudnet.wrapper.tweak.CloudNetTweaker");
    } catch (ClassNotFoundException exception) {
      // the LaunchWrapper is not available, doing nothing
    }

    Thread applicationThread = new Thread(() -> {
      try {
        LOGGER.info(
          "Starting Application-Thread based of " + Wrapper.this.getServiceConfiguration().getProcessConfig()
            .getEnvironment() + "\n");
        method.invoke(null, new Object[]{arguments.toArray(new String[0])});
      } catch (Exception exception) {
        LOGGER.severe("Exception while starting application", exception);
      }
    }, "Application-Thread");
    applicationThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
    applicationThread.start();

    this.eventManager
      .callEvent(new ApplicationPostStartEvent(this, main, applicationThread, ClassLoader.getSystemClassLoader()));
    return true;
  }

  @NotNull
  public IWrapperConfiguration getConfig() {
    return this.config;
  }

  @NotNull
  @Deprecated
  public File getWorkDirectory() {
    return this.workDirectory.toFile();
  }

  @NotNull
  public Path getWorkingDirectoryPath() {
    return this.workDirectory;
  }

  @NotNull
  public List<String> getCommandLineArguments() {
    return this.commandLineArguments;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public INetworkClient getNetworkClient() {
    return this.networkClient;
  }

  @NotNull
  public Thread getMainThread() {
    return this.mainThread;
  }

  @NotNull
  public ServiceInfoSnapshot getLastServiceInfoSnapShot() {
    return this.lastServiceInfoSnapShot;
  }

  @NotNull
  public ServiceInfoSnapshot getCurrentServiceInfoSnapshot() {
    return this.currentServiceInfoSnapshot;
  }

  /**
   * @deprecated use {@link CloudNetDriver#getDatabaseProvider()} instead
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  public IDatabaseProvider getDatabaseProvider() {
    return this.databaseProvider;
  }

  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  public void setDatabaseProvider(@NotNull IDatabaseProvider databaseProvider) {
    Preconditions.checkNotNull(databaseProvider);
    this.databaseProvider = databaseProvider;
  }

  public INetworkChannel getNetworkChannel() {
    return this.networkClient.getFirstChannel();
  }
}
