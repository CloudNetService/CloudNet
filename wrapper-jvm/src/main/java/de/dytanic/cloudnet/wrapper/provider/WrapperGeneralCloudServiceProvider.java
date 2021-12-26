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

import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.network.listener.message.ServiceChannelMessageListener;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

  private final RPCSender rpcSender;
  private final Supplier<NetworkChannel> channelSupplier;

  public WrapperGeneralCloudServiceProvider(@NonNull Wrapper wrapper) {
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      GeneralCloudServiceProvider.class);
    this.channelSupplier = wrapper.networkClient()::firstChannel;

    wrapper.eventManager().registerListener(new ServiceChannelMessageListener(wrapper.eventManager()));
  }

  @Override
  public @NonNull SpecificCloudServiceProvider specificProvider(@NonNull UUID serviceUniqueId) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, this.channelSupplier, serviceUniqueId);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider specificProviderByName(@NonNull String serviceName) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, this.channelSupplier, serviceName);
  }

  @Override
  public @NonNull Collection<UUID> servicesAsUniqueId() {
    return this.rpcSender.invokeMethod("servicesAsUniqueId").fireSync();
  }

  @Override
  public @Nullable ServiceInfoSnapshot serviceByName(@NonNull String name) {
    return this.rpcSender.invokeMethod("serviceByName", name).fireSync();
  }

  @Override
  public @NonNull Collection<ServiceInfoSnapshot> services() {
    return this.rpcSender.invokeMethod("services").fireSync();
  }

  @Override
  public @NonNull Collection<ServiceInfoSnapshot> runningServices() {
    return this.rpcSender.invokeMethod("runningServices").fireSync();
  }

  @Override
  public @NonNull Collection<ServiceInfoSnapshot> servicesByTask(@NonNull String taskName) {
    return this.rpcSender.invokeMethod("servicesByTask", taskName).fireSync();
  }

  @Override
  public @NonNull Collection<ServiceInfoSnapshot> servicesByEnvironment(
    @NonNull ServiceEnvironmentType environment
  ) {
    return this.rpcSender.invokeMethod("servicesByEnvironment", environment).fireSync();
  }

  @Override
  public @NonNull Collection<ServiceInfoSnapshot> servicesByGroup(@NonNull String group) {
    return this.rpcSender.invokeMethod("servicesByGroup", group).fireSync();
  }

  @Override
  public @Nullable ServiceInfoSnapshot service(@NonNull UUID uniqueId) {
    return this.rpcSender.invokeMethod("service", uniqueId).fireSync();
  }

  @Override
  public int serviceCount() {
    return this.rpcSender.invokeMethod("serviceCount").fireSync();
  }

  @Override
  public int serviceCountByGroup(@NonNull String group) {
    return this.rpcSender.invokeMethod("serviceCountByGroup", group).fireSync();
  }

  @Override
  public int serviceCountByTask(@NonNull String taskName) {
    return this.rpcSender.invokeMethod("serviceCountByTask", taskName).fireSync();
  }
}
