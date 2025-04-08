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

package eu.cloudnetservice.driver.impl.document.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A type adapter factory implementation that first validates that the correct type can be provided by it and then calls
 * the given delegate function, passing the target gson instance to it.
 *
 * @param typeSupportChecker the check predicate if a type is supported by this factory.
 * @param delegateFactory    the delegated function to construct the actual type adapter.
 * @since 4.0
 */
record DelegateTypeAdapterFactory(
  @NonNull Predicate<Class<?>> typeSupportChecker,
  @NonNull Function<Gson, TypeAdapter<?>> delegateFactory
) implements TypeAdapterFactory {

  /**
   * Constructs a new factory that validates that the requested type is exactly the given base type before calling the
   * delegate function.
   *
   * @param baseClass       the base class that the requested type must be equal to.
   * @param delegateFactory the delegate factory to call when a type factory should be created.
   * @return a type adapter factory that validates that the exact type is requested before constructing the adapter.
   * @throws NullPointerException if the given base type or delegate factory is null.
   */
  public static @NonNull TypeAdapterFactory standardFactory(
    @NonNull Class<?> baseClass,
    @NonNull Function<Gson, TypeAdapter<?>> delegateFactory
  ) {
    return new DelegateTypeAdapterFactory(baseClass::equals, delegateFactory);
  }

  /**
   * Constructs a new factory that validates that the requested type is somewhere in the hierarchy of the given base
   * type before calling the delegate function.
   *
   * @param baseClass       the base class that the requested type must be in the hierarchy of.
   * @param delegateFactory the delegate factory to call when a type factory should be created.
   * @return a type adapter factory that validates that a hierarchy type is requested before constructing the adapter.
   * @throws NullPointerException if the given base type or delegate factory is null.
   */
  public static @NonNull TypeAdapterFactory hierarchyFactory(
    @NonNull Class<?> baseClass,
    @NonNull Function<Gson, TypeAdapter<?>> delegateFactory
  ) {
    return new DelegateTypeAdapterFactory(baseClass::isAssignableFrom, delegateFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable TypeAdapter<T> create(@NonNull Gson gson, @NonNull TypeToken<T> type) {
    var requestedType = type.getRawType();
    if (this.typeSupportChecker.test(requestedType)) {
      return (TypeAdapter<T>) this.delegateFactory.apply(gson);
    }

    return null;
  }
}
