/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.service.property;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a service property which wraps another property but calls a transforming function before passing the value
 * down to the wrapped service property.
 *
 * @param <I> the type of the wrapped service property (the transformation target).
 * @param <T> the input type of this property, before transformation.
 */
public final class TransformingServiceProperty<I, T> implements ServiceProperty<T> {

  private final ServiceProperty<I> wrapped;

  private BiFunction<ServiceInfoSnapshot, I, T> getModifier;
  private BiFunction<ServiceInfoSnapshot, T, I> setModifier;

  /**
   * Constructs a new transforming service property instance.
   *
   * @param wrapped the wrapper service property.
   * @throws NullPointerException if the given wrapped property is null.
   */
  private TransformingServiceProperty(@NonNull ServiceProperty<I> wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Constructs a new transforming service property instance by wrapping the given service property.
   *
   * @param wrapped the service property to wrap.
   * @param <I>     the type of the wrapped service property (the transformation target).
   * @param <T>     the input type of this property, before transformation.
   * @return a new transforming service property wrapping the given property.
   * @throws NullPointerException if the given property is null.
   */
  public static <I, T> @NonNull TransformingServiceProperty<I, T> wrap(@NonNull ServiceProperty<I> wrapped) {
    return new TransformingServiceProperty<>(wrapped);
  }

  /**
   * Sets the modifier to apply to the value of the wrapped service property before returning it. If the given modifier
   * is null this property will no longer support reading the value from the service snapshot and all calls to the get
   * method will result in an exception.
   *
   * @param mod the modifier to apply after getting the value from the wrapped property.
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull TransformingServiceProperty<I, T> modifyGet(@Nullable BiFunction<ServiceInfoSnapshot, I, T> mod) {
    this.getModifier = mod;
    return this;
  }

  /**
   * Sets the modifier to apply to this service property before passing it to the wrapped property. If the given
   * modifier is null this property will no longer support writing the value to the service snapshot and all calls to
   * the write method will result in an exception.
   *
   * @param mod the modifier to apply before writing the given value into the service snapshot.
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull TransformingServiceProperty<I, T> modifySet(@Nullable BiFunction<ServiceInfoSnapshot, T, I> mod) {
    this.setModifier = mod;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T read(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    if (this.getModifier == null) {
      throw new UnsupportedOperationException("Reading is not supported for this property");
    }

    var wrappedValue = this.wrapped.read(serviceInfoSnapshot);
    return wrappedValue == null ? null : this.getModifier.apply(serviceInfoSnapshot, wrappedValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value) {
    if (this.setModifier == null) {
      throw new UnsupportedOperationException("Writing is not supported for this property");
    }

    this.wrapped.write(serviceInfoSnapshot, this.setModifier.apply(serviceInfoSnapshot, value));
  }
}
