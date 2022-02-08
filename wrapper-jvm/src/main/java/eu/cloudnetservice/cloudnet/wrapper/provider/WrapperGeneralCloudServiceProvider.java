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

package eu.cloudnetservice.cloudnet.wrapper.provider;

import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.provider.GeneralCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.defaults.RemoteSpecificCloudServiceProvider;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;

public abstract class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

  private final RPCSender rpcSender;
  private final Supplier<NetworkChannel> channelSupplier;

  public WrapperGeneralCloudServiceProvider(@NonNull RPCSender sender) {
    this.rpcSender = sender;
    this.channelSupplier = sender.associatedComponent()::firstChannel;
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProvider(@NonNull UUID serviceUniqueId) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, this.channelSupplier, serviceUniqueId);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProviderByName(@NonNull String serviceName) {
    return new RemoteSpecificCloudServiceProvider(this, this.rpcSender, this.channelSupplier, serviceName);
  }
}
