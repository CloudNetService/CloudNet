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
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultCloudServiceManager implements ICloudServiceManager {

  protected static final Path TEMP_SERVICE_DIR = Paths.get(
    System.getProperty("cloudnet.tempDir.services", "temp/services"));
  protected static final Path PERSISTENT_SERVICE_DIR = Paths.get(
    System.getProperty("cloudnet.persistable.services.path", "local/services"));

  protected final RPCSender sender;
  protected final IClusterNodeServerProvider clusterNodeServerProvider;

  protected final Map<UUID, SpecificCloudServiceProvider> knownServices = new ConcurrentHashMap<>();
  protected final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();
  protected final Map<ServiceEnvironmentType, ServiceConfigurationPreparer> preparers = new ConcurrentHashMap<>();

  public DefaultCloudServiceManager(@NotNull CloudNet nodeInstance) {
    this.clusterNodeServerProvider = nodeInstance.getClusterNodeServerProvider();
    this.sender = nodeInstance.getRPCProviderFactory().providerForClass(null, GeneralCloudServiceProvider.class);
    // register the default factory
    this.addCloudServiceFactory("jvm", new JVMServiceFactory(nodeInstance, nodeInstance.getEventManager()));
    // register the default configuration preparers
    this.addServicePreparer(ServiceEnvironmentType.NUKKIT, new NukkitConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.VELOCITY, new VelocityConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.BUNGEECORD, new BungeeConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.GLOWSTONE, new GlowstoneConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.WATERDOG_PE, new WaterdogPEConfigurationPreparer());
    this.addServicePreparer(ServiceEnvironmentType.MINECRAFT_SERVER, new VanillaServiceConfigurationPreparer());
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getSpecificProvider(@NotNull UUID serviceUniqueId) {
    return this.knownServices.getOrDefault(serviceUniqueId, EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getSpecificProviderByName(@NotNull String serviceName) {
    return this.knownServices.values().stream()
      .filter(provider -> provider.getServiceInfoSnapshot() != null)
      .filter(provider -> provider.getServiceInfoSnapshot().getServiceId().getName().equals(serviceName))
      .findFirst()
      .orElse(EmptySpecificCloudServiceProvider.INSTANCE);
  }

  @Override
  public @UnmodifiableView @NotNull Collection<UUID> getServicesAsUniqueId() {
    return this.knownServices.values().stream()
      .filter(provider -> provider.getServiceInfoSnapshot() != null)
      .map(provider -> provider.getServiceInfoSnapshot().getServiceId().getUniqueId())
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServices() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getStartedCloudServices() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByTask(@NotNull String taskName) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.getServiceId().getTaskName().equals(taskName))
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByEnvironment(
    @NotNull ServiceEnvironmentType environment
  ) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.getServiceId().getEnvironment() == environment)
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> Arrays.asList(snapshot.getConfiguration().getGroups()).contains(group))
      .collect(Collectors.toList());
  }

  @Override
  public int getServicesCount() {
    return this.knownServices.size();
  }

  @Override
  public int getServicesCountByGroup(@NotNull String group) {
    return this.getCloudServicesByGroup(group).size();
  }

  @Override
  public int getServicesCountByTask(@NotNull String taskName) {
    return this.getCloudServicesByTask(taskName).size();
  }

  @Override
  public @Nullable ServiceInfoSnapshot getCloudServiceByName(@NotNull String name) {
    return this.getSpecificProviderByName(name).getServiceInfoSnapshot();
  }

  @Override
  public @Nullable ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId) {
    return this.getSpecificProvider(uniqueId).getServiceInfoSnapshot();
  }

  @Override
  public @NotNull Collection<ICloudServiceFactory> getCloudServiceFactories() {
    return Collections.unmodifiableCollection(this.cloudServiceFactories.values());
  }

  @Override
  public @NotNull Optional<ICloudServiceFactory> getCloudServiceFactory(@NotNull String runtime) {
    return Optional.ofNullable(this.cloudServiceFactories.get(runtime));
  }

  @Override
  public void addCloudServiceFactory(@NotNull String runtime, @NotNull ICloudServiceFactory factory) {
    this.cloudServiceFactories.putIfAbsent(runtime, factory);
  }

  @Override
  public void removeCloudServiceFactory(@NotNull String runtime) {
    this.cloudServiceFactories.remove(runtime);
  }

  @Override
  public @NotNull Collection<ServiceConfigurationPreparer> getServicePreparers() {
    return Collections.unmodifiableCollection(this.preparers.values());
  }

  @Override
  public @NotNull Optional<ServiceConfigurationPreparer> getServicePreparer(@NotNull ServiceEnvironmentType type) {
    return Optional.ofNullable(this.preparers.get(type));
  }

  @Override
  public void addServicePreparer(
    @NotNull ServiceEnvironmentType type,
    @NotNull ServiceConfigurationPreparer preparer
  ) {
    this.preparers.putIfAbsent(type, preparer);
  }

  @Override
  public void removeServicePreparer(@NotNull ServiceEnvironmentType type) {
    this.preparers.remove(type);
  }

  @Override
  public @NotNull Path getTempDirectoryPath() {
    return TEMP_SERVICE_DIR;
  }

  @Override
  public @NotNull Path getPersistentServicesDirectoryPath() {
    return PERSISTENT_SERVICE_DIR;
  }

  @Override
  public void startAllCloudServices() {
    this.getLocalCloudServices().forEach(SpecificCloudServiceProvider::start);
  }

  @Override
  public void stopAllCloudServices() {
    this.getLocalCloudServices().forEach(SpecificCloudServiceProvider::stop);
  }

  @Override
  public void deleteAllCloudServices() {
    this.getLocalCloudServices().forEach(SpecificCloudServiceProvider::delete);
  }

  @Override
  public @NotNull @UnmodifiableView Collection<ICloudService> getLocalCloudServices() {
    return this.knownServices.values().stream()
      .filter(provider -> provider instanceof ICloudService) // -> ICloudService => local service
      .map(provider -> (ICloudService) provider)
      .collect(Collectors.toList());
  }

  @Override
  public @Nullable ICloudService getLocalCloudService(@NotNull String name) {
    SpecificCloudServiceProvider provider = this.getSpecificProviderByName(name);
    return provider instanceof ICloudService ? (ICloudService) provider : null;
  }

  @Override
  public @Nullable ICloudService getLocalCloudService(@NotNull UUID uniqueId) {
    SpecificCloudServiceProvider provider = this.knownServices.get(uniqueId);
    return provider instanceof ICloudService ? (ICloudService) provider : null;
  }

  @Override
  public @Nullable ICloudService getLocalCloudService(@NotNull ServiceInfoSnapshot snapshot) {
    return this.getLocalCloudService(snapshot.getServiceId().getUniqueId());
  }

  @Override
  public int getCurrentUsedHeapMemory() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .mapToInt(snapshot -> snapshot.getConfiguration().getProcessConfig().getMaxHeapMemorySize())
      .sum();
  }

  @Override
  public int getCurrentReservedMemory() {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .mapToInt(snapshot -> snapshot.getConfiguration().getProcessConfig().getMaxHeapMemorySize())
      .sum();
  }

  @Override
  public void registerLocalService(@NotNull ICloudService service) {
    this.knownServices.putIfAbsent(service.getServiceId().getUniqueId(), service);
  }

  @Override
  public void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot) {
    // deleted services were removed on the other node - remove it here too
    if (snapshot.getLifeCycle() == ServiceLifeCycle.DELETED) {
      this.knownServices.remove(snapshot.getServiceId().getUniqueId());
    } else {
      // register the service if the provider is available
      SpecificCloudServiceProvider provider = this.knownServices.get(snapshot.getServiceId().getUniqueId());
      if (provider == null) {
        this.knownServices.putIfAbsent(
          snapshot.getServiceId().getUniqueId(),
          new RemoteNodeCloudServiceProvider(this, this.sender, snapshot));
      } else if (provider instanceof RemoteNodeCloudServiceProvider) {
        // update the provider if possible - we need only to handle remote node providers as local providers will update
        // the snapshot directly "in" them
        ((RemoteNodeCloudServiceProvider) provider).setSnapshot(snapshot);
      }
    }
  }

  @Override
  public void handleInitialSetServices(@NotNull Collection<ServiceInfoSnapshot> snapshots) {
    // register all the services
    for (ServiceInfoSnapshot snapshot : snapshots) {
      // ignore deleted services
      if (snapshot.getLifeCycle() != ServiceLifeCycle.DELETED) {
        // register a remote provider for that
        this.knownServices.put(
          snapshot.getServiceId().getUniqueId(),
          new RemoteNodeCloudServiceProvider(this, this.sender, snapshot));
      }
    }
  }

  @Override
  public @NotNull ICloudService createLocalCloudService(@NotNull ServiceConfiguration configuration) {
    // get the cloud service factory for the configuration
    ICloudServiceFactory factory = this.cloudServiceFactories.get(configuration.getRuntime());
    if (factory == null) {
      throw new IllegalArgumentException("No service factory for runtime " + configuration.getRuntime());
    }
    // create the new service using the factory
    return factory.createCloudService(this, configuration);
  }

  @Override
  public @NotNull SpecificCloudServiceProvider startService(@NotNull ServiceTask task) {
    // get all services of the given task, map it to its node unique id
    Pair<ServiceInfoSnapshot, IClusterNodeServer> prepared = this.getCloudServicesByTask(task.getName())
      .stream()
      .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.PREPARED)
      .map(service -> {
        // get the node server associated with the node
        IClusterNodeServer nodeServer = this.clusterNodeServerProvider.getNodeServer(
          service.getServiceId().getNodeUniqueId());
        // the server should never be null - just to be sure
        return nodeServer == null ? null : new Pair<>(service, nodeServer);
      })
      .filter(Objects::nonNull)
      .filter(pair -> pair.getSecond().isAvailable())
      .filter(pair -> {
        // filter out all nodes which are not able to start the service
        NetworkClusterNodeInfoSnapshot nodeInfoSnapshot = pair.getSecond().getNodeInfoSnapshot();
        int maxHeapMemory = pair.getFirst().getConfiguration().getProcessConfig().getMaxHeapMemorySize();
        // used + heap_of_service <= max
        return nodeInfoSnapshot.getUsedMemory() + maxHeapMemory <= nodeInfoSnapshot.getMaxMemory();
      })
      .min((left, right) -> {
        // begin by comparing the heap memory usage
        ComparisonChain chain = ComparisonChain.start().compare(
          left.getSecond().getNodeInfoSnapshot().getUsedMemory()
            + left.getFirst().getConfiguration().getProcessConfig().getMaxHeapMemorySize(),
          right.getSecond().getNodeInfoSnapshot().getUsedMemory()
            + right.getFirst().getConfiguration().getProcessConfig().getMaxHeapMemorySize());
        // only include the cpu usage if both nodes can provide a value
        if (left.getSecond().getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage() >= 0
          && right.getSecond().getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left.getSecond().getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage(),
            right.getSecond().getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
    // check if we found a prepared service
    if (prepared != null) {
      // start the service
      SpecificCloudServiceProvider provider = prepared.getFirst().provider();
      provider.start();
      return provider;
    } else {
      // create a new service
      ServiceInfoSnapshot service = CloudNet.getInstance()
        .getCloudServiceFactory()
        .createCloudService(ServiceConfiguration.builder(task).build());
      return service == null ? EmptySpecificCloudServiceProvider.INSTANCE : service.provider();
    }
  }
}
