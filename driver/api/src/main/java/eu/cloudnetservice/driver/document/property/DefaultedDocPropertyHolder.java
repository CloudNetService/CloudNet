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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * An implementation of a doc property holder that defaults all method calls to the underlying document that is returned
 * by the {@link #propertyHolder()} method.
 *
 * @since 4.0
 */
public interface DefaultedDocPropertyHolder extends DocPropertyHolder {

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> @UnknownNullability E readProperty(@NonNull DocProperty<E> property) {
    return property.readFrom(this.propertyHolder());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> @UnknownNullability E readPropertyOrDefault(@NonNull DocProperty<E> property, @Nullable E def) {
    var knownProperty = this.readProperty(property);
    return knownProperty == null ? def : knownProperty;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> @UnknownNullability E readPropertyOrGet(
    @NonNull DocProperty<E> property,
    @NonNull Supplier<? extends E> supplier
  ) {
    var knownProperty = this.readProperty(property);
    return knownProperty == null ? supplier.get() : knownProperty;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> @NonNull E readPropertyOrThrow(@NonNull DocProperty<E> property) {
    return this.readPropertyOrThrow(property, NoSuchElementException::new);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E, T extends Throwable> @NonNull E readPropertyOrThrow(
    @NonNull DocProperty<E> property,
    @NonNull Supplier<? extends T> exceptionSupplier
  ) throws T {
    var value = this.readProperty(property);
    if (value == null) {
      throw exceptionSupplier.get();
    }
    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> boolean propertyPresent(@NonNull DocProperty<E> property) {
    return this.propertyHolder().contains(property.key());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <E> boolean propertyAbsent(@NonNull DocProperty<E> property) {
    return !this.propertyHolder().contains(property.key());
  }

  /**
   * An implementation of a mutable doc property holder that defaults all method calls to the underlying document that
   * is returned by the {@link #propertyHolder()} method.
   *
   * @param <S> the type that implements this interface, for chainable method return types.
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  interface Mutable<S extends DocPropertyHolder.Mutable<S>>
    extends DefaultedDocPropertyHolder, DocPropertyHolder.Mutable<S> {

    /**
     * {@inheritDoc}
     */
    @Override
    default <E> @NonNull S writeProperty(@NonNull DocProperty<E> property, @Nullable E value) {
      property.writeTo(this.propertyHolder(), value);
      return (S) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NonNull <E> S writePropertyIfAbsent(@NonNull DocProperty<E> property, @Nullable E value) {
      if (this.propertyAbsent(property)) {
        property.writeTo(this.propertyHolder(), value);
      }
      return (S) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NonNull <E> S writePropertyIfAbsent(
      @NonNull DocProperty<E> property,
      @NonNull Supplier<? extends E> valueSupplier
    ) {
      if (this.propertyAbsent(property)) {
        property.writeTo(this.propertyHolder(), valueSupplier.get());
      }
      return (S) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NonNull <E> S writePropertyIfPresent(@NonNull DocProperty<E> property, @Nullable E val) {
      if (this.propertyPresent(property)) {
        property.writeTo(this.propertyHolder(), val);
      }
      return (S) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NonNull <E> S writePropertyIfPresent(
      @NonNull DocProperty<E> property,
      @NonNull Supplier<? extends E> valueSupplier
    ) {
      if (this.propertyPresent(property)) {
        property.writeTo(this.propertyHolder(), valueSupplier.get());
      }
      return (S) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <E> @Nullable E removeProperty(@NonNull DocProperty<E> property) {
      if (property.readOnly()) {
        throw new UnsupportedOperationException("Cannot remove read-only property");
      }

      var value = this.readProperty(property);
      this.propertyHolder().remove(property.key());
      return value;
    }

    /**
     * A subclass of a mutable doc property holder which allows to modify the underlying property document of this
     * holder easily.
     *
     * @param <S> the type that implements this interface, for chainable method return types.
     * @since 4.0
     */
    interface WithDirectModifier<S extends DocPropertyHolder.Mutable<S>> extends DefaultedDocPropertyHolder.Mutable<S> {

      /**
       * Passes the underlying mutable document of this holder to the given modifier function. It is up to the function
       * which changes are applied to the document. All changes made inside the modifier (without copying the passed
       * document) will reflect into this holder.
       *
       * @param propertyModifier the modifier function to apply to the underlying document.
       * @return this property holder, for chaining.
       * @throws NullPointerException if the given modifier function is null.
       */
      default @NonNull S modifyProperties(@NonNull Consumer<Document.Mutable> propertyModifier) {
        propertyModifier.accept(this.propertyHolder());
        return (S) this;
      }

      /**
       * Applies the given operator to the underlying document and copies over all items from the underlying document
       * into this holder. Properties that were added prior to the method invocation will be removed.
       *
       * @param modifier the modifier operator to apply to the underlying document.
       * @return this property holder, for chaining.
       * @throws NullPointerException if the given modifier operator is null.
       */
      default @NonNull S modifyAndSetProperties(@NonNull Function<Document.Mutable, ? extends Document> modifier) {
        var modificationResult = modifier.apply(this.propertyHolder());
        if (modificationResult != null) {
          // remove all properties from the current document
          this.propertyHolder().clear();

          // send over all properties from the document result into the underlying document
          var send = modificationResult.send();
          this.propertyHolder().receive(send);
        }

        return (S) this;
      }
    }
  }
}
