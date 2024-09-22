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

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import jakarta.inject.Provider;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * An injection layer which delegates all calls to the parent layer, but throwing an exception if there is a try to
 * close the layer.
 *
 * @param parent the parent layer to delegate all calls to.
 * @param <I>    the type of injector used by the parent layer.
 * @since 4.0
 */
record UncloseableInjectionLayer<I extends Injector>(@NonNull InjectionLayer<I> parent) implements InjectionLayer<I> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.parent.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull I injector() {
    return this.parent.injector();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(@NonNull Class<T> type) {
    return this.parent.instance(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(@NonNull BindingKey<T> bindingKey) {
    return this.parent.instance(bindingKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(
    @NonNull Class<T> type,
    @NonNull Consumer<Map<BindingKey<?>, Provider<?>>> overrides
  ) {
    return this.parent.instance(type, overrides);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull UninstalledBinding<?> binding) {
    this.parent.install(binding);
  }

  @Override
  public void install(@NonNull DynamicBinding binding) {
    this.parent.install(binding);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component) {
    this.parent.installAutoConfigureBindings(loader, component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InjectionLayer<I> asUncloseable() {
    return this;
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
    throw new UnsupportedOperationException("Injection layer cannot be closed");
  }
}
