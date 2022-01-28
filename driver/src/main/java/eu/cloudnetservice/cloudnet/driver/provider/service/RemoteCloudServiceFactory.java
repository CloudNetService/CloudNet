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

package eu.cloudnetservice.cloudnet.driver.provider.service;

import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.NetworkComponent;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class RemoteCloudServiceFactory implements CloudServiceFactory {

  private final RPCSender rpcSender;
  private final Supplier<NetworkChannel> channelSupplier;

  public RemoteCloudServiceFactory(
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull NetworkComponent defaultComponent,
    @NonNull RPCFactory factory
  ) {
    this.channelSupplier = channelSupplier;
    this.rpcSender = factory.providerForClass(defaultComponent, CloudServiceFactory.class);
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(@NonNull ServiceConfiguration config) {
    return this.rpcSender.invokeMethod("createCloudService", config).fireSync(this.channelSupplier.get());
  }
}
