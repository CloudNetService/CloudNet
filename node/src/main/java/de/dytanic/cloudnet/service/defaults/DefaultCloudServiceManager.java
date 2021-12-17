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

package de.dytanic.cloudnet.service.defaults;

import com.google.common.collect.ComparisonChain;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.CloudNetTick;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.CloudServicePreForceStopEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.BungeeConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.GlowstoneConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.NukkitConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.VanillaServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.VelocityConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.config.WaterdogPEConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.factory.JVMServiceFactory;
import de.dytanic.cloudnet.service.defaults.provider.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.defaults.provider.RemoteNodeCloudServiceProvider;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultCloudServiceManager implements ICloudServiceManager {

  protected static final Path TEMP_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.tempDir.services", "temp/services"));
  protected static final Path PERSISTENT_SERVICE_DIR = Path.of(
    System.getProperty("cloudnet.persistable.services.path", "local/services"));

  private static final Logger LOGGER = LogManager.logger(ICloudServiceManager.class);

  protected final RPCSender sender;
  protected final IClusterNodeServerProvider clusterNodeServerProvider;

  protected final Map<UUID, SpecificCloudServiceProvider> knownServices = new ConcurrentHashMap<>();
  protected final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();
  protected final Map<ServiceEnvironmentType, ServiceConfigurationPreparer> preparers = new ConcurrentHashMap<>();

  public DefaultCloudServiceManager(@NonNull CloudNet nodeInstance) {
    this.clusterNodeServerProvider = nodeInstance.getClusterNodeServerProvider();
    // rpc init
    this.sender = nodeInstance.rpcProviderFactory().providerForClass(null, GeneralCloudServiceProvider.class);
    nodeInstance.rpcProviderFactory()
      .newHandler(GeneralCloudServiceProvider.class, this)
      .registerToDefaultRegistry();
    nodeInstance.rpcProviderFactory()
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
    // cluster data sync
    nodeInstance.dataSyncRegistry().registerHandler(
      DataSyncHandler.<ServiceInfoSnapshot>builder()
        .key("services")
        .alwaysForce()
        .nameExtractor(INameable::name)
        .dataCollector(this::services)
        .convertObject(ServiceInfoSnapshot.class)
        .writer(ser -> {
          // ugly hack to get the channel of the service's associated node
          var node = this.clusterNodeServerProvider.nodeServer(ser.serviceId().nodeUniqueId());
          if (node != null && node.available()) {
            this.handleServiceUpdate(ser, node.channel());
          }
        })
        .currentGetter(group -> this.specificProviderByName(group.name()).serviceInfo())
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
    }, CloudNetTick.TPS);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider specificProvider(@NonNull UUID serviceUniqueId) {
    return this.knownServices.getOrDefault(serviceUniqueId, EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider specificProviderByName(@NonNull String serviceName) {
    return this.knownServices.values().stream()
      .filter(provider -> provider.serviceInfo() != null)
      .filter(provider -> provider.serviceInfo().serviceId().name().equals(serviceName))
      .findFirst()
      .orElse(EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @UnmodifiableView @NonNull Collection<UUID> servicesAsUniqueId() {
    return this.knownServices.values().stream()
      .filter(provider -> provider.serviceInfo() != null)
      .map(provider -> provider.serviceInfo().serviceId().uniqueId())
      .toList();
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
  public @UnmodifiableView @NonNull Collection<ServiceInfoSnapshot> servicesByEnvironment(
    @NonNull ServiceEnvironmentType environment
  ) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.serviceId().environment().equals(environment))
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
    return this.specificProviderByName(name).serviceInfo();
  }

  @Override
  public @Nullable ServiceInfoSnapshot service(@NonNull UUID uniqueId) {
    return this.specificProvider(uniqueId).serviceInfo();
  }

  @Override
  public @NonNull Collection<ICloudServiceFactory> cloudServiceFactories() {
    return Collections.unmodifiableCollection(this.cloudServiceFactories.values());
  }

  @Override
  public @NonNull Optional<ICloudServiceFactory> cloudServiceFactory(@NonNull String runtime) {
    return Optional.ofNullable(this.cloudServiceFactories.get(runtime));
  }

  @Override
  public void addCloudServiceFactory(@NonNull String runtime, @NonNull ICloudServiceFactory factory) {
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
  public @NonNull Optional<ServiceConfigurationPreparer> servicePreparer(@NonNull ServiceEnvironmentType type) {
    return Optional.ofNullable(this.preparers.get(type));
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
  public @NonNull @UnmodifiableView Collection<ICloudService> localCloudServices() {
    return this.knownServices.values().stream()
      .filter(provider -> provider instanceof ICloudService) // -> ICloudService => local service
      .map(provider -> (ICloudService) provider)
      .toList();
  }

  @Override
  public @Nullable ICloudService localCloudService(@NonNull String name) {
    var provider = this.specificProviderByName(name);
    return provider instanceof ICloudService ? (ICloudService) provider : null;
  }

  @Override
  public @Nullable ICloudService localCloudService(@NonNull UUID uniqueId) {
    var provider = this.knownServices.get(uniqueId);
    return provider instanceof ICloudService ? (ICloudService) provider : null;
  }

  @Override
  public @Nullable ICloudService localCloudService(@NonNull ServiceInfoSnapshot snapshot) {
    return this.localCloudService(snapshot.serviceId().uniqueId());
  }

  @Override
  public int currentUsedHeapMemory() {
    return this.localCloudServices().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.lifeCycle() == ServiceLifeCycle.RUNNING)
      .mapToInt(snapshot -> snapshot.configuration().processConfig().maxHeapMemorySize())
      .sum();
  }

  @Override
  public int currentReservedMemory() {
    return this.localCloudServices().stream()
      .map(SpecificCloudServiceProvider::serviceInfo)
      .filter(Objects::nonNull)
      .mapToInt(snapshot -> snapshot.configuration().processConfig().maxHeapMemorySize())
      .sum();
  }

  @Override
  public void registerLocalService(@NonNull ICloudService service) {
    this.knownServices.putIfAbsent(service.serviceId().uniqueId(), service);
  }

  @Override
  public void unregisterLocalService(@NonNull ICloudService service) {
    this.knownServices.remove(service.serviceId().uniqueId());
  }

  @Override
  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot, @UnknownNullability INetworkChannel source) {
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
          new RemoteNodeCloudServiceProvider(this, this.sender, () -> source, snapshot));
        LOGGER.fine("Registered remote service %s", null, snapshot.serviceId());
      } else if (provider instanceof RemoteNodeCloudServiceProvider) {
        // update the provider if possible - we need only to handle remote node providers as local providers will update
        // the snapshot directly "in" them
        ((RemoteNodeCloudServiceProvider) provider).snapshot(snapshot);
        LOGGER.fine("Updated service snapshot of %s to %s", null, snapshot.serviceId(), snapshot);
      } else if (provider instanceof ICloudService) {
        // just set the service information locally - no further processing
        ((ICloudService) provider).updateServiceInfoSnapshot(snapshot);
      }
    }
  }

  @Override
  public @NonNull ICloudService createLocalCloudService(@NonNull ServiceConfiguration configuration) {
    // get the cloud service factory for the configuration
    var factory = this.cloudServiceFactories.get(configuration.runtime());
    if (factory == null) {
      throw new IllegalArgumentException("No service factory for runtime " + configuration.runtime());
    }
    // create the new service using the factory
    return factory.createCloudService(this, configuration);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider selectOrCreateService(@NonNull ServiceTask task) {
    // get all services of the given task, map it to its node unique id
    var prepared = this.servicesByTask(task.name())
      .stream()
      .filter(taskService -> taskService.lifeCycle() == ServiceLifeCycle.PREPARED)
      .map(service -> {
        // get the node server associated with the node
        var nodeServer = this.clusterNodeServerProvider.nodeServer(
          service.serviceId().nodeUniqueId());
        // the server should never be null - just to be sure
        return nodeServer == null ? null : new Pair<>(service, nodeServer);
      })
      .filter(Objects::nonNull)
      .filter(pair -> pair.second().available())
      .filter(pair -> {
        // filter out all nodes which are not able to start the service
        var nodeInfoSnapshot = pair.second().nodeInfoSnapshot();
        var maxHeapMemory = pair.first().configuration().processConfig().maxHeapMemorySize();
        // used + heap_of_service <= max
        return nodeInfoSnapshot.usedMemory() + maxHeapMemory <= nodeInfoSnapshot.maxMemory();
      })
      .min((left, right) -> {
        // begin by comparing the heap memory usage
        var chain = ComparisonChain.start().compare(
          left.second().nodeInfoSnapshot().usedMemory()
            + left.first().configuration().processConfig().maxHeapMemorySize(),
          right.second().nodeInfoSnapshot().usedMemory()
            + right.first().configuration().processConfig().maxHeapMemorySize());
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
      var service = CloudNet.instance()
        .cloudServiceFactory()
        .createCloudService(ServiceConfiguration.builder(task).build());
      return service == null ? EmptySpecificCloudServiceProvider.INSTANCE : service.provider();
    }
  }
}
