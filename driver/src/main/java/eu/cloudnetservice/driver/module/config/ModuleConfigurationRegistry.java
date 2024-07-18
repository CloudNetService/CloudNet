/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.module.config;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A registry of configurations that were loaded for a module during its lifetime. This can for example be used as a
 * hooking point for debugging modules to expose the loaded configurations. Direct registration of configurations is
 * explicitly not exposed into the api and should be done.
 *
 * @since 4.0
 */
public interface ModuleConfigurationRegistry {

  /**
   * Get an unmodifiable view of the configurations that were loaded for the module.
   *
   * @return the configurations that were loaded for the module.
   */
  @NonNull
  @UnmodifiableView
  Collection<ModuleConfigurationContainer<?>> registeredConfigurations();

  /**
   * Get the configuration container for the configuration represented by the given model, can be null if no such
   * configuration was loaded yet.
   *
   * @param configurationModel the model of the configuration to get.
   * @param <T>                the type of the configuration model.
   * @return the container of the configuration represented by the given model, null if no such config is loaded.
   * @throws NullPointerException if the given configuration model is null.
   */
  @Nullable
  <T> ModuleConfigurationContainer<T> configuration(@NonNull Class<T> configurationModel);
}
