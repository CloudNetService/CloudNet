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

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.binding.BindingConstructor;
import eu.cloudnetservice.driver.base.Named;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents the current layer of injection which is being worked on.
 *
 * @param <I> the type of injector this layer uses.
 * @since 4.0
 */
public sealed interface InjectionLayer<I extends Injector>
  extends Named, AutoCloseable
  permits DefaultInjectionLayer, UncloseableInjectionLayer {

  /**
   * The file name format for the autoconfigure files. This format will be used when calling
   * {@code installAutoConfigureBindings} and gets formatted with the given component name to the method.
   */
  String AUTO_CONFIGURE_FILE_NAME_FORMAT = "autoconfigure/%s.aero";

  /**
   * Returns the singleton boot injection layer. That layer contains all bindings which were used during the current
   * runtime component initialization and contains all core bindings.
   *
   * @return the singleton boot injection layer.
   */
  static @NonNull InjectionLayer<Injector> boot() {
    return InjectionLayerProvider.boot();
  }

  /**
   * Returns the singleton injection layer which should be used for all kinds of external component injection (like
   * plugins). The ext layer has all bindings of the boot layer present.
   *
   * @return the singleton ext injection layer.
   */
  static @NonNull InjectionLayer<Injector> ext() {
    return InjectionLayerProvider.ext();
  }

  /**
   * Provides a fresh injection layer, with no bindings configured to the returned injector, except for a binding to the
   * layer itself with the provided name.
   *
   * @param name the name of the new injection layer.
   * @return a new injection layer with no configured bindings.
   * @throws NullPointerException     if the given name is null.
   * @throws IllegalArgumentException if the given name is invalid.
   */
  static @NonNull InjectionLayer<Injector> fresh(@NonNull String name) {
    return InjectionLayerProvider.fresh(name);
  }

  /**
   * Constructs a new child layer for the given parent layer. All bindings from the parent layer are still visible to
   * the child layer, but not vise-versa.
   * <p>
   * The new layer has a binding for the layer type with the given name in order to request injection of the given
   * layer. Note that injecting the injector of a layer still results in the child layer injector, even if no name is
   * present.
   *
   * @param parent the parent layer to construct the child from.
   * @param name   the name of the child layer.
   * @return a new layer with the given parent layer.
   * @throws NullPointerException     if the given parent layer or name is null.
   * @throws IllegalArgumentException if the given name is invalid.
   */
  static @NonNull InjectionLayer<Injector> child(@NonNull InjectionLayer<Injector> parent, @NonNull String name) {
    return InjectionLayerProvider.child(parent, name);
  }

  /**
   * Constructs a new child layer for the given parent layer. All bindings from the parent layer are still visible to
   * the child layer, but not vise-versa. The returned child layer uses a specified injector and passes it to the given
   * configurator in order to precisely configure bindings.
   * <p>
   * The new layer has a binding for the layer type with the given name in order to request injection of the given
   * layer. Note that injecting the injector of a layer still results in the child layer injector, even if no name is
   * present.
   * <p>
   * This type of layer should be used in a context when the constructed bindings of classes should be visible to the
   * parent layer, while some bindings need to be specifically overridden. This can for example be a modular context,
   * where the module description should only be visible to the current module, not to all modules.
   *
   * @param parent       the parent layer to construct the child from.
   * @param name         the name of the child layer.
   * @param configurator the configurator for the layer, for convince it also provides the specified injector.
   * @return a new specified layer with the given parent layer.
   * @throws NullPointerException     if the given parent layer, name or configurator is null.
   * @throws IllegalArgumentException if the given name is invalid or the parent layer is the boot layer.
   */
  static @NonNull InjectionLayer<SpecifiedInjector> specifiedChild(
    @NonNull InjectionLayer<? extends Injector> parent,
    @NonNull String name,
    @NonNull BiConsumer<InjectionLayer<SpecifiedInjector>, SpecifiedInjector> configurator
  ) {
    Preconditions.checkArgument(parent != boot(), "Parent of a specified layer cannot be the boot layer");
    return InjectionLayerProvider.specifiedChild(parent, name, configurator);
  }

  /**
   * Finds the injection layer associated with the given object or returns the default {@link #ext()} injection layer.
   * The following search rules apply (in order):
   * <ol>
   *   <li>If the given object is an {@link InjectionLayerHolder}, the layer stored in the holder is returned.
   *   <li>If the given object has a layer associated in the layer registry, that layer is returned.
   *   <li>If the given object is a class the associated class loader of the given class is checked.
   *   <li>If the given object is not a class loader the loader of the object class is checked.
   *   <li>If none of the above rules matches the the default {@link #ext()} layer is returned.
   * </ol>
   *
   * @param object the object to get the associated injection layer of.
   * @return the injection layer associated with the given object or the default {@link #ext()} layer.
   * @throws NullPointerException if the given object is null.
   */
  static @NonNull InjectionLayer<?> findLayerOf(@NonNull Object object) {
    return findLayerOf(object, InjectionLayer.ext());
  }

  /**
   * Finds the injection layer associated with the given object or returns the given default injection layer. The
   * following search rules apply (in order):
   * <ol>
   *   <li>If the given object is an {@link InjectionLayerHolder}, the layer stored in the holder is returned.
   *   <li>If the given object has a layer associated in the layer registry, that layer is returned.
   *   <li>If the given object is a class the associated class loader of the given class is checked.
   *   <li>If the given object is not a class loader the loader of the object class is checked.
   *   <li>If none of the above rules matches the given default layer is returned.
   * </ol>
   *
   * @param object the object to get the associated injection layer of.
   * @param def    the default layer to return if no layer can be found.
   * @return the injection layer associated with the given object or the given default layer.
   * @throws NullPointerException if the given object or default layer is null.
   */
  static @NonNull InjectionLayer<?> findLayerOf(@NonNull Object object, @NonNull InjectionLayer<?> def) {
    return InjectionLayerProvider.findLayerOf(object, def);
  }

  /**
   * Get the name of this injection layer, for identification purposes. The names boot and ext are reserved for internal
   * use only.
   *
   * @return the name of this layer.
   */
  @Override
  @NonNull String name();

  /**
   * Gets the underlying injector of this layer.
   *
   * @return the underlying injector of this layer.
   */
  @NonNull I injector();

  /**
   * Convince method to create or get the instance of the given class type.
   *
   * @param type the type of the element to get.
   * @param <T>  the type of the class modeled by the given class object.
   * @return the constructed instance of the class type, may be null.
   * @throws NullPointerException if the given type is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @UnknownNullability <T> T instance(@NonNull Class<T> type);

  /**
   * Convince method to create or get the instance of the given element.
   *
   * @param element the element of the type to get.
   * @param <T>     the type of the return value modeled by the given element.
   * @return the constructed instance of the class type, may be null.
   * @throws NullPointerException if the given element is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @UnknownNullability <T> T instance(@NonNull Element element);

  /**
   * Convince method to create or get the instance of the given type, while allowing to specifically influence the
   * injection context.
   *
   * @param type    the type of the element to get.
   * @param builder the builder to configure the injection context for the operation.
   * @param <T>     the type of the return value modeled by the given element.
   * @return the constructed instance of the class type, may be null.
   * @throws NullPointerException if the given type or builder is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @UnknownNullability <T> T instance(@NonNull Class<T> type, @NonNull Consumer<InjectionContext.Builder> builder);

  /**
   * Installs the binding constructed by the given bindings constructor to this injection layer.
   *
   * @param constructor the constructor to install.
   * @throws NullPointerException if the given constructor is null or the constructor constructs a null value.
   */
  void install(@NonNull BindingConstructor constructor);

  /**
   * Installs the autoconfiguration bindings for the given component. For this method to work the autoconfiguration
   * bindings must be located at {@code autoconfigure/<component>.aero}.
   * <p>
   * This method binds the autoconfiguration bindings to the underlying injector of this layer.
   *
   * @param loader    the loader in which the file resource is located.
   * @param component the name of the component to load the autoconfiguration bindings of.
   * @throws NullPointerException if the given class loader or component name is null.
   * @throws AerogelException     if an I/O exception occurs while loading or closing the data stream.
   */
  void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component);

  /**
   * Makes this injection layer not closeable, returning the same layer if it is already not closeable.
   *
   * @return the same injection layer from the functionality, but not closeable.
   */
  @NonNull
  InjectionLayer<I> asUncloseable();

  /**
   * Registers this injection layer to the injection layer registry. Subsequent calls to
   * {@link InjectionLayer#findLayerOf(Object)} will be able to resolve the layer if the given object value is the exact
   * the same as given as one of the hints (exact as by using the {@code ==} compare operation).
   * <p>
   * Note that the layer lookup order is predictable based on the call order of register. If different layers are
   * registered for the same objects, the first layer to be registered will be found.
   *
   * @param hints the hints to use for the layer registration.
   * @return the same layer as used to call the method, for chaining.
   * @throws NullPointerException if the given hints array is null.
   */
  @NonNull
  InjectionLayer<I> register(@NonNull Object... hints);

  /**
   * Closes this injector and removes all leftover bindings (if any).
   *
   * @throws UnsupportedOperationException if this layer cannot be closed.
   */
  @Override
  void close();
}
