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

import eu.cloudnetservice.driver.document.Document;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;

/**
 * A module config container that contains a single module configuration.
 *
 * @param <T> the type of the configuration model.
 * @since 4.0
 */
public non-sealed interface SingleModuleConfigContainer<T> extends ModuleConfigContainer<T> {

  /**
   * Removes the configuration associated with this container from the underlying storage. After this method was called,
   * the configuration cannot be accessed anymore from this container. If the configuration was already removed, the
   * call is silently ignored.
   *
   * @throws UnsupportedOperationException if the storage does not support removals or this container is read-only.
   */
  void remove();

  /**
   * Get if the configuration associated with this container was removed.
   *
   * @return true if the configuration was removed, false otherwise.
   */
  boolean removed();

  /**
   * Updates the underlying configuration model from the given document. If the given configuration isn't known to this
   * container, it is added to it. Updates and additions are only flushed to the underlying storage if specifically
   * requested. In all other cases the update is only done in-memory and lost with the next container update. Note that
   * the declared UOE is only thrown if flushing was requested, but the underlying storage does not support the
   * operation.
   *
   * @param document the document containing the configuration model to apply.
   * @param flush    if the update should be flushed to the underlying storage.
   * @throws NullPointerException          if the given document is null.
   * @throws IllegalArgumentException      if the given config document is invalid.
   * @throws IllegalStateException         if the configuration associated with this container was removed.
   * @throws UnsupportedOperationException if the storage does not support storing or this container is read-only.
   */
  void updateFromDocument(@NonNull Document document, boolean flush);

  /**
   * Get the configuration instance wrapped in this container. Note that this method loads the configuration if the
   * configuration wasn't loaded before.
   *
   * @return the configuration instance wrapped in this container.
   * @throws IllegalStateException if the configuration associated with this container was removed.
   */
  @NonNull
  T config();

  // overrides to return the actual container type instead of the generic one

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> reload(boolean forceLoadAll);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> enableAutoReload();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> disableAutoReload();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> updateConfiguration(@NonNull T config, boolean flush);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  @CheckReturnValue
  SingleModuleConfigContainer<T> asReadOnly();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> removeFlag(@NonNull ModuleConfigFlag flag);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  SingleModuleConfigContainer<T> setFlag(@NonNull ModuleConfigFlag flag);
}
