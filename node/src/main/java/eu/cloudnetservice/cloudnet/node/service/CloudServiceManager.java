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

package eu.cloudnetservice.cloudnet.node.service;

import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

public interface CloudServiceManager extends GeneralCloudServiceProvider {

  @NonNull Collection<CloudServiceFactory> cloudServiceFactories();

  @NonNull Optional<CloudServiceFactory> cloudServiceFactory(@NonNull String runtime);

  void addCloudServiceFactory(@NonNull String runtime, @NonNull CloudServiceFactory factory);

  void removeCloudServiceFactory(@NonNull String runtime);

  @NonNull Collection<ServiceConfigurationPreparer> servicePreparers();

  @NonNull Optional<ServiceConfigurationPreparer> servicePreparer(@NonNull ServiceEnvironmentType environmentType);

  void addServicePreparer(@NonNull ServiceEnvironmentType type, @NonNull ServiceConfigurationPreparer preparer);

  void removeServicePreparer(@NonNull ServiceEnvironmentType type);

  @NonNull Path tempDirectory();

  @NonNull Path persistentServicesDirectory();

  void startAllCloudServices();

  void stopAllCloudServices();

  void deleteAllCloudServices();

  int currentUsedHeapMemory();

  int currentReservedMemory();

  @NonNull
  @UnmodifiableView Collection<CloudService> localCloudServices();

  @Nullable CloudService localCloudService(@NonNull String name);

  @Nullable CloudService localCloudService(@NonNull UUID uniqueId);

  @Nullable CloudService localCloudService(@NonNull ServiceInfoSnapshot snapshot);

  @Internal
  void registerLocalService(@NonNull CloudService service);

  @Internal
  void unregisterLocalService(@NonNull CloudService service);

  @Internal
  void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot, @UnknownNullability NetworkChannel source);

  @Internal
  @NonNull CloudService createLocalCloudService(@NonNull ServiceConfiguration serviceConfiguration);

  @Internal
  @NonNull SpecificCloudServiceProvider selectOrCreateService(@NonNull ServiceTask task);
}
