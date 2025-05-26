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

import eu.cloudnetservice.driver.module.ModuleConfigKey;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A registry of configurations that were loaded for a module during its lifetime. This can, for example, be used as a
 * hooking point for debugging modules to expose the loaded configurations. Direct registration of configurations is
 * explicitly not exposed into the api and should be done.
 *
 * @since 4.0
 */
public interface ModuleConfigRegistry {

  /**
   * Get an unmodifiable view of the configurations that were loaded for the module.
   *
   * @return the configurations that were loaded for the module.
   */
  @NonNull
  @UnmodifiableView
  Collection<ModuleConfigContainer<?>> registeredConfigurations();

  /**
   * Get the configuration container for the configuration with the given key, can be null if no such configuration was
   * loaded yet.
   *
   * @param key the key of the configuration to get.
   * @param <T> the type of the configuration model.
   * @return the container of the configuration with the given key, null if no such config is loaded.
   * @throws NullPointerException if the given key is null.
   */
  @Nullable
  <T> ModuleConfigContainer<T> configContainer(@NonNull ModuleConfigKey key);

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
  <T> ModuleConfigContainer<T> configContainer(@NonNull Type configurationModel);

  /**
   * Gets or constructs a configuration container for the given config properties. An existing configuration container
   * is resolved based on the config key provided by the given properties.
   *
   * @param properties the properties to use for resolving/creating the configuration container.
   * @param <T>        the type of the configuration model.
   * @return an existing or newly constructed config container based on the given properties.
   * @throws NullPointerException if the given config properties instance is null.
   */
  @NonNull
  <T> ModuleConfigContainer<T> configContainer(@NonNull ModuleConfigProperties<T> properties);
}
