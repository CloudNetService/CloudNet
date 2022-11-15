/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import dev.derklaro.aerogel.Bindings;
import java.util.ServiceLoader;
import lombok.NonNull;

/**
 * Holds the singleton reference to the injector provider. This class should be used when injection is needed but there
 * is no way to retrieve an instance of the current injector (for example in a plugin on a service).
 * <p>
 * Using dependency injection is the preferred way to get provider instances over using
 * {@code CloudNetDriver.instance()}.
 *
 * @since 4.0
 */
public final class InjectorProviderHolder {

  private static InjectorProvider provider;

  private InjectorProviderHolder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the singleton injector provider for the current runtime, loading it first if the provider was not yet
   * initialized.
   *
   * @return the singleton injector provider for the current runtime.
   */
  public static @NonNull InjectorProvider provider() {
    // check if the provider is already initialized
    if (provider != null) {
      return provider;
    }

    // get the first provided service or set the default instance
    var installedProvider = ServiceLoader.load(InjectorProvider.class).findFirst();
    InjectorProviderHolder.provider = installedProvider.orElseGet(DefaultInjectorProvider::new);

    // install the binding for the provider to the provided injector (does nothing if the installation was already done
    // while constructing the provider holder instance)
    provider.injector().install(Bindings.fixed(InjectorProvider.PROVIDER_ELEMENT, provider));

    // initialization complete
    return provider;
  }
}
