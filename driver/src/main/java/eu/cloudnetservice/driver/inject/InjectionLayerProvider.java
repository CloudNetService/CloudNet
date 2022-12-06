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
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.auto.AutoAnnotationRegistry;
import eu.cloudnetservice.common.StringUtil;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * Internal utility to provide injection layers.
 *
 * @since 4.0
 */
final class InjectionLayerProvider {

  private static InjectionLayer<Injector> boot;
  private static InjectionLayer<Injector> ext;

  private InjectionLayerProvider() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the singleton boot injection layer.
   *
   * @return the boot injection layer.
   */
  @SuppressWarnings("unchecked")
  public static @NonNull InjectionLayer<Injector> boot() {
    // check if the boot layer is already initialized
    if (boot != null) {
      return boot;
    }

    // get the first provided service or set the default instance
    var installedProvider = ServiceLoader.load(InjectionLayer.class).findFirst();
    InjectionLayerProvider.boot = installedProvider.orElseGet(() -> fresh("boot")).asUncloseable();

    // configure the ext layer from the boot layer
    InjectionLayerProvider.ext = child(boot, "ext").asUncloseable();

    // initialization complete
    return boot;
  }

  /**
   * Returns the singleton injection layer which should be used for all kinds of external component injection (like
   * plugins). The ext layer has all bindings of the boot layer present.
   *
   * @return the singleton ext injection layer.
   */
  public static @NonNull InjectionLayer<Injector> ext() {
    // ensure that the ext layer is present
    if (ext == null) {
      boot();
    }

    return ext;
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
   * @throws IllegalArgumentException if the given name is invalid.
   */
  public static @NonNull InjectionLayer<SpecifiedInjector> specifiedChild(
    @NonNull InjectionLayer<? extends Injector> parent,
    @NonNull String name,
    @NonNull BiConsumer<InjectionLayer<SpecifiedInjector>, SpecifiedInjector> configurator
  ) {
    var childInjector = parent.injector().newSpecifiedInjector();
    return configuredLayer(name, childInjector, ((Consumer<InjectionLayer<SpecifiedInjector>>) layer -> {
      var layerElement = InjectionLayer.LAYER_ELEMENT.requireName(name);
      layer.install(Bindings.fixed(layerElement, layer));
    }).andThen(layer -> configurator.accept(layer, childInjector)));
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
  public static @NonNull InjectionLayer<Injector> child(
    @NonNull InjectionLayer<Injector> parent,
    @NonNull String name
  ) {
    var childInjector = parent.injector().newChildInjector();
    return configuredLayer(name, childInjector, layer -> {
      var layerElement = InjectionLayer.LAYER_ELEMENT.requireName(name);
      layer.install(Bindings.fixed(layerElement, layer));
    });
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
  public static @NonNull InjectionLayer<Injector> fresh(@NonNull String name) {
    return configuredLayer(name, layer -> layer.install(Bindings.fixed(InjectionLayer.LAYER_ELEMENT, layer)));
  }

  /**
   * Provides a fresh injection layer, with no bindings configured to the returned injector. After construction the
   * layer is passed to the given consumer to configure it.
   *
   * @param name         the name of the injection layer.
   * @param configurator the configurator for the injection layer.
   * @return a new, configured injection layer with no configured bindings.
   * @throws NullPointerException     if the given name or configurator is null.
   * @throws IllegalArgumentException if the given name is invalid.
   */
  public static @NonNull InjectionLayer<Injector> configuredLayer(
    @NonNull String name,
    @NonNull Consumer<InjectionLayer<Injector>> configurator
  ) {
    return configuredLayer(name, Injector.newInjector(), configurator);
  }

  /**
   * Provides a fresh injection layer, with no bindings configured to the returned injector. After construction the
   * layer is passed to the given consumer to configure it.
   *
   * @param name         the name of the injection layer.
   * @param injector     the injector to use for the layer.
   * @param configurator the configurator for the injection layer.
   * @param <I>          the type of injector to use for the layer.
   * @return a new, configured injection layer with no configured bindings.
   * @throws NullPointerException     if the given name, injector or configurator is null.
   * @throws IllegalArgumentException if the given name is invalid.
   */
  private static @NonNull <I extends Injector> InjectionLayer<I> configuredLayer(
    @NonNull String name,
    @NonNull I injector,
    @NonNull Consumer<InjectionLayer<I>> configurator
  ) {
    validateName(name);

    // build and configure the layer
    var layer = new DefaultInjectionLayer<>(injector, AutoAnnotationRegistry.newRegistry(), name);
    configurator.accept(layer);

    // return the new layer
    return layer;
  }

  /**
   * Finds the injection layer associated with the given object or returns the given default injection layer. The
   * following search rules apply (in order):
   * <ol>
   *   <li>If the given object is an {@link InjectionLayerHolder}, the layer stored in the holder is returned.
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
  public static @NonNull InjectionLayer<?> findLayerOf(@NonNull Object object, @NonNull InjectionLayer<?> def) {
    // check if the given object is a layer holder
    if (object instanceof InjectionLayerHolder<?> layerHolder) {
      return layerHolder.injectionLayer();
    }

    // check if the given object is a class, in that case fall back to the class loader
    if (object instanceof Class<?> clazz) {
      return findLayerOf(clazz.getClassLoader(), def);
    }

    // only check the class loader of the given class if the given object is not a class loader
    if (!(object instanceof ClassLoader loader)) {
      var loader = object.getClass().getClassLoader();
      return findLayerOf(loader, def);
    }

    // if the given object is a class loader try checking the parent of the loader
    if (loader != ClassLoader.getSystemClassLoader() && loader.getParent() != null) {
      return findLayerOf(loader.getParent(), def);
    }

    // fall back to the given default layer
    return def;
  }

  /**
   * Validates the given layer name, throwing an exception if the name is invalid.
   *
   * @param name the name to validate.
   * @throws NullPointerException     if the given name is null.
   * @throws IllegalArgumentException if the given name is invalid.
   */
  private static void validateName(@NonNull String name) {
    // if the boot layer is not yet present everything is allowed
    if (boot == null || ext == null) {
      return;
    }

    // validate that the boot layer name is never used twice
    var normalizedName = StringUtil.toLower(name);
    if (normalizedName.equals("boot") || normalizedName.equals("ext")) {
      throw new IllegalArgumentException(
        "The \"boot\" or \"ext\" injection layer name is reserved and cannot be used twice");
    }

    // check that the name is not empty
    if (normalizedName.isEmpty()) {
      throw new IllegalArgumentException("Injection layer name cannot be empty");
    }
  }
}
