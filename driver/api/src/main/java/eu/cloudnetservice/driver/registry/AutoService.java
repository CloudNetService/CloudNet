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

package eu.cloudnetservice.driver.registry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * An annotation which, when applied to a type, indicates that the type should automatically be registered as a service
 * implementation into a service registry. The automatic registration happens based on service files which are
 * automatically emitted from an annotation processor during the build process. Files that are located in the
 * {@code service} directory inside a jar file will be automatically loaded and the given service implementation types
 * will be resolved and registered. Each line of the file should contain one service implementation class binary name.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoService {

  /**
   * The service types that are implemented by the annotated service implementation. All types must be interfaces and
   * assignable to the annotated type.
   *
   * @return the service types that are implemented by the annotated target type.
   */
  @NonNull Class<?>[] services();

  /**
   * Get the name to use for the annotated service implementation.
   *
   * @return the name to use for the annotated service implementation.
   */
  @NonNull String name();

  /**
   * Indicates if the annotated service implementation should be registered as a singleton instance. If that is the
   * case, the instance of the class is resolved using dependency injection and registered into the registry. If this
   * property is set to false the annotated implementation type will be constructed on each service instance retrieval
   * using the public no-args constructor in the annotated class.
   *
   * @return true if the service should be registered as a singleton, false to retrieve a new instance each time.
   */
  boolean singleton() default true;

  /**
   * Indicates if the annotated service type should be marked as the default service implementation after registration.
   *
   * @return true if the service should be marked as the default after registration, false otherwise.
   */
  boolean markAsDefault() default false;
}
