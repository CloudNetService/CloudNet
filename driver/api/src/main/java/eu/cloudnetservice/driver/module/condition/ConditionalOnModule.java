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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * An annotation that, when applied to a module task or injectable method inside a module, will check that all the given
 * modules (identified by their module id) are loaded in the current runtime. For this to work, the current module must
 * recommend the other module so that it gets loaded first. When that is the case, the method will be left in the class
 * without changes. However, if the check fails either the complete method is erased from the class or the complete
 * method body, depending on the presence of {@link KeepOnConditionFailure}.
 * <p>
 * This can be used to, for example, hook into another module and enable additional functionality if present without
 * making an additional manual check.
 * <p>
 * Note: while this annotation is retained at runtime the actual annotation will be dropped from the class during
 * introspection and cannot be resolved using, for example, {@code Method.getAnnotation}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// TODO: implement processor for this
public @interface ConditionalOnModule {

  /**
   * Get the ids of the modules that must be loaded for the method to be kept.
   *
   * @return the ids of the modules that must be loaded for the method to be kept.
   */
  @NonNull
  String[] value();
}
