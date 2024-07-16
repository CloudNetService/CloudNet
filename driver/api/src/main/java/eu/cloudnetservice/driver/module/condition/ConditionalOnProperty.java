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

import eu.cloudnetservice.driver.module.condition.processors.ConditionalOnPropertyProcessor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * An annotation that, when applied to a module task or injectable method inside a module, will check that the
 * environment variable or system property identified by the provided key(s) matches the given required value. When that
 * is the case, the method will be left in the class without changes. However, if the check fails either the complete
 * method is erased from the class or the complete method body, depending on the presence of
 * {@link KeepOnConditionFailure}.
 * <p>
 * Note: while this annotation is retained at runtime the actual annotation will be dropped from the class during
 * introspection and cannot be resolved using, for example, {@code Method.getAnnotation}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TargetConditionProcessor(ConditionalOnPropertyProcessor.class)
public @interface ConditionalOnProperty {

  /**
   * Get the environment variable which can be used to specify the required value. If no value is associated with the
   * environment variable, the system property is used as a fallback. Set this key to an empty string (default) to
   * indicate that no environment variable is associated with the property.
   * <p>
   * Note: at least an environment variable or system property must be given, if that is not the case the processing
   * step is always considered to be successful.
   *
   * @return the environment variable which can be used to specify the required value.
   */
  @NonNull
  String env() default "";

  /**
   * Get the system property which can be used to specify the required value. Set this key to an empty string (default)
   * to indicate that no system property is associated with the property.
   * <p>
   * Note: at least an environment variable or system property must be given, if that is not the case the processing
   * step is always considered to be successful.
   *
   * @return the system property which can be used to specify the required value.
   */
  @NonNull
  String prop() default "";

  /**
   * Get the required value of either the environment variable or system property.
   *
   * @return the required value of either the environment variable or system property.
   */
  @NonNull
  String requiredValue();

  /**
   * Get if the value comparison between the provided value in runtime and the required value should be executed
   * case-sensitive (default) or case-insensitive.
   *
   * @return true if the value comparison should be done case-sensitive, false otherwise.
   */
  boolean caseSensitive() default true;
}
