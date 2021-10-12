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

package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteCloudServiceFactory implements CloudServiceFactory {

  private final RPCSender rpcSender;
  private final Supplier<INetworkChannel> channelSupplier;

  public RemoteCloudServiceFactory(
    @NotNull Supplier<INetworkChannel> channelSupplier,
    @NotNull INetworkComponent defaultComponent,
    @NotNull RPCProviderFactory factory
  ) {
    this.channelSupplier = channelSupplier;
    this.rpcSender = factory.providerForClass(defaultComponent, CloudServiceFactory.class);
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration config) {
    return this.rpcSender.invokeMethod("createCloudService", config).fireSync(this.channelSupplier.get());
  }
}
