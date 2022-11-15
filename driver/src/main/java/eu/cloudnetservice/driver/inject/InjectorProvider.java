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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.AutoAnnotationRegistry;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * SPI to use or modify the injection behaviour of the current component. This class provides some convince methods in
 * order to improve the injection workflow.
 * <p>
 * This class can be injected as well.
 *
 * @see InjectorProviderHolder
 * @since 4.0
 */
@Singleton
public interface InjectorProvider {

  /**
   * The file name format for the autoconfigure files. This format will be used when calling
   * {@code installAutoConfigureBindings} and gets formatted with the given component name to the method.
   */
  String AUTO_CONFIGURE_FILE_NAME_FORMAT = "autoconfigure/%s.aero";
  /**
   * The element which represents the injector provider, no special properties are applied.
   */
  Element PROVIDER_ELEMENT = Element.forType(InjectorProvider.class);

  /**
   * Gets the underlying injector of this provider. The returned injector should always be a root injector, never a
   * child injector. If the injector is needed which is currently injecting a class/method/field, the injector class
   * should be requested over this provider.
   *
   * @return the underlying injector of this provider, should be the root injector.
   */
  @NonNull Injector injector();

  /**
   * Gets the auto annotation registry instance. The instance can be used for all kinds of calls as it's not bound to a
   * specific injector.
   *
   * @return the underlying auto inject registry.
   */
  @NonNull AutoAnnotationRegistry autoRegistry();

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
   * Installs the autoconfiguration bindings for the given component. For this method to work the autoconfiguration
   * bindings must be located at {@code autoconfigure/<component>.aero}.
   * <p>
   * This method binds the autoconfiguration bindings to the underlying injector of this provider. If a specific
   * injector is needed use {@link #installAutoConfigureBindings(Injector, ClassLoader, String)} instead.
   *
   * @param loader    the loader in which the file resource is located.
   * @param component the name of the component to load the autoconfiguration bindings of.
   * @throws NullPointerException if the given class loader or component name is null.
   * @throws AerogelException     if an I/O exception occurs while loading or closing the data stream.
   */
  void installAutoConfigureBindings(@NonNull ClassLoader loader, @NonNull String component);

  /**
   * Installs the autoconfiguration bindings for the given component to the given injector. For this method to work the
   * autoconfiguration bindings must be located at {@code autoconfigure/<component>.aero}.
   *
   * @param injector  the injector to install the bindings to.
   * @param loader    the loader in which the file resource is located.
   * @param component the name of the component to load the autoconfiguration bindings of.
   * @throws NullPointerException if the given injector, class loader or component name is null.
   * @throws AerogelException     if an I/O exception occurs while loading or closing the data stream.
   */
  void installAutoConfigureBindings(@NonNull Injector injector, @NonNull ClassLoader loader, @NonNull String component);
}
