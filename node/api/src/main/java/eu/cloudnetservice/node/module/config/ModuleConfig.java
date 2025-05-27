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

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * This annotation enables the injection of a module configuration into a field or parameter. Example usage:
 * {@snippet lang = "java":
 * public void someModuleTask(@ModuleConfig(id = "test") ModuleConfigContainer<TestConfig> testConfig) {
 *   // do something with the configuration
 * }
 *}
 *
 * @since 4.0
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface ModuleConfig {

  /**
   * Get the id of the configuration that should be injected.
   *
   * @return the id of the configuration that should be injected.
   */
  @NonNull
  String id();

  /**
   * Get the id of the module with which the configuration is associated. If empty, the current module from the context
   * will be used instead.
   *
   * @return the id of the module with which the configuration is associated, empty for the current module.
   */
  @NonNull
  String moduleId() default "";

  /**
   * Get the storage override to be used for the configuration. If empty, the default storage for the config is used.
   *
   * @return the storage override to use for the configuration, empty for the default storage.
   */
  @NonNull
  String storageOverride() default "";

  /**
   * Get the document factory that should be used for converting a stored configuration into a document. By default, the
   * configuration is converted using the {@code json} codec.
   *
   * @return the document factory that should be used for converting a stored configuration into a document
   */
  @NonNull
  String documentFactory() default "json";

  /**
   * Get the standard module configuration flags that should be used for the configuration. By default, no flags are
   * applied. If non-standard flags are required, the module config needs to be resolved manually with the additional
   * flags set.
   *
   * @return the standard module configuration flags that should be used for the configuration.
   */
  @NonNull
  StandardModuleConfigFlag[] flags() default {};

  /**
   * Get the reference to a static method that constructs a default value for the configuration. The reference must
   * start with a fully qualified class name, followed by a hashtag, which is followed by the method name. An example
   * reference is: {@code eu.cloudnetservice.module.test.TestConfig#constructDefault}. Note that the static method must
   * return the same (or a subtype of) the type of the annotated element. Note that composite configurations cannot
   * provide a default factory. If empty, no default factory method is applied.
   *
   * @return the reference to a static method that constructs a default value for the configuration.
   */
  @NonNull
  String defaultFactory() default "";
}
