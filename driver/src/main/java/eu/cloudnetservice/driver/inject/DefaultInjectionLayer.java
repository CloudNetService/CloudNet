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
import dev.derklaro.aerogel.auto.AerogelAutoModule;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import jakarta.inject.Provider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation for of an injector layer.
 *
 * @param injector   the injector to use for the layer.
 * @param autoModule the auto registry to use for the layer.
 * @param name       the name of this injection layer.
 * @param <I>        the type of injector this layer uses.
 * @since 4.0
 */
record DefaultInjectionLayer<I extends Injector>(
  @NonNull I injector,
  @NonNull AerogelAutoModule autoModule,
  @NonNull String name
) implements InjectionLayer<I> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInjectionLayer.class);

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
  @SuppressWarnings("unchecked")
  public <T> @UnknownNullability T instance(
    @NonNull Class<T> type,
    @NonNull Consumer<Map<BindingKey<?>, Provider<?>>> overrides
  ) {
    // get the binding associated with the given type & construct a context builder
    var element = BindingKey.of(type);
    var binding = this.injector.binding(element);

    // construct the map and decorate it using the consumer
    Map<BindingKey<?>, Provider<?>> overridesMap = new HashMap<>();
    overrides.accept(overridesMap);

    var contextScope = InjectionContextProvider.provider().enterContextScope(this.injector, binding, overridesMap);

    // resolve the instance
    return contextScope.executeScoped(() -> {
      try {
        return (T) contextScope.context().resolveInstance();
      } finally {
        if (contextScope.context().root()) {
          contextScope.context().finishConstruction();
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull UninstalledBinding<?> binding) {
    this.injector.installBinding(binding);
  }

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
      LOGGER.warn("Unable to auto configure bindings for component {} with file {}", component, fileName, exception);
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
    // TODO aerogel
    /*if (this.injector instanceof Target specifiedInjector) {
      specifiedInjector.removeConstructedBindings();
    }*/

    // remove this injector from the registry
    InjectionLayerProvider.REGISTRY.unregisterLayer(this);
  }
}
