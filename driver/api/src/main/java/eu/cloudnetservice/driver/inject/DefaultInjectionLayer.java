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

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.auto.runtime.AutoAnnotationRegistry;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.internal.context.util.ContextInstanceResolveHelper;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The default implementation for of an injector layer.
 *
 * @param injector     the injector to use for the layer.
 * @param autoRegistry the auto registry to use for the layer.
 * @param name         the name of this injection layer.
 * @param <I>          the type of injector this layer uses.
 * @since 4.0
 */
@ApiStatus.Internal
record DefaultInjectionLayer<I extends Injector>(
  @NonNull I injector,
  @NonNull AutoAnnotationRegistry autoRegistry,
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
  public <T> @UnknownNullability T instance(@NonNull Element element) {
    return this.injector.instance(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @UnknownNullability T instance(
    @NonNull Class<T> type,
    @NonNull Consumer<InjectionContext.Builder> builder
  ) {
    // get the binding associated with the given type & construct a context builder
    var element = Element.forType(type);
    var binding = this.injector.binding(element);
    var contextBuilder = InjectionContext.builder(type, binding.provider(element));

    // apply the builder decorator to the builder
    builder.accept(contextBuilder);

    // resolve the instance
    return (T) ContextInstanceResolveHelper.resolveInstanceAndRemoveContext(contextBuilder.build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull BindingConstructor constructor) {
    this.injector.install(constructor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component) {
    var fileName = String.format(AUTO_CONFIGURE_FILE_NAME_FORMAT, component);
    this.autoRegistry.installBindings(loader, fileName, this.injector);
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
    if (this.injector instanceof SpecifiedInjector specifiedInjector) {
      specifiedInjector.removeConstructedBindings();
    }

    // remove this injector from the registry
    InjectionLayerProvider.REGISTRY.unregisterLayer(this);
  }
}
