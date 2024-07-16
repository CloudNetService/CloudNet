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

package eu.cloudnetservice.driver.module.condition;

import eu.cloudnetservice.driver.module.condition.processors.ConditionalOnEnvProcessor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * An annotation that, when applied to a module task or injectable method inside a module, will check that the module
 * runs in one of the given driver environments. When that is the case, the method will be left in the class without
 * changes. However, if the check fails either the complete method is erased from the class or the complete method body,
 * depending on the presence of {@link KeepOnConditionFailure}.
 * <p>
 * This method can for example be used to hook into wrapper-specific functionality that shouldn't be executed on nodes.
 * <p>
 * Note: while this annotation is retained at runtime the actual annotation will be dropped from the class during
 * introspection and cannot be resolved using, for example, {@code Method.getAnnotation}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TargetConditionProcessor(ConditionalOnEnvProcessor.class)
public @interface ConditionalOnEnv {

  /**
   * Get the names of the driver environments in which annotated method should run. Only one of the given environments
   * must match in order to mark the check as successful.
   *
   * @return the names of the driver environments in which the annotated method should run.
   */
  @NonNull
  String[] value();
}
