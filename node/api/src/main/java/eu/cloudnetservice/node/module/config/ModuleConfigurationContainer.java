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
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A container containing a loaded configuration file. Each container can be injected and will trigger a configuration
 * load in runtime. For this to work the corresponding type must be annotated with {@link ModuleConfiguration}. The
 * container always holds the latest loaded configuration instance which can be dynamically reloaded and changed. This
 * is preferable to injecting the configuration type directly as this would provide the single loaded type without
 * having access to changes later on.
 *
 * @param <T> the type modeling the configuration layout.
 * @since 4.0
 */
public interface ModuleConfigurationContainer<T> {

  /**
   * Reloads this configuration from the original source. If the original source no longer has this configuration
   * available, then the current model is kept as the configuration.
   */
  void reload();

  /**
   * Updates the modeled configuration type and saves the updated model to the original source path. This does not
   * persist the given model in case the codec does not support storing configurations.
   *
   * @param configModel the updated configuration instance to use.
   * @throws NullPointerException if the given configuration model is null.
   */
  void update(@NonNull T configModel);

  /**
   * Updates the underlying configuration model from the given document.
   *
   * @param document the document containing the configuration model to apply.
   * @throws NullPointerException     if the given document is null.
   * @throws IllegalArgumentException if the configuration in the given document is invalid.
   */
  void updateFromDocument(@NonNull Document document);

  /**
   * Registers a listener which will be triggered if this configuration container changes, either by being reloaded or
   * by being updated.
   *
   * @param listener the update listener to invoke when the underlying configuration changes.
   * @throws NullPointerException if the given listener is null.
   */
  void registerUpdateListener(@NonNull Consumer<T> listener);

  /**
   * Get the loaded and modeled configuration instance.
   *
   * @return the loaded and modeled configuration instance.
   */
  @NonNull
  T configModel();

  /**
   * Get the path from which this configuration was originally loaded.
   *
   * @return the path from which this configuration was originally loaded.
   */
  @NonNull
  String path();

  /**
   * Get the configuration codec that was used to load this configuration.
   *
   * @return the configuration codec that was used to load this configuration.
   */
  @NonNull
  ModuleConfigurationCodec codec();

  /**
   * Get if this configuration contains sensitive data which should not be exposed in, for example, debug outputs.
   *
   * @return true if this configuration contains sensitive data, false otherwise.
   */
  boolean containsSensitiveData();

  /**
   * Marks that this module container contains sensitive data.
   *
   * @return this module container, for chaining.
   */
  @NonNull
  ModuleConfigurationContainer<T> markContainsSensitiveData();
}
