/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.driver.template.defaults.RemoteTemplateStorage;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperTemplateStorageProvider implements TemplateStorageProvider {

  private final RPCSender rpcSender;
  private final ComponentInfo componentInfo;
  private final NetworkClient networkClient;

  public WrapperTemplateStorageProvider(
    @NonNull RPCSender sender,
    @NonNull ComponentInfo componentInfo,
    @NonNull NetworkClient networkClient
  ) {
    this.rpcSender = sender;
    this.componentInfo = componentInfo;
    this.networkClient = networkClient;
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
    return this.rpcSender.factory().generateRPCChainBasedApi(
      this.rpcSender,
      TemplateStorage.class,
      GenerationContext.forClass(RemoteTemplateStorage.class).build()
    ).newInstance(new Object[]{storage, this.componentInfo, this.networkClient}, new Object[]{storage});
  }
}
