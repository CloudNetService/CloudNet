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

import aerogel.Inject;
import aerogel.Singleton;
import aerogel.auto.Provides;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.provider.service.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
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

@Singleton
@Provides({ICloudServiceManager.class, GeneralCloudServiceProvider.class})
public class DefaultCloudServiceManager implements ICloudServiceManager {

  protected static final Path TEMP_SERVICE_DIR = Paths.get(
    System.getProperty("cloudnet.tempDir.services", "temp/services"));
  protected static final Path PERSISTENT_SERVICE_DIR = Paths.get(
    System.getProperty("cloudnet.persistable.services.path", "local/services"));

  protected final RPCSender sender;

  protected final Map<UUID, SpecificCloudServiceProvider> knownServices = new ConcurrentHashMap<>();
  protected final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();

  @Inject
  public DefaultCloudServiceManager(@NotNull RPCProviderFactory factory) {
    // @todo: make the component actually nullable
    this.sender = factory.providerForClass(null, GeneralCloudServiceProvider.class);
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
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServices(@NotNull String taskName) {
    return this.knownServices.values().stream()
      .map(SpecificCloudServiceProvider::getServiceInfoSnapshot)
      .filter(Objects::nonNull)
      .filter(snapshot -> snapshot.getServiceId().getTaskName().equals(taskName))
      .collect(Collectors.toList());
  }

  @Override
  public @UnmodifiableView @NotNull Collection<ServiceInfoSnapshot> getCloudServices(
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
    return this.getCloudServices(taskName).size();
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
      if (provider == null && snapshot.getLifeCycle() == ServiceLifeCycle.PREPARED) {
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
}
