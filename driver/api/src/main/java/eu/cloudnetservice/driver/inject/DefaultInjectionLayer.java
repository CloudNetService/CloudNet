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

import dev.derklaro.aerogel.InjectionRequest;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.AerogelAutoModule;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The default implementation for of an injector layer.
 *
 * @param injector   the injector to use for the layer.
 * @param autoModule the auto registry to use for the layer.
 * @param name       the name of this injection layer.
 * @param <I>        the type of injector this layer uses.
 * @since 4.0
 */
@ApiStatus.Internal
record DefaultInjectionLayer<I extends Injector>(
  @NonNull I injector,
  @NonNull AerogelAutoModule autoModule,
  @NonNull String name
) implements InjectionLayer<I> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull I injector() {
    return this.injector;
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
  public <T> @UnknownNullability T instance(@NonNull BindingKey<T> bindingKey) {
    return this.injector.instance(bindingKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(
    @NonNull Class<T> type,
    @NonNull UnaryOperator<InjectionRequest<T>> decorator
  ) {
    var key = BindingKey.of(type);
    var injectionRequest = decorator.apply(this.injector.createInjectionRequest(key));
    return injectionRequest.construct();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull UninstalledBinding<?> binding) {
    this.injector.installBinding(binding);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull DynamicBinding binding) {
    this.injector.installBinding(binding);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component) {
    var fileName = String.format(AUTO_CONFIGURE_FILE_NAME_FORMAT, component);
    try (var stream = loader.getResourceAsStream(fileName)) {
      if (stream != null) {
        this.autoModule.deserializeBindings(stream, loader).installBindings(this.injector);
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(
        String.format("Unable to auto configure bindings for component %s with file %s", component, fileName),
        exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InjectionLayer<I> asUncloseable() {
    return new UncloseableInjectionLayer<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InjectionLayer<I> register(@NonNull Object... hints) {
    InjectionLayerProvider.REGISTRY.registerLayer(this, hints);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    // remove the bindings from the parent injector if needed
    this.injector.close();

    // remove this injector from the registry
    InjectionLayerProvider.REGISTRY.unregisterLayer(this);
  }
}
