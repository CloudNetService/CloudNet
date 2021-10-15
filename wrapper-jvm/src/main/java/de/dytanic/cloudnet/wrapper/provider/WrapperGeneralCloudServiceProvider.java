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

package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

  private final RPCSender rpcSender;

  public WrapperGeneralCloudServiceProvider(@NotNull Wrapper wrapper) {
    this.rpcSender = wrapper.getRPCProviderFactory().providerForClass(
      wrapper.getNetworkClient(),
      GeneralCloudServiceProvider.class);
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getSpecificProvider(@NotNull UUID serviceUniqueId) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, serviceUniqueId);
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getSpecificProviderByName(@NotNull String serviceName) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, serviceName);
  }

  @Override
  public @NotNull Collection<UUID> getServicesAsUniqueId() {
    return this.rpcSender.invokeMethod("getServicesAsUniqueId").fireSync();
  }

  @Override
  public @Nullable ServiceInfoSnapshot getCloudServiceByName(@NotNull String name) {
    return this.rpcSender.invokeMethod("getCloudServiceByName", name).fireSync();
  }

  @Override
  public @NotNull Collection<ServiceInfoSnapshot> getCloudServices() {
    return this.rpcSender.invokeMethod("getCloudServices").fireSync();
  }

  @Override
  public @NotNull Collection<ServiceInfoSnapshot> getStartedCloudServices() {
    return this.rpcSender.invokeMethod("getStartedCloudServices").fireSync();
  }

  @Override
  public @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByTask(@NotNull String taskName) {
    return this.rpcSender.invokeMethod("getCloudServicesByTask", taskName).fireSync();
  }

  @Override
  public @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByEnvironment(
    @NotNull ServiceEnvironmentType environment
  ) {
    return this.rpcSender.invokeMethod("getCloudServicesByEnvironment", environment).fireSync();
  }

  @Override
  public @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group) {
    return this.rpcSender.invokeMethod("getCloudServicesByGroup", group).fireSync();
  }

  @Override
  public @Nullable ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId) {
    return this.rpcSender.invokeMethod("getCloudService", uniqueId).fireSync();
  }

  @Override
  public int getServicesCount() {
    return this.rpcSender.invokeMethod("getServicesCount").fireSync();
  }

  @Override
  public int getServicesCountByGroup(@NotNull String group) {
    return this.rpcSender.invokeMethod("getServicesCountByGroup", group).fireSync();
  }

  @Override
  public int getServicesCountByTask(@NotNull String taskName) {
    return this.rpcSender.invokeMethod("getServicesCountByTask", taskName).fireSync();
  }
}
