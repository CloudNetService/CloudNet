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

package eu.cloudnetservice.node.service;

import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.node.cluster.NodeServer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface CloudServiceManager extends CloudServiceProvider {

  @NonNull
  @UnmodifiableView Map<String, LocalCloudServiceFactory> cloudServiceFactories();

  @Nullable LocalCloudServiceFactory cloudServiceFactory(@NonNull String runtime);

  void addCloudServiceFactory(@NonNull String runtime, @NonNull LocalCloudServiceFactory factory);

  void addCloudServiceFactory(@NonNull String runtime, @NonNull Class<? extends LocalCloudServiceFactory> factory);

  void removeCloudServiceFactory(@NonNull String runtime);

  @NonNull Collection<ServiceConfigurationPreparer> servicePreparers();

  @NonNull ServiceConfigurationPreparer servicePreparer(@NonNull ServiceEnvironmentType environmentType);

  void addServicePreparer(
    @NonNull ServiceEnvironmentType type,
    @NonNull Class<? extends ServiceConfigurationPreparer> preparer);

  void addServicePreparer(@NonNull ServiceEnvironmentType type, @NonNull ServiceConfigurationPreparer preparer);

  void removeServicePreparer(@NonNull ServiceEnvironmentType type);

  @NonNull Path tempDirectory();

  @NonNull Path persistentServicesDirectory();

  void startAllCloudServices();

  void stopAllCloudServices();

  void deleteAllCloudServices();

  int currentUsedHeapMemory();

  int currentReservedMemory();

  @Nullable NodeServer selectNodeForService(@NonNull ServiceConfiguration configuration);

  @NonNull
  @UnmodifiableView Collection<CloudService> localCloudServices();

  @Nullable CloudService localCloudService(@NonNull String name);

  @Nullable CloudService localCloudService(@NonNull UUID uniqueId);

  @Nullable CloudService localCloudService(@NonNull ServiceInfoSnapshot snapshot);

  @NonNull
  @Unmodifiable Collection<String> defaultJvmOptions();
}
