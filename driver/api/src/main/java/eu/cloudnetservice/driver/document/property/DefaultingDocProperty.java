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
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A doc property that injects the given default value when reading or writing a null value.
 *
 * @param downstream the doc property to delegate all operations to (possibly after injecting the default value).
 * @param def        the default value to inject into the chain if needed, may be null.
 * @param <E>        the type which gets read/written by this property.
 * @since 4.0
 */
record DefaultingDocProperty<E>(@NonNull DocProperty<E> downstream, @Nullable E def) implements DocProperty<E> {

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
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> asReadOnly() {
    return new ReadOnlyDocProperty<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> withDefault(@Nullable E def) {
    return Objects.equals(this.def, def) ? this : new DefaultingDocProperty<>(this.downstream, def);
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
    // if the given write rewrite function is null we should wrap the property in a read only one
    // this clearly indicates to the end user that there is no way that he can write the property to a document
    var rewritingProperty = new RewritingDocProperty<>(this, readRewriteFunction, writeRewriteFunction);
    return writeRewriteFunction == null ? rewritingProperty.asReadOnly() : rewritingProperty;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable E readFrom(@NonNull Document document) {
    var downstreamValue = this.downstream.readFrom(document);
    return downstreamValue == null ? this.def : downstreamValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<E> writeTo(@NonNull Document.Mutable document, @Nullable E value) {
    this.downstream.writeTo(document, value == null ? this.def : value);
    return this;
  }
}
