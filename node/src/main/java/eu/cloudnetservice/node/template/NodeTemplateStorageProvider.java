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

package eu.cloudnetservice.node.template;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(TemplateStorageProvider.class)
public class NodeTemplateStorageProvider implements TemplateStorageProvider {

  private final ServiceRegistry serviceRegistry;

  @Inject
  public NodeTemplateStorageProvider(
    @NonNull RPCFactory rpcFactory,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry
  ) {
    this.serviceRegistry = serviceRegistry;
    rpcFactory.newHandler(TemplateStorageProvider.class, this).registerTo(handlerRegistry);
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
    return this.serviceRegistry.provider(TemplateStorage.class, storage);
  }

  @Override
  public @NonNull Collection<String> availableTemplateStorages() {
    return this.serviceRegistry.providers(TemplateStorage.class).stream().map(Nameable::name).toList();
  }
}
