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

package eu.cloudnetservice.driver.document.property;

import eu.cloudnetservice.driver.document.Document;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A holder for doc properties that supports various read and write operations to the underlying document.
 *
 * @since 4.0
 */
public interface DocPropertyHolder {

  /**
   * Reads the value of the given property from the underlying document.
   *
   * @param property the property to read.
   * @param <E>      the type which gets read/written by the given property.
   * @return the value of the property.
   * @throws NullPointerException if the given property is null.
   */
  @UnknownNullability <E> E readProperty(@NonNull DocProperty<E> property);

  /**
   * Reads the given property from the underlying document or returns the given default value if the property is not set
   * or set to {@code null}.
   *
   * @param property the property to read.
   * @param def      the default value to return if the property value is null.
   * @param <E>      the type which gets read/written by the given property.
   * @return the value of the property or the given default value.
   * @throws NullPointerException if the given property is null.
   */
  @UnknownNullability <E> E readPropertyOrDefault(@NonNull DocProperty<E> property, @Nullable E def);

  /**
   * Reads the given property from the underlying document or returns the default value computed by the given supplier
   * if the property is not set or set to {@code null}.
   *
   * @param property the property to read.
   * @param supplier the supplier for the default value to return if the property value is null.
   * @param <E>      the type which gets read/written by the given property.
   * @return the value of the property or the given default value.
   * @throws NullPointerException if the given property or default supplier is null.
   */
  @UnknownNullability <E> E readPropertyOrGet(
    @NonNull DocProperty<E> property,
    @NonNull Supplier<? extends E> supplier);

  /**
   * Reads the given property from the underlying document or throws a {@link NoSuchElementException} if the value is
   * not set or set to {@code null}.
   *
   * @param property the property to read.
   * @param <E>      the type which gets read/written by the given property.
   * @return the value of the property.
   * @throws NullPointerException   if the given property is null.
   * @throws NoSuchElementException if the property is not set or set to null.
   */
  @NonNull <E> E readPropertyOrThrow(@NonNull DocProperty<E> property);

  /**
   * Reads the given property from the underlying document or throws the exception that is computed by the given
   * exception supplier if the value is not set or set to {@code null}.
   *
   * @param property          the property to read.
   * @param exceptionSupplier the supplier that produces the exception to throw in case the property has no value set.
   * @param <E>               the type which gets read/written by the given property.
   * @param <T>               the type of exception to throw in case the property has no value set.
   * @return the value of the property.
   * @throws NullPointerException if the given property or exception supplier is null.
   * @throws T                    if the property is not set or set to null.
   */
  @NonNull <E, T extends Throwable> E readPropertyOrThrow(
    @NonNull DocProperty<E> property,
    @NonNull Supplier<? extends T> exceptionSupplier) throws T;

  /**
   * Get if a value is associated with the given property. Values that are {@code null} are included as present.
   *
   * @param property the property to check for.
   * @param <E>      the type which gets read/written by the given property.
   * @return true if a value is associated with the property, false otherwise.
   * @throws NullPointerException if the given property is null.
   */
  <E> boolean propertyPresent(@NonNull DocProperty<E> property);

  /**
   * Get if no value is associated with the given property. Values that are {@code null} are included as present.
   *
   * @param property the property to check for.
   * @param <E>      the type which gets read/written by the given property.
   * @return true if no value is associated with the property, false otherwise.
   * @throws NullPointerException if the given property is null.
   */
  <E> boolean propertyAbsent(@NonNull DocProperty<E> property);

  /**
   * Get the underlying document that all read and write operations are delegated to.
   *
   * @return the underlying document.
   */
  @NonNull Document propertyHolder();

  /**
   * A mutable version of a doc property holder which allows modifications to it.
   *
   * @param <S> the type that implements this interface, for chainable method return types.
   * @since 4.0
   */
  interface Mutable<S extends Mutable<S>> extends DocPropertyHolder {

    /**
     * Writes the given property and value into the underlying document.
     *
     * @param property the property to write.
     * @param value    the value to associate with the given property.
     * @param <E>      the type which gets read/written by the given property.
     * @return this property holder, for chaining.
     * @throws NullPointerException          if the given doc property is null.
     * @throws UnsupportedOperationException if the given doc property does not support writing.
     */
    @NonNull <E> S writeProperty(@NonNull DocProperty<E> property, @Nullable E value);

    /**
     * Writes the given property and value into the underlying document if no other value is associated yet with the
     * given property.
     *
     * @param property the property to write.
     * @param value    the value to associate with the given property in case no other value is associated yet.
     * @param <E>      the type which gets read/written by the given property.
     * @return this property holder, for chaining.
     * @throws NullPointerException          if the given doc property is null.
     * @throws UnsupportedOperationException if the given doc property does not support writing.
     */
    @NonNull <E> S writePropertyIfAbsent(@NonNull DocProperty<E> property, @Nullable E value);

    /**
     * Writes the given property and computed value into the underlying document if no other value is associated yet
     * with the given property.
     *
     * @param property      the property to write.
     * @param valueSupplier a supplier for the value to associate if no other value is yet associated.
     * @param <E>           the type which gets read/written by the given property.
     * @return this property holder, for chaining.
     * @throws NullPointerException          if the given doc property or value supplier is null.
     * @throws UnsupportedOperationException if the given doc property does not support writing.
     */
    @NonNull <E> S writePropertyIfAbsent(
      @NonNull DocProperty<E> property,
      @NonNull Supplier<? extends E> valueSupplier);

    /**
     * Writes the given property and value into the underlying document if another value is already associated with the
     * given property.
     *
     * @param property the property to write.
     * @param value    the value to associate with the given property in case another value is already associated.
     * @param <E>      the type which gets read/written by the given property.
     * @return this property holder, for chaining.
     * @throws NullPointerException          if the given doc property is null.
     * @throws UnsupportedOperationException if the given doc property does not support writing.
     */
    @NonNull <E> S writePropertyIfPresent(@NonNull DocProperty<E> property, @Nullable E value);

    /**
     * Writes the given property and computed value into the underlying document if another value is already associated
     * with the given property.
     *
     * @param property      the property to write.
     * @param valueSupplier a supplier for the value to associate if another value is already associated.
     * @param <E>           the type which gets read/written by the given property.
     * @return this property holder, for chaining.
     * @throws NullPointerException          if the given doc property or value supplier is null.
     * @throws UnsupportedOperationException if the given doc property does not support writing.
     */
    @NonNull <E> S writePropertyIfPresent(
      @NonNull DocProperty<E> property,
      @NonNull Supplier<? extends E> valueSupplier);

    /**
     * Removes the given doc property from the underlying document.
     *
     * @param property the property to remove.
     * @param <E>      the type which gets read/written by the given property.
     * @return the old value associated with the given doc property.
     * @throws NullPointerException          if the given property is null.
     * @throws UnsupportedOperationException if the given doc property is read-only.
     */
    @UnknownNullability <E> E removeProperty(@NonNull DocProperty<E> property);

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull Document.Mutable propertyHolder();
  }
}
