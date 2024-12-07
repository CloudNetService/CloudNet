/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.service.defaults;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ComparisonChain;
import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.event.service.CloudServicePreForceStopEvent;
import eu.cloudnetservice.node.impl.cluster.sync.DefaultDataSyncHandler;
import eu.cloudnetservice.node.impl.service.InternalCloudService;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import eu.cloudnetservice.node.impl.service.defaults.config.BungeeConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.config.LimboLoohpServiceConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.config.NukkitConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.config.VanillaServiceConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.config.VelocityConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.config.WaterdogPEConfigurationPreparer;
import eu.cloudnetservice.node.impl.service.defaults.factory.JVMLocalCloudServiceFactory;
import eu.cloudnetservice.node.impl.service.defaults.provider.EmptySpecificCloudServiceProvider;
import eu.cloudnetservice.node.impl.service.defaults.provider.RemoteNodeCloudServiceProvider;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.LocalCloudServiceFactory;
import eu.cloudnetservice.node.service.ServiceConfigurationPreparer;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides({CloudServiceManager.class, CloudServiceProvider.class, InternalCloudServiceManager.class})
public class DefaultCloudServiceManager implements InternalCloudServiceManager {

  protected static final Path TEMP_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.tempDir.services", "temp/services"));
  protected static final Path PERSISTENT_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.persistable.services.path", "local/services"));
  protected static final ServiceConfigurationPreparer NO_OP_PREPARER = (_) -> {
  };

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudServiceManager.class);

  private static final ClassDesc CD_UUID = ClassDesc.of(UUID.class.getName());
  private static final ClassDesc CD_SPECIFIC_PROVIDER = ClassDesc.of(SpecificCloudServiceProvider.class.getName());
  private static final MethodTypeDesc MTD_SERVICE_PROVIDER = MethodTypeDesc.of(CD_SPECIFIC_PROVIDER, CD_UUID);

  protected final RPCSender sender;
  protected final Collection<String> defaultJvmOptions;
  protected final NodeServerProvider nodeServerProvider;
  protected final CloudServiceFactory cloudServiceFactory;
  protected final RPCImplementationBuilder.InstanceAllocator<? extends SpecificCloudServiceProvider> specificProviderAllocator;

  protected final Map<UUID, SpecificCloudServiceProvider> knownServices = new ConcurrentHashMap<>();
  protected final Cache<UUID, InternalCloudService> localUnacceptedServices = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(1))
    .build();

  protected final Map<String, LocalCloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();
  protected final Map<ServiceEnvironmentType, ServiceConfigurationPreparer> preparers = new ConcurrentHashMap<>();

  @Inject
  public DefaultCloudServiceManager(
    @NonNull DefaultTickLoop mainThread,
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceFactory cloudServiceFactory,
    @NonNull @jakarta.inject.Named("consoleArgs") List<String> args
  ) {
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceFactory = cloudServiceFactory;
    this.defaultJvmOptions = Arrays.asList(args.removeFirst().split(";;"));

    // init rpc
    this.sender = rpcFactory.newRPCSenderBuilder(CloudServiceProvider.class).targetChannel(() -> null).build();
    var cloudServiceProviderHandler = rpcFactory.newRPCHandlerBuilder(CloudServiceProvider.class)
      .targetInstance(this)
      .build();
    handlerRegistry.registerHandler(cloudServiceProviderHandler);

    var specificProviderHandler = rpcFactory.newRPCHandlerBuilder(SpecificCloudServiceProvider.class).build();
    handlerRegistry.registerHandler(specificProviderHandler);

    this.specificProviderAllocator = rpcFactory.newRPCBasedImplementationBuilder(RemoteNodeCloudServiceProvider.class)
      .superclass(SpecificCloudServiceProvider.class)
      .targetChannel(() -> null)
      .generateImplementation();

    // register the default configuration preparers
    this.addServicePreparer(ServiceEnvironmentType.NUKKIT, NukkitConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.VELOCITY, VelocityConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.BUNGEECORD, BungeeConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.LIMBO_LOOHP, LimboLoohpServiceConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.WATERDOG_PE, WaterdogPEConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.MINECRAFT_SERVER, VanillaServiceConfigurationPreparer.class);
    this.addServicePreparer(ServiceEnvironmentType.MODDED_MINECRAFT_SERVER, VanillaServiceConfigurationPreparer.class);
    // cluster data sync
    dataSyncRegistry.registerHandler(
      DefaultDataSyncHandler.<ServiceInfoSnapshot>builder()
        .key("services")
        .alwaysForce()
        .nameExtractor(Named::name)
        .dataCollector(this::services)
        .convertObject(ServiceInfoSnapshot.class)
        .writer(ser -> {
          // ugly hack to get the channel of the service's associated node
          var node = this.nodeServerProvider.node(ser.serviceId().nodeUniqueId());
          if (node != null && node.available()) {
            this.handleServiceUpdate(ser, node.channel());
          }
        })
        .currentGetter(group -> this.serviceProviderByName(group.name()).serviceInfo())
        .build());
    // schedule the updating of the local service log cache
    mainThread.scheduleTask(() -> {
      for (var service : this.localCloudServices()) {
        // we only need to look at running services
        if (service.lifeCycle() == ServiceLifeCycle.RUNNING) {
          // detect dead services and stop them
          if (service.alive()) {
            service.serviceConsoleLogCache().update();
            LOGGER.trace("Updated service log cache of {}", service.serviceId().name());
          } else {
            eventManager.callEvent(new CloudServicePreForceStopEvent(service));
            service.stop();
            LOGGER.trace("Stopped dead service {}", service.serviceId().name());
          }
        }
      }
      return null;
    }, Duration.ofMillis(DefaultTickLoop.MILLIS_BETWEEN_TICKS));
  }

  @PostConstruct
  private void registerDefaultServiceFactory() {
    this.addCloudServiceFactory("jvm", JVMLocalCloudServiceFactory.class);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProvider(@NonNull UUID serviceUniqueId) {
    return this.knownServices.getOrDefault(serviceUniqueId, EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProviderByName(@NonNull String serviceName) {
    return this.knownServices.values().stream()
      .filter(provider -> {
        var serviceInfo = provider.serviceInfo();
        return serviceInfo != null && serviceInfo.serviceId().name().equals(serviceName);
      })
      .findFirst()
      .orElse(EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> services() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> runningServices() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.lifeCycle() == ServiceLifeCycle.RUNNING)
      .toList();
  }

  @Override
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> servicesByTask(@NonNull String taskName) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.serviceId().taskName().equals(taskName))
      .toList();
  }

  @Override
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> servicesByEnvironment(@NonNull String environment) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.serviceId().environmentName().equals(environment))
      .toList();
  }

  @Override
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> servicesByGroup(@NonNull String group) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.configuration().groups().contains(group))
      .toList();
  }

  @Override
  public int serviceCount() {
    return this.knownServices.size();
  }

  @Override
  public int serviceCountByGroup(@NonNull String group) {
    return this.servicesByGroup(group).size();
  }

  @Override
  public int serviceCountByTask(@NonNull String taskName) {
    return this.servicesByTask(taskName).size();
  }

  @Override
  public @Nullable ServiceInfoSnapshot serviceByName(@NonNull String name) {
    return this.serviceProviderByName(name).serviceInfo();
  }

  @Override
  public @Nullable ServiceInfoSnapshot service(@NonNull UUID uniqueId) {
    return this.serviceProvider(uniqueId).serviceInfo();
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceInfoSnapshot>> servicesAsync() {
    return TaskUtil.supplyAsync(this::services);
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceInfoSnapshot>> runningServicesAsync() {
    return TaskUtil.supplyAsync(this::runningServices);
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByTaskAsync(@NonNull String taskName) {
    return TaskUtil.supplyAsync(() -> this.servicesByTask(taskName));
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByEnvironmentAsync(
    @NonNull String environment
  ) {
    return TaskUtil.supplyAsync(() -> this.servicesByEnvironment(environment));
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByGroupAsync(@NonNull String group) {
    return TaskUtil.supplyAsync(() -> this.servicesByGroup(group));
  }

  @Override
  public @NonNull CompletableFuture<Integer> serviceCountAsync() {
    return TaskUtil.supplyAsync(this::serviceCount);
  }

  @Override
  public @NonNull CompletableFuture<Integer> serviceCountByGroupAsync(@NonNull String group) {
    return TaskUtil.supplyAsync(() -> this.serviceCountByGroup(group));
  }

  @Override
  public @NonNull CompletableFuture<Integer> serviceCountByTaskAsync(@NonNull String taskName) {
    return TaskUtil.supplyAsync(() -> this.serviceCountByTask(taskName));
  }

  @Override
  public @NonNull CompletableFuture<ServiceInfoSnapshot> serviceByNameAsync(@NonNull String name) {
    return TaskUtil.supplyAsync(() -> this.serviceByName(name));
  }

  @Override
  public @NonNull CompletableFuture<ServiceInfoSnapshot> serviceAsync(@NonNull UUID uniqueId) {
    return TaskUtil.supplyAsync(() -> this.service(uniqueId));
  }

  @Override
  @UnmodifiableView
  public @NonNull Map<String, LocalCloudServiceFactory> cloudServiceFactories() {
    return Collections.unmodifiableMap(this.cloudServiceFactories);
  }

  @Override
  public @Nullable LocalCloudServiceFactory cloudServiceFactory(@NonNull String runtime) {
    return this.cloudServiceFactories.get(runtime);
  }

  @Override
  public void addCloudServiceFactory(@NonNull String runtime, @NonNull LocalCloudServiceFactory factory) {
    this.cloudServiceFactories.putIfAbsent(runtime, factory);
  }

  @Override
  public void addCloudServiceFactory(
    @NonNull String runtime,
    @NonNull Class<? extends LocalCloudServiceFactory> factory
  ) {
    var injectionLayer = InjectionLayer.findLayerOf(factory);
    this.addCloudServiceFactory(runtime, injectionLayer.instance(factory));
  }

  @Override
  public void removeCloudServiceFactory(@NonNull String runtime) {
    this.cloudServiceFactories.remove(runtime);
  }

  @Override
  public @NonNull Collection<ServiceConfigurationPreparer> servicePreparers() {
    return Collections.unmodifiableCollection(this.preparers.values());
  }

  @Override
  public @NonNull ServiceConfigurationPreparer servicePreparer(@NonNull ServiceEnvironmentType type) {
    return this.preparers.getOrDefault(type, NO_OP_PREPARER);
  }

  @Override
  public void addServicePreparer(
    @NonNull ServiceEnvironmentType type,
    @NonNull Class<? extends ServiceConfigurationPreparer> preparer
  ) {
    var injectionLayer = InjectionLayer.findLayerOf(preparer);
    this.addServicePreparer(type, injectionLayer.instance(preparer));
  }

  @Override
  public void addServicePreparer(
    @NonNull ServiceEnvironmentType type,
    @NonNull ServiceConfigurationPreparer preparer
  ) {
    this.preparers.putIfAbsent(type, preparer);
  }

  @Override
  public void removeServicePreparer(@NonNull ServiceEnvironmentType type) {
    this.preparers.remove(type);
  }

  @Override
  public @NonNull Path tempDirectory() {
    return TEMP_SERVICE_DIR;
  }

  @Override
  public @NonNull Path persistentServicesDirectory() {
    return PERSISTENT_SERVICE_DIR;
  }

  @Override
  public void startAllCloudServices() {
    this.localCloudServices().forEach(SpecificCloudServiceProvider::start);
  }

  @Override
  public void stopAllCloudServices() {
    this.localCloudServices().forEach(SpecificCloudServiceProvider::stop);
  }

  @Override
  public void deleteAllCloudServices() {
    this.localCloudServices().forEach(SpecificCloudServiceProvider::delete);
  }

  @Override
  public @NonNull @UnmodifiableView Collection<CloudService> localCloudServices() {
    return this.knownServices.values().stream()
      .filter(provider -> provider instanceof CloudService) // -> CloudService => local service
      .map(provider -> (CloudService) provider)
      .toList();
  }

  @Override
  public @Nullable CloudService localCloudService(@NonNull String name) {
    return this.serviceProviderByName(name) instanceof CloudService service ? service : null;
  }

  @Override
  public @Nullable CloudService localCloudService(@NonNull UUID uniqueId) {
    return this.knownServices.get(uniqueId) instanceof CloudService service ? service : null;
  }

  @Override
  public @Nullable CloudService localCloudService(@NonNull ServiceInfoSnapshot snapshot) {
    return this.localCloudService(snapshot.serviceId().uniqueId());
  }

  @Override
  public @NonNull @Unmodifiable Collection<String> defaultJvmOptions() {
    return this.defaultJvmOptions;
  }

  @Override
  public int currentUsedHeapMemory() {
    return this.localCloudServices().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(snapshot -> snapshot.lifeCycle() == ServiceLifeCycle.RUNNING)
      .mapToInt(snapshot -> snapshot.configuration().processConfig().maxHeapMemorySize())
      .sum();
  }

  @Override
  public int currentReservedMemory() {
    return this.localCloudServices().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .mapToInt(snapshot -> snapshot.configuration().processConfig().maxHeapMemorySize())
      .sum();
  }

  @Override
  public @Nullable NodeServer selectNodeForService(@NonNull ServiceConfiguration configuration) {
    // check if the node is already specified
    if (configuration.serviceId().nodeUniqueId() != null) {
      // check for a cluster node server
      var server = this.nodeServerProvider.node(configuration.serviceId().nodeUniqueId());
      if (server != null) {
        // the requested node is a cluster node, check if that node is still accepting services
        return !server.available() || server.nodeInfoSnapshot().draining() ? null : server;
      }
      // no node server with the given name which can start services found
      return null;
    }

    // find the best node server
    return this.nodeServerProvider.nodeServers().stream()
      .filter(NodeServer::available)
      .filter(nodeServer -> !nodeServer.nodeInfoSnapshot().draining())
      .filter(server -> {
        var allowedNodes = configuration.serviceId().allowedNodes();
        return allowedNodes.isEmpty() || allowedNodes.contains(server.info().uniqueId());
      })
      .min((left, right) -> {
        // calculate the reserved memory amount based on the cached service information on this node
        // this is the better way to do this, as newly created services on other nodes will get cached instantly, rather
        // than us needing to wait for the updated node info to be sent by the associated node. In normal scenarios
        // that is not a big problem, however when many start requests are coming in, that can lead to one node picking
        // up a lot of services until (only a few ms later) the updated snapshot is present.
        var leftReservedMemory = this.calculateReservedMemoryPercentage(left);
        var rightReservedMemory = this.calculateReservedMemoryPercentage(right);

        // we elevate the used heap memory percentage over the cpu usage, as it's varying much more
        var chain = ComparisonChain.start().compare(leftReservedMemory, rightReservedMemory);
        // only include the cpu usage if both nodes can provide a value
        if (left.nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0
          && right.nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left.nodeInfoSnapshot().processSnapshot().systemCpuUsage(),
            right.nodeInfoSnapshot().processSnapshot().systemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
  }

  @Override
  public void registerLocalService(@NonNull InternalCloudService service) {
    this.knownServices.putIfAbsent(service.serviceId().uniqueId(), service);
  }

  @Override
  public void unregisterLocalService(@NonNull CloudService service) {
    this.knownServices.remove(service.serviceId().uniqueId());
  }

  @Override
  public void registerUnacceptedService(@NonNull InternalCloudService service) {
    this.localUnacceptedServices.put(service.serviceId().uniqueId(), service);
  }

  @Override
  public @Nullable InternalCloudService takeUnacceptedService(@NonNull UUID serviceUniqueId) {
    // this is the correct way to invalidate & get the value associated with the id in the cache
    // see https://stackoverflow.com/a/67994912/13008679
    return this.localUnacceptedServices.asMap().remove(serviceUniqueId);
  }

  @Override
  public void forceRemoveRegisteredService(@NonNull UUID uniqueId) {
    this.knownServices.remove(uniqueId);
  }

  @Override
  public @Nullable SpecificCloudServiceProvider registerService(
    @NonNull ServiceInfoSnapshot snapshot,
    @NonNull NetworkChannel source
  ) {
    // check if the service provider is already registered, return null to indicate that we didn't register the service
    var serviceUniqueId = snapshot.serviceId().uniqueId();
    if (this.knownServices.containsKey(serviceUniqueId)) {
      return null;
    }

    // build the service provider for the newly added service
    var baseRPC = this.sender.invokeMethod("serviceProvider", MTD_SERVICE_PROVIDER, serviceUniqueId);
    var serviceProvider = this.specificProviderAllocator
      .withBaseRPC(baseRPC)
      .withTargetChannel(() -> source)
      .withAdditionalConstructorParameters(snapshot)
      .allocate();

    // register the service and return the new provider, unless some other thread registered the service
    var knownProvider = this.knownServices.putIfAbsent(serviceUniqueId, serviceProvider);
    return knownProvider == null ? serviceProvider : null;
  }

  @Override
  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot, @Nullable NetworkChannel source) {
    // deleted services were removed on the other node - remove it here too
    if (snapshot.lifeCycle() == ServiceLifeCycle.DELETED) {
      this.knownServices.remove(snapshot.serviceId().uniqueId());
      LOGGER.debug("Deleted cloud service {} after lifecycle change to deleted", snapshot.serviceId());
    } else {
      // register the service if the provider is available
      var provider = this.knownServices.get(snapshot.serviceId().uniqueId());
      if (provider == null) {
        // this is the only point where the channel has to be present
        Objects.requireNonNull(source, "Node Network Channel has to be present to register service");
        this.registerService(snapshot, source);
        LOGGER.debug("Registered remote service {}", snapshot.serviceId());
      } else if (provider instanceof RemoteNodeCloudServiceProvider remoteProvider) {
        // update the provider if possible - we need only to handle remote node providers as local providers will update
        // the snapshot directly "in" them
        remoteProvider.snapshot(snapshot);
        LOGGER.debug("Updated service snapshot of {} to {}", snapshot.serviceId(), snapshot);
      } else if (provider instanceof InternalCloudService localService) {
        // just set the service information locally - no further processing
        localService.updateServiceInfoSnapshot(snapshot);
      }
    }
  }

  @Override
  public @NonNull InternalCloudService createLocalCloudService(@NonNull ServiceConfiguration configuration) {
    // get the cloud service factory for the configuration
    var factory = this.cloudServiceFactory(configuration.runtime());
    if (factory == null) {
      throw new IllegalArgumentException("No service factory for runtime " + configuration.runtime());
    }
    // create the new service using the factory
    return (InternalCloudService) factory.createCloudService(this, configuration);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider selectOrCreateService(@NonNull ServiceTask task) {
    // filter out all nodes which are able to start a service of the given task
    var nodes = this.nodeServerProvider.nodeServers().stream()
      .filter(NodeServer::available)
      .filter(nodeServer -> !nodeServer.nodeInfoSnapshot().draining())
      .filter(server -> {
        var allowedNodes = task.associatedNodes();
        return allowedNodes.isEmpty() || allowedNodes.contains(server.info().uniqueId());
      })
      .filter(server -> {
        var snapshot = server.nodeInfoSnapshot();
        return snapshot.usedMemory() + task.processConfiguration().maxHeapMemorySize() <= snapshot.maxMemory();
      })
      .collect(Collectors.toMap(NodeServer::name, Function.identity()));
    // if there are no nodes which can pick up the service then do nothing
    if (nodes.isEmpty()) {
      return EmptySpecificCloudServiceProvider.INSTANCE;
    }

    // get all services of the given task, map it to its node unique id
    var prepared = this.servicesByTask(task.name())
      .stream()
      .filter(taskService -> taskService.lifeCycle() == ServiceLifeCycle.PREPARED)
      .map(service -> {
        // get the node server associated with the node, if the server is null it has not enough memory to start a service
        var nodeServer = nodes.get(service.serviceId().nodeUniqueId());
        return nodeServer == null ? null : new Tuple2<>(service, nodeServer);
      })
      .filter(Objects::nonNull)
      .min((left, right) -> {
        // begin by comparing the heap memory usage
        var chain = ComparisonChain.start().compare(
          left._2().nodeInfoSnapshot().memoryUsagePercentage(),
          right._2().nodeInfoSnapshot().memoryUsagePercentage());
        // only include the cpu usage if both nodes can provide a value
        if (left._2().nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0
          && right._2().nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left._2().nodeInfoSnapshot().processSnapshot().systemCpuUsage(),
            right._2().nodeInfoSnapshot().processSnapshot().systemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
    // check if we found a prepared service
    if (prepared != null) {
      return prepared._1().provider();
    } else {
      // create a new service
      var createResult = this.cloudServiceFactory.createCloudService(ServiceConfiguration.builder(task).build());
      return createResult.state() != ServiceCreateResult.State.CREATED
        ? EmptySpecificCloudServiceProvider.INSTANCE
        : createResult.serviceInfo().provider();
    }
  }

  protected int calculateReservedMemoryPercentage(@NonNull NodeServer server) {
    // get the reserved memory on the given node based on the services which are running on it and sum it up
    var reservedMemory = this.services().stream()
      .filter(info -> info.serviceId().nodeUniqueId().equals(server.name()))
      .mapToInt(info -> info.configuration().processConfig().maxHeapMemorySize())
      .sum();
    // convert to a percentage
    return (reservedMemory * 100) / server.nodeInfoSnapshot().maxMemory();
  }
}
