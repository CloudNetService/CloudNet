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

import eu.cloudnetservice.driver.module.condition.processors.ConditionalOnClassProcessor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * An annotation that, when applied to a module task or injectable method inside a module, will check that all the given
 * classes in this annotation are present or absent, depending on the given presence status. When that is the case, the
 * method will be left in the class without changes. However, if the check fails either the complete method is erased
 * from the class or the complete method body, depending on the presence of {@link KeepOnConditionFailure}.
 * <p>
 * This can be used to, for example, hook into another module or an older module version without having to deal with
 * class loading issues due to missing classes on the classpath. The annotation is introspected before the class is
 * actually loaded meaning that no care must be taken about class loading issues inside the method.
 * <p>
 * Note: while this annotation is retained at runtime the actual annotation will be dropped from the class during
 * introspection and cannot be resolved using, for example, {@code Method.getAnnotation}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TargetConditionProcessor(ConditionalOnClassProcessor.class)
public @interface ConditionalOnClass {

  /**
   * The classes that must either be present on absent. The values are introspected before the containing class is
   * loaded, therefore it's safe to specify class references that might not be on the runtime classpath.
   *
   * @return the classes that must either be present or absent in runtime.
   */
  @NonNull
  Class<?>[] value();

  /**
   * Defines the required presence of the given classes. This annotation can either check if the given classes are
   * present or if the given classes are absent.
   *
   * @return the required presence of the given classes.
   */
  @NonNull
  Presence presence() default Presence.PRESENT;

  /**
   * The status variants of class presence.
   *
   * @since 4.0
   */
  enum Presence {

    /**
     * The classes must be present on the runtime classpath.
     */
    PRESENT,
    /**
     * The classes must be absent on the runtime classpath.
     */
    ABSENT,
  }
}
