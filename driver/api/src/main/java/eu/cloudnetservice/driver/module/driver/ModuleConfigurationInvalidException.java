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

package eu.cloudnetservice.driver.module.driver;

import java.nio.file.Path;
import lombok.NonNull;

/**
 * An exception thrown when a module is unable to read its configuration file for whatever reason.
 *
 * @since 4.0
 */
public class ModuleConfigurationInvalidException extends RuntimeException {

  /**
   * Constructs a new module configuration invalid exception.
   *
   * @param configPath        the path to the configuration file.
   * @param originalException the exception thrown originally during parsing.
   * @throws NullPointerException if the given config path or original exception is null.
   */
  public ModuleConfigurationInvalidException(@NonNull Path configPath, @NonNull Exception originalException) {
    super(String.format("Unable to read module configuration from path %s", configPath), originalException);
  }
}
