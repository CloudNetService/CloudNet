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

package eu.cloudnetservice.node.module.config;

import eu.cloudnetservice.driver.base.DisposableResource;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageDescriptor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;

/**
 * !!! LAZY
 */
public sealed interface ModuleConfigContainer<T> permits SingleModuleConfigContainer, CompositeModuleConfigContainer {

  /**
   * Get the descriptor of the storage used by this module configuration container.
   *
   * @return the descriptor of the storage used by this module configuration container.
   */
  @NonNull
  ModuleConfigStorageDescriptor storageDescriptor();

  /**
   * Flushes all configurations available to this container to the underlying storage.
   *
   * @throws UnsupportedOperationException if the storage does not support storing or this container is read-only.
   */
  void flush();

  /**
   * Reloads all configurations that are currently loaded in this container from the underlying storage. It can
   * optionally be enabled to also load all configurations that weren't loaded prior to the method invocation.
   *
   * @param forceLoadAll whether all available configurations should be loaded, even if they weren't previously loaded.
   * @return this container, for chaining.
   * @throws UnsupportedOperationException if thus container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> reload(boolean forceLoadAll);

  /**
   * @return this container, for chaining.
   * @throws UnsupportedOperationException if the storage does not support watching or this container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> enableAutoReload();

  /**
   * Disables the auto-reloading of configurations in this container from the underlying storage. If auto reload was not
   * enabled, this method does nothing.
   *
   * @return this container, for chaining.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> disableAutoReload();

  /**
   * Updates the given configuration in this container. If the given configuration isn't known to this container, it is
   * added to it. Updates and additions are only flushed to the underlying storage if specifically requested. In all
   * other cases the update is only done in-memory and lost with the next container update. Note that the declared UOE
   * is only thrown if flushing was requested, but the underlying storage does not support the operation.
   *
   * @param config the config to add or update.
   * @param flush  if the update should be flushed to the underlying storage.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given configuration is null.
   * @throws UnsupportedOperationException if the storage does not support storing or this container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> updateConfiguration(@NonNull T config, boolean flush);

  @NonNull
  DisposableResource registerTransformer(@NonNull UnaryOperator<Document> listener);

  @NonNull
  DisposableResource registerLoadListener(@NonNull Consumer<T> listener);

  @NonNull
  DisposableResource registerUpdateListener(@NonNull Consumer<T> listener);

  @NonNull
  DisposableResource registerRemoveListener(@NonNull Consumer<T> listener);

  /**
   * Get if this module configuration container is read-only and does not support update operations.
   *
   * @return true if this container is read-only, false otherwise.
   */
  boolean readOnly();

  /**
   * Get a new read-only container instance which does not support any write operations. Note that this returns a new
   * container instance and does not mark the original container instance as read-only.
   *
   * @return a new configuration container based on this container that does not support writes.
   */
  @NonNull
  @CheckReturnValue
  ModuleConfigContainer<T> asReadOnly();

  /**
   * Get if the given flag is set in this module container.
   *
   * @param flag the flag to check for.
   * @return true if the given flag is set, false otherwise.
   * @throws NullPointerException if the given flag is null.
   */
  boolean flagSet(@NonNull ModuleConfigFlag flag);

  /**
   * Sets the given flag in this configuration container. This method does nothing if the given flag is already set in
   * this container.
   *
   * @param flag the flag to set.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given flag is null.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> setFlag(@NonNull ModuleConfigFlag flag);

  /**
   * Removes the given flag from this configuration container. This method does nothing if the given flag is not
   * currently set in this container.
   *
   * @param flag the flag to remove.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given flag is null.
   * @throws UnsupportedOperationException if this container is read-only.
   */
  @NonNull
  ModuleConfigContainer<T> removeFlag(@NonNull ModuleConfigFlag flag);
}
