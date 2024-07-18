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

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.driver.module.ModuleContainer;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * A codec instance to load and store configurations objects with. This can for example be a json-based loader.
 *
 * @since 4.0
 */
public interface ModuleConfigurationCodec extends Named {

  /**
   * Get if this codec supports storing configurations. This could for example be false if the configuration is loaded
   * from an HTTP server but the server does not support updating the resource.
   *
   * @return true if this codec supports storing configurations, false otherwise.
   */
  boolean supportsStoring();

  /**
   * Stores the given configuration container to the location where the configuration was loaded from. This method is
   * only supported when {@link #supportsStoring()} returns true.
   *
   * @param configurationContainer the configuration container to store.
   * @throws NullPointerException          if the given configuration container is null.
   * @throws UnsupportedOperationException if this codec does not support storing configurations.
   */
  void storeConfiguration(@NonNull ModuleConfigurationContainer<?> configurationContainer);

  /**
   * Tries to load the configuration from the requested config path. If the configuration does not exist at the
   * location, the given default config supplier is used to retrieve and store an empty configuration model (the model
   * is not stored if this codec does not support storing configurations). In case the loading of the module
   * configuration fails the method should throw an exception to indicate a non-recoverable error which should interrupt
   * the module loading process.
   *
   * @param moduleContainer     the module container to which the config belongs, might be in an early init stage.
   * @param requestedConfigPath the path to the requested configuration.
   * @param requestedConfigType the configuration model which was requested to be parsed from the config.
   * @param <T>                 the type of the configuration model.
   * @return a configuration container containing the loaded configuration model.
   * @throws NullPointerException if one of the given arguments is null.
   * @throws Exception            if any fatal exception occurs during loading of the configuration.
   */
  @NonNull
  <T> ModuleConfigurationContainer<T> loadConfiguration(
    @NonNull ModuleContainer moduleContainer,
    @NonNull String requestedConfigPath,
    @NonNull Class<T> requestedConfigType,
    @NonNull Supplier<T> defaultConfigSupplier) throws Exception;
}
