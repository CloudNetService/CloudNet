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

package eu.cloudnetservice.driver.document.defaults;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.DocumentFactoryRegistry;
import eu.cloudnetservice.driver.document.empty.EmptyDocumentFactory;
import eu.cloudnetservice.driver.document.gson.GsonDocumentFactory;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

@Singleton
@Provides(DocumentFactoryRegistry.class)
final class DefaultDocumentFactoryRegistry implements DocumentFactoryRegistry {

  private final Map<String, DocumentFactory> registeredFactories = new ConcurrentHashMap<>();

  public DefaultDocumentFactoryRegistry() {
    this.registeredFactories.put("json", GsonDocumentFactory.INSTANCE);
    this.registeredFactories.put("empty", EmptyDocumentFactory.INSTANCE);
  }

  @Override
  public @UnmodifiableView @NonNull Collection<DocumentFactory> documentFactories() {
    return Collections.unmodifiableCollection(this.registeredFactories.values());
  }

  @Override
  public @NonNull DocumentFactory documentFactory(@NonNull String formatName) {
    var factory = this.registeredFactories.get(formatName);
    return Objects.requireNonNull(factory, "no factory with the format name " + formatName + " registered");
  }

  @Override
  public @Nullable DocumentFactory findDocumentFactory(@NonNull String formatName) {
    return this.registeredFactories.get(formatName);
  }

  @Override
  public @Nullable DocumentFactory unregisterDocumentFactory(@NonNull String formatName) {
    return this.registeredFactories.remove(formatName);
  }

  @Override
  public void registerDocumentFactory(@NonNull String formatName, @NonNull DocumentFactory factory) {
    this.registeredFactories.putIfAbsent(formatName, factory);
  }

  @Override
  public @Nullable DocumentFactory replaceDocumentFactory(
    @NonNull String formatName,
    @NonNull DocumentFactory factory
  ) {
    return this.registeredFactories.put(formatName, factory);
  }
}
