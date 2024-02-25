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

package eu.cloudnetservice.driver.network.http.annotation.parser;

import eu.cloudnetservice.driver.network.http.HttpComponent;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A parser which can convert and register annotated http elements, supporting custom annotations as well.
 *
 * @param <T> the type of http component associated with this parser.
 * @since 4.0
 */
public interface HttpAnnotationParser<T extends HttpComponent<T>> {

  /**
   * Get the http component which is associated with this parser. All handlers parsed using this parser will get
   * registered into that component.
   *
   * @return the http component associated with this parser.
   */
  @NonNull T httpComponent();

  /**
   * Get an unmodifiable view of all annotation processors which were registered to this parser.
   *
   * @return all annotation processors registered to this parser.
   */
  @UnmodifiableView
  @NonNull Collection<HttpAnnotationProcessor> annotationProcessors();

  /**
   * Registers an annotation processor to this parser.
   *
   * @param processor the processor to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given processor is null.
   */
  @NonNull HttpAnnotationParser<T> registerAnnotationProcessor(@NonNull HttpAnnotationProcessor processor);

  /**
   * Unregisters an annotation processor from this parser if previously registered.
   *
   * @param processor the processor to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given processor is null.
   */
  @NonNull HttpAnnotationParser<T> unregisterAnnotationProcessor(@NonNull HttpAnnotationProcessor processor);

  /**
   * Unregisters all annotation processors from this parser whose classes were loaded by the given class loader.
   *
   * @param classLoader the loader of the processor classes to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull HttpAnnotationParser<T> unregisterAnnotationProcessors(@NonNull ClassLoader classLoader);

  /**
   * Parses all non-static http handlers methods annotated with {@code @HttpRequestHandler} in the given class instance.
   * This method will call all previously registered annotation processors and build context preprocessors from them,
   * then register the final parsed handler to the http component associated with this parser.
   *
   * @param handlerClass the class in which the handler methods to register are located.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException        if the given handler instance is null.
   * @throws IllegalArgumentException    if annotating a static method, not taking a context as the first arg or if the
   *                                     annotation defines no paths or http methods to handle.
   * @throws InaccessibleObjectException if a http handler method is not accessible.
   */
  @NonNull HttpAnnotationParser<T> parseAndRegister(@NonNull Class<?> handlerClass);

  /**
   * Parses all non-static http handlers methods annotated with {@code @HttpRequestHandler} in the given class instance.
   * This method will call all previously registered annotation processors and build context preprocessors from them,
   * then register the final parsed handler to the http component associated with this parser.
   *
   * @param handlerInstance the instance of the class in which the handler methods to register are located.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException        if the given handler instance is null.
   * @throws IllegalArgumentException    if annotating a static method, not taking a context as the first arg or if the
   *                                     annotation defines no paths or http methods to handle.
   * @throws InaccessibleObjectException if a http handler method is not accessible.
   */
  @NonNull HttpAnnotationParser<T> parseAndRegister(@NonNull Object handlerInstance);
}
