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
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A composite container for module configurations. This container loads configurations from a prefixed key so that a
 * single configuration consisting of multiple configurations can be split into multiple files. This can, for example,
 * be useful if a configuration for each service task is created which should be split up into multiple files instead of
 * one holding a list of configurations for each task.
 *
 * @param <T> the type of the configuration model.
 * @since 4.0
 */
public non-sealed interface CompositeModuleConfigContainer<T extends IdentifiableModuleConfig>
  extends ModuleConfigContainer<T>, Iterable<T> {

  /**
   * Updates the configuration with the given id from the given document. If the given configuration isn't known to this
   * container, it is added to it. Updates and additions are only flushed to the underlying storage if specifically
   * requested. In all other cases the update is only done in-memory and lost with the next container update. Note that
   * the declared UOE is only thrown if flushing was requested, but the underlying storage does not support the
   * operation.
   *
   * @param configId the id of the configuration to update.
   * @param document the document containing the configuration model to apply.
   * @param flush    if the update should be flushed to the underlying storage.
   * @throws NullPointerException          if the given config id or document is null.
   * @throws IllegalArgumentException      if the given config id or config document is invalid.
   * @throws UnsupportedOperationException if the storage does not support storing or this container is read-only.
   */
  void updateFromDocument(@NonNull String configId, @NonNull Document document, boolean flush);

  /**
   * Removes the given configuration from this container. If the given configuration isn't known to this container, the
   * call is silently ignored. Removals are only flushed to the underlying storage if specifically requested. In all
   * other cases the removal is only done in-memory and lost with the next container update. Note that the declared UOE
   * is only thrown if flushing was requested, but the underlying storage does not support the operation.
   *
   * @param configuration the configuration to remove.
   * @param flush         if the remove should be flushed to the underlying storage.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given configuration is null.
   * @throws IllegalArgumentException      if the given configuration provides an invalid id.
   * @throws UnsupportedOperationException if the storage does not support removals or this container is read-only.
   */
  @NonNull
  CompositeModuleConfigContainer<T> removeConfiguration(@NonNull T configuration, boolean flush);

  /**
   * Removes the given configuration from this container. If the given configuration isn't known to this container, the
   * call is silently ignored. Removals are only flushed to the underlying storage if specifically requested. In all
   * other cases the removal is only done in-memory and lost with the next container update. Note that the declared UOE
   * is only thrown if flushing was requested, but the underlying storage does not support the operation.
   *
   * @param configurationId the id of the configuration to remove.
   * @param flush           if the remove should be flushed to the underlying storage.
   * @return this container, for chaining.
   * @throws NullPointerException          if the given configuration is null
   * @throws IllegalArgumentException      if the given configuration id is invalid.
   * @throws UnsupportedOperationException if the storage does not support removals or this container is read-only.
   */
  @NonNull
  CompositeModuleConfigContainer<T> removeConfiguration(@NonNull String configurationId, boolean flush);

  /**
   * Retrieves the configuration with the given id from this container. If no such configuration exists or the
   * configuration couldn't be converted to the required type, null is returned.
   *
   * @param configurationId the id of the configuration to retrieve.
   * @return the configuration with the given id, or null if no such configuration exists.
   * @throws NullPointerException     if the given configuration is null
   * @throws IllegalArgumentException if the given configuration id is invalid.
   */
  @Nullable
  T configuration(@NonNull String configurationId);

  /**
   * Returns an unmodifiable list of all configurations that are available to this container. Note that this call loads
   * all configurations from the underlying store that weren't yet accessed by this container before returning.
   *
   * @return all configurations that are available to this container.
   */
  @NonNull
  @UnmodifiableView
  List<T> configurations();

  /**
   * Returns a sequential stream holding all the configurations available to this container. Note that this call lazily
   * loads all configurations from the underlying store that weren't yet accessed by this container.
   *
   * @return a sequential stream holding all the configurations available to this container.
   */
  @NonNull
  Stream<T> configurationsStream();

  // overrides to return the actual container type instead of the generic one

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> reload(boolean forceLoadAll);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> enableAutoReload();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> disableAutoReload();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> updateConfiguration(@NonNull T config, boolean flush);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  @CheckReturnValue
  CompositeModuleConfigContainer<T> asReadOnly();

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> removeFlag(@NonNull ModuleConfigFlag flag);

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  CompositeModuleConfigContainer<T> setFlag(@NonNull ModuleConfigFlag flag);
}
