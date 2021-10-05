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

import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public interface ICloudServiceManager extends GeneralCloudServiceProvider {

  @NotNull
  Collection<ICloudServiceFactory> getCloudServiceFactories();

  @NotNull
  Optional<ICloudServiceFactory> getCloudServiceFactory(@NotNull String runtime);

  void addCloudServiceFactory(@NotNull String runtime, @NotNull ICloudServiceFactory factory);

  void removeCloudServiceFactory(@NotNull String runtime);

  @NotNull
  Path getTempDirectoryPath();

  @NotNull
  Path getPersistentServicesDirectoryPath();

  void startAllCloudServices();

  void stopAllCloudServices();

  void deleteAllCloudServices();

  @NotNull
  @UnmodifiableView Collection<ICloudService> getLocalCloudServices();

  int getCurrentUsedHeapMemory();

  int getCurrentReservedMemory();

  @Internal
  void registerLocalService(@NotNull ICloudService service);

  @Internal
  void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot);
}
