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

package eu.cloudnetservice.driver.network.rpc.annotation;

import eu.cloudnetservice.driver.network.rpc.RPC;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;

/**
 * An annotation used in combination with the annotation processor to validate that an api class can be used to remotely
 * execute methods. It validates that a class does not break the contract defined in the {@link RPC} class.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCValidation {

  /**
   * Get a regular expression of the methods names to skip while validating the class. Defaults to an empty indicating
   * that no methods should get skipped during validation.
   *
   * @return a regular expression of the methods names to skip while validating the class.
   */
  @Language("RegExp") String value() default "";

  /**
   * Get if static methods in the class should get validated as well. Defaults to false.
   *
   * @return if static methods in the class should get validated as well.
   */
  boolean includeStatic() default false;
}
