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

package eu.cloudnetservice.wrapper.impl.provider;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.impl.template.RemoteTemplateStorage;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperTemplateStorageProvider implements TemplateStorageProvider {

  private final RPCSender providerRPCSender;
  private final RPCImplementationBuilder.InstanceAllocator<? extends TemplateStorage> templateStorageAllocator;

  @RPCInvocationTarget
  public WrapperTemplateStorageProvider(
    @NonNull RPCSender sender,
    @NonNull ComponentInfo componentInfo,
    @NonNull NetworkClient networkClient,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    this.providerRPCSender = sender;

    var rpcFactory = sender.sourceFactory();
    this.templateStorageAllocator = rpcFactory.newRPCBasedImplementationBuilder(RemoteTemplateStorage.class)
      .superclass(TemplateStorage.class)
      .targetChannel(channelSupplier)
      .generateImplementation()
      .withAdditionalConstructorParameters(null, componentInfo, networkClient); // first null param is the storage name
  }

  @Override
  public @NonNull TemplateStorage localTemplateStorage() {
    var storage = this.templateStorage(ServiceTemplate.LOCAL_STORAGE);
    if (storage != null) {
      return storage;
    }

    throw new UnsupportedOperationException("The local storage was unregistered!");
  }

  @Override
  public @Nullable TemplateStorage templateStorage(@NonNull String storage) {
    var baseRPC = this.providerRPCSender.invokeCaller(storage);
    return this.templateStorageAllocator
      .withBaseRPC(baseRPC)
      .changeConstructorParameter(0, storage)
      .allocate();
  }
}
