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

import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.cloudnet.driver.template.defaults.RemoteTemplateStorage;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperTemplateStorageProvider implements TemplateStorageProvider {

  private final RPCSender rpcSender;

  public WrapperTemplateStorageProvider(@NonNull RPCSender sender) {
    this.rpcSender = sender;
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
    return new RemoteTemplateStorage(storage, this.rpcSender.invokeMethod("templateStorage", storage));
  }
}
