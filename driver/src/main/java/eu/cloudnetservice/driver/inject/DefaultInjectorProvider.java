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

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.AutoAnnotationRegistry;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The default implementation for of an injector provider. This implementation will be used as a fallback when no other
 * injector provider is located on the class path.
 *
 * @since 4.0
 */
final class DefaultInjectorProvider implements InjectorProvider {

  private final Injector injector = Injector.newInjector();
  private final AutoAnnotationRegistry autoRegistry = AutoAnnotationRegistry.newInstance();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull AutoAnnotationRegistry autoRegistry() {
    return this.autoRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(@NonNull Class<T> type) {
    return this.injector.instance(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component) {
    this.installAutoConfigureBindings(this.injector, loader, component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installAutoConfigureBindings(
    @NonNull Injector injector,
    @NonNull ClassLoader loader,
    @NonNull String component
  ) {
    var fileName = String.format(AUTO_CONFIGURE_FILE_NAME_FORMAT, component);
    this.autoRegistry.installBindings(loader, fileName, injector);
  }
}
