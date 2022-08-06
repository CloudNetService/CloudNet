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

package eu.cloudnetservice.node.service.defaults;

import com.google.common.collect.ComparisonChain;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.TickLoop;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.event.service.CloudServicePreForceStopEvent;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceFactory;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.ServiceConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.BungeeConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.GlowstoneConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.NukkitConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.VanillaServiceConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.VelocityConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.config.WaterdogPEConfigurationPreparer;
import eu.cloudnetservice.node.service.defaults.factory.JVMServiceFactory;
import eu.cloudnetservice.node.service.defaults.provider.EmptySpecificCloudServiceProvider;
import eu.cloudnetservice.node.service.defaults.provider.RemoteNodeCloudServiceProvider;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultCloudServiceManager implements CloudServiceManager {

  protected static final Path TEMP_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.tempDir.services", "temp/services"));
  protected static final Path PERSISTENT_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.persistable.services.path", "local/services"));
  protected static final ServiceConfigurationPreparer NO_OP_PREPARER = (nodeInstance, cloudService) -> {
  };

  private static final Logger LOGGER = LogManager.logger(CloudServiceManager.class);

  protected final RPCSender sender;
  protected final Collection<String> defaultJvmOptions;
  protected final NodeServerProvider nodeServerProvider;

  protected final Map<UUID, SpecificCloudServiceProvider> knownServices = new ConcurrentHashMap<>();
  protected final Map<String, CloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();
  protected final Map<ServiceEnvironmentType, ServiceConfigurationPreparer> preparers = new ConcurrentHashMap<>();

  public DefaultCloudServiceManager(@NonNull Node nodeInstance, @NonNull Collection<String> defaultJvmOptions) {
    this.defaultJvmOptions = defaultJvmOptions;
    this.nodeServerProvider = nodeInstance.nodeServerProvider();
    // rpc init
    this.sender = nodeInstance.rpcFactory().providerForClass(null, CloudServiceProvider.class);
    nodeInstance.rpcFactory()
      .newHandler(CloudServiceProvider.class, this)
      .registerToDefaultRegistry();
    nodeInstance.rpcFactory()
      .newHandler(SpecificCloudServiceProvider.class, null)
      .registerToDefaultRegistry();
    // register the default factory
    this.addCloudServiceFactory("jvm", new JVMServiceFactory(nodeInstance, nodeInstance.eventManager()));
    // register the default configuration preparers
    this.addServicePreparer(ServiceEnvironmentType.NUKKIT, new NukkitConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.VELOCITY, new VelocityConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.BUNGEECORD, new BungeeConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.GLOWSTONE, new GlowstoneConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.WATERDOG_PE, new WaterdogPEConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.MINECRAFT_SERVER, new VanillaServiceConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.MODDED_MINECRAFT_SERVER, new VanillaServiceConfigurationPreparer());
    // cluster data sync
    nodeInstance.dataSyncRegistry().registerHandler(
      DataSyncHandler.<ServiceInfoSnapshot>builder()
        .key("services")
        .alwaysForce()
        .nameExtractor(Nameable::name)
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
    nodeInstance.mainThread().scheduleTask(() -> {
      for (var service : this.localCloudServices()) {
        // we only need to look at running services
        if (service.lifeCycle() == ServiceLifeCycle.RUNNING) {
          // detect dead services and stop them
          if (service.alive()) {
            service.serviceConsoleLogCache().update();
            LOGGER.fine("Updated service log cache of %s", null, service.serviceId().name());
          } else {
            nodeInstance.eventManager().callEvent(new CloudServicePreForceStopEvent(service));
            service.stop();
            LOGGER.fine("Stopped dead service %s", null, service.serviceId().name());
          }
        }
      }
      return null;
    }, TickLoop.TPS);
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
  @UnmodifiableView
  public @NonNull Map<String, CloudServiceFactory> cloudServiceFactories() {
    return Collections.unmodifiableMap(this.cloudServiceFactories);
  }

  @Override
  public @Nullable CloudServiceFactory cloudServiceFactory(@NonNull String runtime) {
    return this.cloudServiceFactories.get(runtime);
  }

  @Override
  public void addCloudServiceFactory(@NonNull String runtime, @NonNull CloudServiceFactory factory) {
    this.cloudServiceFactories.putIfAbsent(runtime, factory);
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
  public void registerLocalService(@NonNull CloudService service) {
    this.knownServices.putIfAbsent(service.serviceId().uniqueId(), service);
  }

  @Override
  public void unregisterLocalService(@NonNull CloudService service) {
    this.knownServices.remove(service.serviceId().uniqueId());
  }

  @Override
  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot, @UnknownNullability NetworkChannel source) {
    // deleted services were removed on the other node - remove it here too
    if (snapshot.lifeCycle() == ServiceLifeCycle.DELETED) {
      this.knownServices.remove(snapshot.serviceId().uniqueId());
      LOGGER.fine("Deleted cloud service %s after lifecycle change to deleted", null, snapshot.serviceId());
    } else {
      // register the service if the provider is available
      var provider = this.knownServices.get(snapshot.serviceId().uniqueId());
      if (provider == null) {
        this.knownServices.putIfAbsent(
          snapshot.serviceId().uniqueId(),
          this.sender.factory().generateRPCChainBasedApi(
            this.sender,
            "serviceProvider",
            SpecificCloudServiceProvider.class,
            GenerationContext.forClass(RemoteNodeCloudServiceProvider.class).channelSupplier(() -> source).build()
          ).newInstance(new Object[]{snapshot}, new Object[]{snapshot.serviceId().uniqueId()}));
        LOGGER.fine("Registered remote service %s", null, snapshot.serviceId());
      } else if (provider instanceof RemoteNodeCloudServiceProvider remoteProvider) {
        // update the provider if possible - we need only to handle remote node providers as local providers will update
        // the snapshot directly "in" them
        remoteProvider.snapshot(snapshot);
        LOGGER.fine("Updated service snapshot of %s to %s", null, snapshot.serviceId(), snapshot);
      } else if (provider instanceof CloudService localService) {
        // just set the service information locally - no further processing
        localService.updateServiceInfoSnapshot(snapshot);
      }
    }
  }

  @Override
  public @NonNull CloudService createLocalCloudService(@NonNull ServiceConfiguration configuration) {
    // get the cloud service factory for the configuration
    var factory = this.cloudServiceFactory(configuration.runtime());
    if (factory == null) {
      throw new IllegalArgumentException("No service factory for runtime " + configuration.runtime());
    }
    // create the new service using the factory
    return factory.createCloudService(this, configuration);
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
        return nodeServer == null ? null : new Pair<>(service, nodeServer);
      })
      .filter(Objects::nonNull)
      .min((left, right) -> {
        // begin by comparing the heap memory usage
        var chain = ComparisonChain.start().compare(
          left.second().nodeInfoSnapshot().memoryUsagePercentage(),
          right.second().nodeInfoSnapshot().memoryUsagePercentage());
        // only include the cpu usage if both nodes can provide a value
        if (left.second().nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0
          && right.second().nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left.second().nodeInfoSnapshot().processSnapshot().systemCpuUsage(),
            right.second().nodeInfoSnapshot().processSnapshot().systemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
    // check if we found a prepared service
    if (prepared != null) {
      return prepared.first().provider();
    } else {
      // create a new service
      var createResult = Node.instance()
        .cloudServiceFactory()
        .createCloudService(ServiceConfiguration.builder(task).build());
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
