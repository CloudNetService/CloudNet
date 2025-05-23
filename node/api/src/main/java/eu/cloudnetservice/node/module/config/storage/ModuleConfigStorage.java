/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.config.storage;

import eu.cloudnetservice.driver.base.DisposableResource;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.module.ModuleConfigKey;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A storage for module configurations. A storage implementation is required to at least provide two operations:
 * <ol>
 *   <li>Listing all configurations that are available in the storage.
 *   <li>Getting a module configuration from the storage.
 * </ol>
 * <br>
 * There are other, optional operations that a module storage can support. This includes:
 * <ol>
 *   <li>Storing newly created and updated module configurations.
 *   <li>Deleting module configurations.
 *   <li>Watching a specific module configuration for changes.
 * </ol>
 * <br>
 * A module configuration storage can indicate if an operation is supported by providing flags for each operation that
 * is supported. It's up to the implementation of a module configuration to decide if a call to an unsupported operation
 * should result in an exception or get silently ignored. However, the first option is preferred to provide transparency
 * to the caller of the method.
 * <br>
 * Storage implementations are registered to and loaded from the service registry.
 *
 * @since 4.0
 */
public interface ModuleConfigStorage extends Named {

  /**
   * Get the descriptor of this configuration storage.
   *
   * @return the descriptor of this configuration storage.
   */
  @NonNull
  ModuleConfigStorageDescriptor descriptor();

  /**
   * Get all configuration keys that are available in this storage.
   *
   * @return all configuration keys that are available in this storage.
   */
  @NonNull
  Set<ModuleConfigKey> availableConfigKeys();

  /**
   * Get all configuration keys in this storage that match the given config key.
   *
   * @param key the key to filter for. Can either ba a specific or a composite key.
   * @return all configuration keys in this storage that match the given key.
   * @throws NullPointerException if the given key is null.
   */
  @NonNull
  Set<ModuleConfigKey> availableConfigKeys(@NonNull ModuleConfigKey key);

  /**
   * Loads the configuration with the given key from this storage, returning null if the configuration does not exist in
   * this storage. The caller of this method is responsible for closing the returned input stream after the
   * configuration was consumed.
   *
   * @param key the key of the configuration to load.
   * @return an input stream of the configuration document or null if the configuration does not exist in this storage.
   * @throws NullPointerException           if the given configuration key is null.
   * @throws ModuleConfigStorageIOException if loading of the given configuration failed for some reason.
   */
  @Nullable
  InputStream loadConfig(@NonNull ModuleConfigKey key);

  /**
   * Stores the configuration document in association with the given configuration key in this storage. An exception
   * might be thrown if this storage implementation does not support storing of configurations (this can be determined
   * using the flags provided by this storage).
   *
   * @param key      the key of the configuration to store.
   * @param document the configuration document to store.
   * @throws NullPointerException           if the given configuration key or configuration document is null.
   * @throws ModuleConfigStorageIOException if storing of the given configuration failed for some reason.
   * @throws UnsupportedOperationException  if this implementation does not support storing of configurations.
   */
  void storeConfig(@NonNull ModuleConfigKey key, @NonNull Document document);

  /**
   * Deletes the configuration document associated with the given configuration key from this storage. An exception
   * might be thrown if this storage implementation does not support the deletion of configurations (this can be
   * determined using the flags provided by this storage).
   *
   * @param key the key of the configuration to delete.
   * @throws NullPointerException           if the given configuration key is null.
   * @throws ModuleConfigStorageIOException if deleting the given configuration failed for some reason.
   * @throws UnsupportedOperationException  if this implementation does not support deletion of configurations.
   */
  void deleteConfig(@NonNull ModuleConfigKey key);

  /**
   * Registers an update listener for configuration changes to this storage. An optional key can be provided if the
   * caller only wants updates for specific configuration keys. An exception might be thrown if this storage
   * implementation does not support watching the configurations for changes (this can be determined using the flags
   * provided by this storage).
   * <p>
   * If the given config key is a composite key, the listener receives updates for all configurations that are matching
   * the composite key. If the key targets a specific config, then only updates of the specific config are passed to the
   * listener. The listener always receives specific config keys, never a composite key.
   *
   * @param key            an optional config key to only receive updates of configs matching the key.
   * @param updateListener the update listener that receives the keys of the updated configurations.
   * @return a disposable resource to remove the update listener from this storage.
   * @throws NullPointerException          if the given update listener is null.
   * @throws UnsupportedOperationException if this implementation does not support watching for configuration updates.
   */
  @NonNull
  DisposableResource watchForUpdates(@Nullable ModuleConfigKey key, @NonNull Consumer<ModuleConfigKey> updateListener);
}
