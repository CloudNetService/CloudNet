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
import eu.cloudnetservice.node.module.config.CompositeModuleConfigContainer;
import eu.cloudnetservice.node.module.config.IdentifiableModuleConfig;
import eu.cloudnetservice.node.module.config.ModuleConfigFlag;
import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorage;
import eu.cloudnetservice.node.module.config.storage.StandardModuleConfigStorageFlag;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of a composite module config container.
 *
 * @param <T> the type of the configuration model.
 * @since 4.0
 */
final class DefaultCompositeModuleConfigContainer<T extends IdentifiableModuleConfig>
  extends AbstractModuleConfigContainer<T, CompositeModuleConfigContainer<T>>
  implements CompositeModuleConfigContainer<T> {

  private final Map<String, T> configurations = new LinkedHashMap<>();

  DefaultCompositeModuleConfigContainer(
    @NonNull ModuleConfigStorage storage,
    @NonNull ModuleConfigProperties<T> properties
  ) {
    super(storage, properties, false);
  }

  private DefaultCompositeModuleConfigContainer(
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
   * Validates that the given config model type is valid to be used with composite module configurations.
   *
   * @param configModelType the config model type to validate.
   * @throws IllegalArgumentException if the given config model type is invalid.
   */
  static void validateConfigModelType(@NonNull Type configModelType) {
    var rawType = GenericTypeReflector.erase(configModelType);
    if (!IdentifiableModuleConfig.class.isAssignableFrom(rawType)) {
      var msg = String.format(
        "Composite module config type '%s' must implement IdentifiableModuleConfig",
        rawType.getName());
      throw new IllegalArgumentException(msg);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() {
    this.ensureWritable();
    this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_STORING);
    this.configurations.values().forEach(config -> {
      var key = this.keyForId(config.configId());
      this.storage.storeConfig(key, this.documentFromConfig(config));
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompositeModuleConfigContainer<T> reload(boolean forceLoadAll) {
    this.ensureWritable();
    if (forceLoadAll) {
      this.loadAllConfigurations();
      return this;
    }

    var configsToLoad = Set.copyOf(this.configurations.keySet());
    configsToLoad.forEach(this::loadConfiguration);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompositeModuleConfigContainer<T> updateConfiguration(@NonNull T config, boolean flush) {
    this.ensureWritable();
    var configId = config.configId();
    var configKey = this.keyForId(configId);
    if (flush) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_STORING);
      this.storage.storeConfig(configKey, this.documentFromConfig(config));
    }

    this.configurations.put(configId, config);
    this.notifyUpdate(config);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateFromDocument(@NonNull String configId, @NonNull Document document, boolean flush) {
    var configKey = this.keyForId(configId);
    var configModel = this.configFromDocument(configKey, document);
    this.updateConfiguration(configModel, flush);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompositeModuleConfigContainer<T> removeConfiguration(@NonNull T configuration, boolean flush) {
    return this.removeConfiguration(configuration.configId(), flush);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public CompositeModuleConfigContainer<T> removeConfiguration(@NonNull String configurationId, boolean flush) {
    this.ensureWritable();
    if (flush) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_DELETION);
      this.storage.deleteConfig(this.keyForId(configurationId));
    }

    var removed = this.configurations.remove(configurationId);
    if (removed != null) {
      this.notifyRemove(removed);
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T configuration(@NonNull String configurationId) {
    var config = this.configurations.get(configurationId);
    if (config == null) {
      this.loadConfiguration(configurationId);
      config = this.configurations.get(configurationId);
    }

    return config;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull List<T> configurations() {
    this.loadAllConfigurations();
    return List.copyOf(this.configurations.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Stream<T> configurationsStream() {
    this.loadAllConfigurations();
    return this.configurations.values().stream();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Iterator<T> iterator() {
    return this.configurations().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompositeModuleConfigContainer<T> asReadOnly() {
    var copy = new DefaultCompositeModuleConfigContainer<>(
      this.storage,
      this.properties,
      this.loadListeners,
      this.updateListeners,
      this.removeListeners,
      this.transformers,
      true,
      this.flags);
    copy.configurations.putAll(this.configurations);
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleStorageUpdate(@NonNull ModuleConfigKey key) {
    this.loadConfiguration(key.configId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull CompositeModuleConfigContainer<T> self() {
    return this;
  }

  /**
   * Loads all configurations known in the storage into this container.
   */
  private void loadAllConfigurations() {
    this.storage.availableConfigKeys(this.key())
      .forEach(key -> this.loadConfiguration(key.configId()));
  }

  /**
   * Loads the configuration with given id.
   *
   * @param configId the id of the config to load.
   */
  private void loadConfiguration(@NonNull String configId) {
    var key = this.keyForId(configId);
    var document = this.loadDocument(key);
    if (document == null) {
      return;
    }

    var config = this.configFromDocument(key, document);
    this.configurations.put(config.configId(), config);
    this.notifyLoad(config);
  }

  /**
   * Constructs a specific key for a config based on the given config id and base composite key in this container.
   *
   * @param configId the id of the config to construct a module key for.
   * @return a module config key for the given config id.
   */
  private @NonNull ModuleConfigKey keyForId(@NonNull String configId) {
    return this.key().withConfigIdSuffix(configId);
  }
}
