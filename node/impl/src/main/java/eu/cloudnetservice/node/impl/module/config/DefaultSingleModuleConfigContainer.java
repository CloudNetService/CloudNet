/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.config;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.module.ModuleConfigKey;
import eu.cloudnetservice.node.module.config.ModuleConfigFlag;
import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import eu.cloudnetservice.node.module.config.SingleModuleConfigContainer;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorage;
import eu.cloudnetservice.node.module.config.storage.StandardModuleConfigStorageFlag;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of a single module config container.
 *
 * @param <T> the type of the configuration model.
 * @since 4.0
 */
final class DefaultSingleModuleConfigContainer<T>
  extends AbstractModuleConfigContainer<T, SingleModuleConfigContainer<T>>
  implements SingleModuleConfigContainer<T> {

  private boolean loaded;
  private @Nullable T config;

  DefaultSingleModuleConfigContainer(
    @NonNull ModuleConfigStorage storage,
    @NonNull ModuleConfigProperties<T> properties
  ) {
    super(storage, properties, false);
  }

  private DefaultSingleModuleConfigContainer(
    @NonNull ModuleConfigStorage storage,
    @NonNull ModuleConfigProperties<T> properties,
    @NonNull List<Consumer<T>> loadListeners,
    @NonNull List<Consumer<T>> updateListeners,
    @NonNull List<Consumer<T>> removeListeners,
    @NonNull List<UnaryOperator<Document.Mutable>> transformers,
    boolean readOnly,
    @NonNull Set<ModuleConfigFlag> flags
  ) {
    super(storage, properties, loadListeners, updateListeners, removeListeners, transformers, readOnly, flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() {
    this.ensureWritable();
    if (this.config != null) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_STORING);
      this.storage.storeConfig(this.key(), this.documentFromConfig(this.config));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull SingleModuleConfigContainer<T> reload(boolean forceLoadAll) {
    this.ensureWritable();
    if (this.loaded || forceLoadAll) {
      this.loadConfiguration();
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull SingleModuleConfigContainer<T> updateConfiguration(@NonNull T config, boolean flush) {
    this.ensureWritable();
    if (flush) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_STORING);
      this.storage.storeConfig(this.key(), this.documentFromConfig(config));
    }

    this.loaded = true;
    this.config = config;
    this.notifyUpdate(config);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(boolean flush) {
    this.ensureWritable();
    if (flush) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_DELETION);
      this.storage.deleteConfig(this.key());
    }

    var oldConfig = this.config;
    this.loaded = true;
    this.config = null;
    if (oldConfig != null) {
      this.notifyRemove(oldConfig);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateFromDocument(@NonNull Document document, boolean flush) {
    var parsedConfig = this.configFromDocument(this.key(), document);
    this.updateConfiguration(parsedConfig, flush);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean empty() {
    this.ensureLoaded();
    return this.config == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull T config() {
    this.ensureLoaded();
    if (this.config == null) {
      throw new IllegalStateException("Configuration " + this.key() + " is empty");
    }

    return this.config;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull SingleModuleConfigContainer<T> asReadOnly() {
    var copy = new DefaultSingleModuleConfigContainer<>(
      this.storage,
      this.properties,
      this.loadListeners,
      this.updateListeners,
      this.removeListeners,
      this.transformers,
      true,
      this.flags);
    copy.loaded = this.loaded;
    copy.config = this.config;
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleStorageUpdate(@NonNull ModuleConfigKey key) {
    this.loadConfiguration();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull SingleModuleConfigContainer<T> self() {
    return this;
  }

  /**
   * Ensures that this config container is loaded, loading the configuration otherwise.
   */
  private void ensureLoaded() {
    if (!this.loaded) {
      this.loadConfiguration();
    }
  }

  /**
   * Loads the underlying configuration or retrieves a default model if a default supplier is provided.
   *
   * @throws IllegalStateException if loading of the configuration fails for some reason.
   */
  private void loadConfiguration() {
    var document = this.loadDocument(this.key());
    if (document == null) {
      var defaultValueSupplier = this.properties.defaultValueSupplier();
      this.config = defaultValueSupplier == null ? null : defaultValueSupplier.get();
    } else {
      this.config = this.configFromDocument(this.key(), document);
    }

    // mark as loaded and call the load listener
    this.loaded = true;
    if (this.config != null) {
      this.notifyLoad(this.config);
    }
  }
}
