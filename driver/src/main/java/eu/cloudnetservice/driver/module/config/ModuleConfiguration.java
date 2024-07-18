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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * Marks a class that should be deserialized as a module configuration using the given codec. The given path can be
 * interpreted by the codec in all different ways, but should usually be treated relative to the module data directory.
 * How the case of a missing configuration file should be handled is up to the defined codec.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleConfiguration {

  /**
   * Get the codec to use to load the module configuration.
   *
   * @return the codec to use to load the module configuration.
   */
  @NonNull
  String codec();

  /**
   * Get the path where the module configuration file is located.
   *
   * @return the path where the module configuration file is located.
   */
  @NonNull
  String path();

  /**
   * Get if the configuration contains sensitive data. This information can be useful for other module accessing the
   * configuration to see if it contains data that shouldn't be exposed.
   *
   * @return true if the configuration contains sensitive data, false otherwise.
   */
  boolean sensitive() default false;

  /**
   * The static method in the annotated configuration class that can provide a configuration instance filled with
   * default values. The method cannot take any parameters. If no method is specified the no-args constructor is used
   * instead.
   *
   * @return the static factory method in the annotated class to construct a configuration instance with default values.
   */
  String defaultFactory() default "";
}
