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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method should be implemented using an RPC chain invocation. This is only needed if the target api
 * class is generated using the RPC code generator.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCChained {

  /**
   * The flags to pass to the implementation builder when constructing the api implementation.
   *
   * @return the flags to pass to the implementation builder when constructing the api implementation.
   */
  int generationFlags() default 0x01;

  /**
   * Sets the base implementation class to use when generating the implementation for the chained RPC. If not given then
   * the class that is returned from the method is used as a base. Note that the value of this method must be a subtype
   * of the type returned by the annotated method, and handlers must be registered to the return type of the annotated
   * method, not the base implementation type.
   *
   * @return the base implementation class to use when generating an api implementation.
   */
  Class<?> baseImplementation() default Object.class;

  // CHECKSTYLE.OFF: CheckStyle has a bug with annotations inside snippets in JD, see CHECKSTYLE-14446
  /**
   * A mapping that defines how parameters should be passed into the constructor of the implemented class. If the array
   * is empty no parameters will be passed to the constructor. The array has a format of index to index mappings. The
   * first index defines the parameter index on the method it's declared on, the second index defines where that
   * parameter is located on the target constructor.
   * <p>
   * Example:
   * {@snippet lang="java":
   * // maps parameter 0 (in this case named param1) to the second constructor parameter of the target class and
   * // parameter 2 (in this case named param3) to the first constructor parameter
   * @RPCChained(parameterMapping = {0, 1, 2, 0})
   * String myCoolMethod(String param1, String param2, int param3) {} // example declaration, does not work
   * }
   *
   * @return the mapping for the parameter of the method to the constructor parameters.
   */
  // CHECKSTYLE.ON
  int[] parameterMapping() default {};
}
