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

package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

  private final RPCSender rpcSender;

  public WrapperGeneralCloudServiceProvider(Wrapper wrapper) {
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), GeneralCloudServiceProvider.class);
  }

  @Override
  public Collection<UUID> getServicesAsUniqueId() {
    return this.rpcSender.invokeMethod("getServicesAsUniqueId").fireSync();
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudServiceByName(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.rpcSender.invokeMethod("getCloudServiceByName", name).fireSync();
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices() {
    return this.rpcSender.invokeMethod("getCloudServices").fireSync();
  }

  @Override
  public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
    return this.rpcSender.invokeMethod("getStartedCloudServices").fireSync();
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);
    return this.rpcSender.invokeMethod("getCloudServices", taskName).fireSync();
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull ServiceEnvironmentType environment) {
    Preconditions.checkNotNull(environment);
    return this.rpcSender.invokeMethod("getCloudServices", environment).fireSync();
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);
    return this.rpcSender.invokeMethod("getCloudServicesByGroup", group).fireSync();
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);
    return this.rpcSender.invokeMethod("getCloudService", uniqueId).fireSync();
  }

  @Override
  public int getServicesCount() {
    return this.rpcSender.invokeMethod("getServicesCount").fireSync();
  }

  @Override
  public int getServicesCountByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);
    return this.rpcSender.invokeMethod("getServicesCountByGroup", group).fireSync();
  }

  @Override
  public int getServicesCountByTask(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);
    return this.rpcSender.invokeMethod("getServicesCountByTask", taskName).fireSync();
  }
}
