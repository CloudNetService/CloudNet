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

import eu.cloudnetservice.driver.base.DisposableResource;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.module.ModuleConfigKey;
import eu.cloudnetservice.node.module.config.ModuleConfigContainer;
import eu.cloudnetservice.node.module.config.ModuleConfigFlag;
import eu.cloudnetservice.node.module.config.ModuleConfigProperties;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorage;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageDescriptor;
import eu.cloudnetservice.node.module.config.storage.StandardModuleConfigStorageFlag;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of a module configuration container. The {@link ModuleConfigContainer} type is sealed, therefore
 * methods in this class do not carray override annotations.
 *
 * @param <T> the type of the configuration model.
 * @param <C> the concrete container type implementation.
 * @since 4.0
 */
abstract class AbstractModuleConfigContainer<T, C extends ModuleConfigContainer<T>> {

  protected final ModuleConfigStorage storage;
  protected final ModuleConfigProperties<T> properties;

  protected final List<Consumer<T>> loadListeners;
  protected final List<Consumer<T>> updateListeners;
  protected final List<Consumer<T>> removeListeners;
  protected final List<UnaryOperator<Document.Mutable>> transformers;

  protected final boolean readOnly;
  protected final Set<ModuleConfigFlag> flags;

  protected DisposableResource autoReloadRegistration;

  protected AbstractModuleConfigContainer(
    @NonNull ModuleConfigStorage storage,
    @NonNull ModuleConfigProperties<T> properties,
    boolean readOnly
  ) {
    this.storage = storage;
    this.properties = properties;

    this.loadListeners = new ArrayList<>();
    this.updateListeners = new ArrayList<>();
    this.removeListeners = new ArrayList<>();
    this.transformers = new ArrayList<>();

    this.readOnly = readOnly;
    this.flags = new HashSet<>(properties.flags());
  }

  protected AbstractModuleConfigContainer(
    @NonNull ModuleConfigStorage storage,
    @NonNull ModuleConfigProperties<T> properties,
    @NonNull List<Consumer<T>> loadListeners,
    @NonNull List<Consumer<T>> updateListeners,
    @NonNull List<Consumer<T>> removeListeners,
    @NonNull List<UnaryOperator<Document.Mutable>> transformers,
    boolean readOnly,
    @NonNull Set<ModuleConfigFlag> flags
  ) {
    this.storage = storage;
    this.properties = properties;

    this.loadListeners = new ArrayList<>(loadListeners);
    this.updateListeners = new ArrayList<>(updateListeners);
    this.removeListeners = new ArrayList<>(removeListeners);
    this.transformers = new ArrayList<>(transformers);

    this.readOnly = readOnly;
    this.flags = new HashSet<>(flags);
  }

  /**
   * Get the key of the configuration that is wrapped in this container. This can be a specific or composite config key
   * depending on the implementation.
   *
   * @return the key of the configuration that is wrapped in this container.
   */
  // @Override
  public @NonNull ModuleConfigKey key() {
    return this.properties.key();
  }

  /**
   * Get the descriptor of the storage used by this module configuration container.
   *
   * @return the descriptor of the storage used by this module configuration container.
   */
  // @Override
  public @NonNull ModuleConfigStorageDescriptor storageDescriptor() {
    return this.storage.descriptor();
  }

  /**
   * Enables auto-reloading of configuration files from the underlying storage. If auto reload was already enabled, this
   * method does nothing.
   *
   * @return this container, for chaining.
   * @throws UnsupportedOperationException if the storage does not support watching or this container is read-only.
   */
  // @Override
  public @NonNull C enableAutoReload() {
    this.ensureWritable();
    if (this.autoReloadRegistration == null) {
      this.ensureStorageSupports(StandardModuleConfigStorageFlag.SUPPORTS_WATCHING);
      this.autoReloadRegistration = this.storage.watchForUpdates(this.key(), this::handleStorageUpdate);
    }

    return this.self();
  }

  /**
   * Disables the auto-reloading of configurations in this container from the underlying storage. If auto reload was not
   * enabled, this method does nothing.
   *
   * @return this container, for chaining.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  // @Override
  public @NonNull C disableAutoReload() {
    this.ensureWritable();
    if (this.autoReloadRegistration != null) {
      this.autoReloadRegistration.dispose();
      this.autoReloadRegistration = null;
    }

    return this.self();
  }

  /**
   * Registers a transformer to this container. Transformers are called when a document is loaded by this container,
   * before it gets converted to a configuration model. This can, for example, be useful for migrating a configuration
   * from an old version to fit the new schema. The transformer can return null to indicate that no notable change was
   * made. The document is instantly flushed back to the storage if the returned document does not equal the original
   * document (unless the transformer returns null).
   *
   * @param transformer the transformer to apply to documents loaded into this container.
   * @return a disposable resource to remove the transformer from this container.
   * @throws NullPointerException          if the given transformer is null.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  // @Override
  public @NonNull DisposableResource registerTransformer(@NonNull UnaryOperator<Document.Mutable> transformer) {
    this.ensureWritable();
    this.transformers.add(transformer);
    return () -> this.transformers.remove(transformer);
  }

  /**
   * Registers a listener to this container that gets called when a configuration is loaded into this container.
   *
   * @param listener the listener to call when a configuration is loaded.
   * @return a disposable resource to remove the listener from this container.
   * @throws NullPointerException if the given listener is null.
   */
  // @Override
  public @NonNull DisposableResource registerLoadListener(@NonNull Consumer<T> listener) {
    this.loadListeners.add(listener);
    return () -> this.loadListeners.remove(listener);
  }

  /**
   * Registers a listener to this container that gets called when a configuration is updated in this container.
   *
   * @param listener the listener to call when a configuration is updated.
   * @return a disposable resource to remove the listener from this container.
   * @throws NullPointerException if the given listener is null.
   */
  // @Override
  public @NonNull DisposableResource registerUpdateListener(@NonNull Consumer<T> listener) {
    this.updateListeners.add(listener);
    return () -> this.updateListeners.remove(listener);
  }

  /**
   * Registers a listener to this container that gets called when a configuration is removed from this container.
   *
   * @param listener the listener to call when a configuration is removed.
   * @return a disposable resource to remove the listener from this container.
   * @throws NullPointerException if the given listener is null.
   */
  // @Override
  public @NonNull DisposableResource registerRemoveListener(@NonNull Consumer<T> listener) {
    this.removeListeners.add(listener);
    return () -> this.removeListeners.remove(listener);
  }

  /**
   * Get if this module configuration container is read-only and does not support update operations.
   *
   * @return true if this container is read-only, false otherwise.
   */
  // @Override
  public boolean readOnly() {
    return this.readOnly;
  }

  /**
   * Get if the given flag is set in this module container.
   *
   * @param flag the flag to check for.
   * @return true if the given flag is set, false otherwise.
   * @throws NullPointerException if the given flag is null.
   */
  // @Override
  public boolean flagSet(@NonNull ModuleConfigFlag flag) {
    return this.flags.contains(flag);
  }

  /**
   * Sets the given flag in this configuration container. This method does nothing if the given flag is already set in
   * this container.
   *
   * @param flag the flag to set.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given flag is null.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  // @Override
  public @NonNull C setFlag(@NonNull ModuleConfigFlag flag) {
    this.ensureWritable();
    this.flags.add(flag);
    return this.self();
  }

  /**
   * Removes the given flag from this configuration container. This method does nothing if the given flag is not
   * currently set in this container.
   *
   * @param flag the flag to remove.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given flag is null.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  // @Override
  public @NonNull C removeFlag(@NonNull ModuleConfigFlag flag) {
    this.ensureWritable();
    this.flags.remove(flag);
    return this.self();
  }

  /**
   * Wraps the given configuration model into a document of the type requested by the config properties.
   *
   * @param config the config to wrap.
   * @return the given configuration wrapped into a document.
   */
  protected @NonNull Document.Mutable documentFromConfig(@NonNull T config) {
    return this.properties.documentFactory().newDocument(config);
  }

  /**
   * Parses the given document into the config model type. Re-stores the config document if a transformer modifies it
   * and if this container is not read-only (modifications are always applied).
   *
   * @param key      the key of the document being loaded.
   * @param document the document that was loaded from the storage.
   * @return the parsed config model from the given document.
   * @throws IllegalArgumentException if converting to the config model fails.
   */
  @SuppressWarnings("unchecked")
  protected @NonNull T configFromDocument(@NonNull ModuleConfigKey key, @NonNull Document document) {
    var transformedDocument = document.mutableCopy();
    for (var transformer : this.transformers) {
      var transformationResult = transformer.apply(transformedDocument);
      if (transformationResult != null) {
        transformedDocument = transformationResult;
      }
    }

    if (!this.readOnly && document != transformedDocument) {
      this.storage.storeConfig(key, transformedDocument);
    }

    var config = (T) transformedDocument.toInstanceOf(this.properties.configModelType());
    if (config == null) {
      throw new IllegalArgumentException("Unable to convert document to " + this.properties.configModelType());
    }

    return config;
  }

  /**
   * Tries to load the document associated with the given module config key. This method returns {@code null} if the
   * storage does not contain a document for the given key.
   *
   * @param key the key of the config to load.
   * @return the loaded configuration document or {@code null} if no document is associated with the key.
   * @throws IllegalArgumentException if loading of the configuration fails for some reason.
   */
  protected @Nullable Document.Mutable loadDocument(@NonNull ModuleConfigKey key) {
    try (var stream = this.storage.loadConfig(key)) {
      var documentFactory = this.properties.documentFactory();
      return stream != null ? documentFactory.parse(stream) : null;
    } catch (Exception exception) {
      throw new IllegalArgumentException("Unable to load module config " + key, exception);
    }
  }

  /**
   * Calls all config load listeners with the given config model.
   *
   * @param config the loaded config model.
   */
  protected void notifyLoad(@NonNull T config) {
    this.loadListeners.forEach(listener -> listener.accept(config));
  }

  /**
   * Calls all config update listeners with the given config model.
   *
   * @param config the updated config model.
   */
  protected void notifyUpdate(@NonNull T config) {
    this.updateListeners.forEach(listener -> listener.accept(config));
  }

  /**
   * Calls all config remove listeners with the given config model.
   *
   * @param config the removed config model.
   */
  protected void notifyRemove(@NonNull T config) {
    this.removeListeners.forEach(listener -> listener.accept(config));
  }

  /**
   * Ensures that this config container is not read-only, throwing an exception if it is.
   *
   * @throws UnsupportedOperationException if this config container is read-only.
   */
  protected void ensureWritable() {
    if (this.readOnly) {
      throw new UnsupportedOperationException("Container is read-only");
    }
  }

  /**
   * Ensures that the config storage supports the given requested operation.
   *
   * @param flag the flag to check for.
   * @throws UnsupportedOperationException if the storage does not support the requested storage flag.
   */
  protected void ensureStorageSupports(@NonNull StandardModuleConfigStorageFlag flag) {
    if (!this.storage.descriptor().flagSet(flag)) {
      throw new UnsupportedOperationException("Storage " + this.storage.name() + " does not support " + flag);
    }
  }

  /**
   * Notifies the implementation of config file updates when auto reload is enabled.
   *
   * @param key the key of the module config that was updated.
   */
  protected abstract void handleStorageUpdate(@NonNull ModuleConfigKey key);

  /**
   * Returns the concrete config container implementation instance.
   *
   * @return this.
   */
  protected abstract @NonNull C self();
}
