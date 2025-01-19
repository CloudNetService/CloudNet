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

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.NonNull;

/**
 * This annotation enables the use of registered services from the service registry in an injection context.
 * <p>
 * Instead of doing something like:
 * <pre>
 * {@code
 *   @Inject
 *   public TestClass(ServiceRegistry registry) {
 *     var playerManager = registry.firstProvider(PlayerManager.class);
 *     playerManager.onlineCount();
 *   }
 * }
 * </pre>
 * Using this annotation you can do something like:
 * <pre>
 * {@code
 *   @Inject
 *   public TestClass(@Service PlayerManager playerManager) {
 *     playerManager.onlineCount();
 *   }
 * }
 * </pre>
 * <p>
 * Note: If a service or a service name is requested that the service registry does not know {@code null} is passed as
 * parameter value.
 *
 * @see eu.cloudnetservice.driver.registry.ServiceRegistry
 * @since 4.0
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Service {

  /**
   * The name of the registered provider in the service registry. Leave empty if the name of the provider is not
   * important and any provider with a matching class is accepted.
   *
   * @return name of the registered provider in the service registry.
   */
  @NonNull String name() default "";
}
