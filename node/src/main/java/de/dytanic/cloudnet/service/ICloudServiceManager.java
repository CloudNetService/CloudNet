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

package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public interface ICloudServiceManager extends GeneralCloudServiceProvider {

  @NotNull
  Collection<ICloudServiceFactory> cloudServiceFactories();

  @NotNull
  Optional<ICloudServiceFactory> cloudServiceFactory(@NotNull String runtime);

  void addCloudServiceFactory(@NotNull String runtime, @NotNull ICloudServiceFactory factory);

  void removeCloudServiceFactory(@NotNull String runtime);

  @NotNull
  Collection<ServiceConfigurationPreparer> servicePreparers();

  @NotNull
  Optional<ServiceConfigurationPreparer> servicePreparer(@NotNull ServiceEnvironmentType environmentType);

  void addServicePreparer(@NotNull ServiceEnvironmentType type, @NotNull ServiceConfigurationPreparer preparer);

  void removeServicePreparer(@NotNull ServiceEnvironmentType type);

  @NotNull
  Path tempDirectory();

  @NotNull
  Path persistentServicesDirectory();

  void startAllCloudServices();

  void stopAllCloudServices();

  void deleteAllCloudServices();

  int currentUsedHeapMemory();

  int currentReservedMemory();

  @NotNull
  @UnmodifiableView Collection<ICloudService> localCloudServices();

  @Nullable
  ICloudService localCloudService(@NotNull String name);

  @Nullable
  ICloudService localCloudService(@NotNull UUID uniqueId);

  @Nullable
  ICloudService localCloudService(@NotNull ServiceInfoSnapshot snapshot);

  @Internal
  void registerLocalService(@NotNull ICloudService service);

  @Internal
  void unregisterLocalService(@NotNull ICloudService service);

  @Internal
  void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot, @UnknownNullability INetworkChannel source);

  @Internal
  @NotNull ICloudService createLocalCloudService(@NotNull ServiceConfiguration serviceConfiguration);

  @Internal
  @NotNull SpecificCloudServiceProvider selectOrCreateService(@NotNull ServiceTask task);
}
