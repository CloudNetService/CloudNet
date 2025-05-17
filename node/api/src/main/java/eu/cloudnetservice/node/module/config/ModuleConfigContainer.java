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

import lombok.NonNull;

/**
 *
 */
public sealed interface ModuleConfigContainer<T> permits SingleModuleConfigContainer, CompositeModuleConfigContainer {

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
  void updateConfiguration(@NonNull T configModel);

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

  boolean flagSet(@NonNull ModuleConfigFlag flag);

  @NonNull
  ModuleConfigContainer<T> setFlag(@NonNull ModuleConfigFlag flag);

  @NonNull
  ModuleConfigContainer<T> removeFlag(@NonNull ModuleConfigFlag flag);
}
