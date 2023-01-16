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
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A service property implementation which uses functional interfaces for setting / getting the property value. This
 * property might support getting or setting the value based on whether the read / write functions were set.
 *
 * @param <T> the type of the property.
 * @since 4.0
 */
public final class FunctionalServiceProperty<T> implements ServiceProperty<T> {

  private Function<ServiceInfoSnapshot, T> getFunction;
  private BiConsumer<ServiceInfoSnapshot, T> setConsumer;

  /**
   * Creates a new functional service property instance which doesn't support reading and writing.
   *
   * @param <T> the type of the property.
   * @return a new functional service property.
   */
  public static <T> @NonNull FunctionalServiceProperty<T> create() {
    return new FunctionalServiceProperty<>();
  }

  /**
   * Sets the reader function of this property to the given function. If a non-null value is given the property supports
   * reading the value from the underlying snapshot, if null is given calls to read will result in an exception.
   *
   * @param getFunction the read function to use for this property.
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull FunctionalServiceProperty<T> reader(@Nullable Function<ServiceInfoSnapshot, T> getFunction) {
    this.getFunction = getFunction;
    return this;
  }

  /**
   * Sets the write function of this property to the given function. If a non-null value is given the property supports
   * writing a value to the underlying snapshot, if null is given calls to write will result in an exception.
   *
   * @param setConsumer the write function to use for this property.
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull FunctionalServiceProperty<T> writer(@Nullable BiConsumer<ServiceInfoSnapshot, T> setConsumer) {
    this.setConsumer = setConsumer;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T read(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    if (this.getFunction == null) {
      throw new UnsupportedOperationException("Reading is not supported for this property");
    }

    return this.getFunction.apply(serviceInfoSnapshot);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value) {
    if (this.setConsumer == null) {
      throw new UnsupportedOperationException("Writing is not supported for this property");
    }

    this.setConsumer.accept(serviceInfoSnapshot, value);
  }
}
