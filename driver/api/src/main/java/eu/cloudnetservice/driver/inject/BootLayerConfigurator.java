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

package eu.cloudnetservice.driver.inject;

import lombok.NonNull;

/**
 * All classes that are implementing this interface and are registered as a service provider to the class path will be
 * loaded via the service provider interface when the boot injection layer is constructed for the first time. This
 * should be used when a reusable binding is made.
 * <p>
 * The {@link #configureBootLayer(InjectionLayer)} method is only called once per jvm lifetime. If a new service
 * provider gets registered after the call, the provider will be ignored.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface BootLayerConfigurator {

  /**
   * Configures the boot injection layer. Note that the given injection layer cannot be closed.
   *
   * @param bootLayer the boot injection layer to configure.
   * @throws NullPointerException if the given boot layer is null.
   */
  void configureBootLayer(@NonNull InjectionLayer<?> bootLayer);
}
