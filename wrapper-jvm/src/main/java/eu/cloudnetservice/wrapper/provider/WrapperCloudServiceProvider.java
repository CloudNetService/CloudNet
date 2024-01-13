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

package eu.cloudnetservice.wrapper.provider;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;

public abstract class WrapperCloudServiceProvider implements CloudServiceProvider {

  private final RPCSender rpcSender;
  private final Supplier<NetworkChannel> channelSupplier;

  public WrapperCloudServiceProvider(@NonNull RPCSender sender) {
    this.rpcSender = sender;
    this.channelSupplier = sender.associatedComponent()::firstChannel;
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProvider(@NonNull UUID serviceUniqueId) {
    return this.rpcSender.factory().generateRPCChainBasedApi(
      this.rpcSender,
      SpecificCloudServiceProvider.class,
      GenerationContext.forClass(SpecificCloudServiceProvider.class).channelSupplier(this.channelSupplier).build()
    ).newRPCOnlyInstance(serviceUniqueId);
  }

  @Override
  public @NonNull SpecificCloudServiceProvider serviceProviderByName(@NonNull String serviceName) {
    return this.rpcSender.factory().generateRPCChainBasedApi(
      this.rpcSender,
      SpecificCloudServiceProvider.class,
      GenerationContext.forClass(SpecificCloudServiceProvider.class).channelSupplier(this.channelSupplier).build()
    ).newRPCOnlyInstance(serviceName);
  }
}
