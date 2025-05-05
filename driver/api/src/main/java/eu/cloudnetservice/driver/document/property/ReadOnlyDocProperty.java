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
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A doc property implementation that only allows reading the value from a document but not setting the value. All
 * methods that wrap this property are implemented in a way that the read-only flag is not lost in a call chain.
 *
 * @param downstream the downstream doc property to delegate all methods (except writing) to.
 * @param <E>        the type which gets read/written by this property.
 * @since 4.0
 */
record ReadOnlyDocProperty<E>(@NonNull DocProperty<E> downstream) implements DocProperty<E> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String key() {
    return this.downstream.key();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readOnly() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> asReadOnly() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> withDefault(@Nullable E def) {
    // wrap the property with the given default value, then make it read-only again to not lose the state
    var defaultingProperty = new DefaultingDocProperty<>(this.downstream, def);
    return defaultingProperty.asReadOnly();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <V> DocProperty<V> withReadRewrite(@NonNull Function<E, V> rewriteFunction) {
    return this.withReadWriteRewrite(rewriteFunction, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <V> DocProperty<V> withReadWriteRewrite(
    @NonNull Function<E, V> readRewriteFunction,
    @Nullable Function<V, E> writeRewriteFunction
  ) {
    // we wrap the resulting function always as read only as this function was read only before
    // to ensure that we don't lose that state - because of this we can always pass null as the
    // write rewrite function (the write method will not be called anyway)
    var rewritingProperty = new RewritingDocProperty<>(this.downstream, readRewriteFunction, null);
    return rewritingProperty.asReadOnly();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable E readFrom(@NonNull Document document) {
    return this.downstream.readFrom(document);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> writeTo(@NonNull Document.Mutable document, @Nullable E value) {
    throw new UnsupportedOperationException("Property is read-only");
  }
}
