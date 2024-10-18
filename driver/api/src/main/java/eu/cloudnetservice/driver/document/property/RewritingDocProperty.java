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
 * A doc property implementation that calls the given functions when reading/writing the value from a document to
 * convert it into a different value (for example when parsing).
 *
 * @param upstream             the upstream doc property to delegate all method calls to.
 * @param readRewriteFunction  the function to apply when a non-null value was read from a document.
 * @param writeRewriteFunction the function to apply when a non-null value is written to a document, can be null.
 * @param <I>                  the type of the input property the actual read/write calls are delegated to.
 * @param <O>                  the output type after passing the values through the convert functions.
 * @since 4.0
 */
record RewritingDocProperty<I, O>(
  @NonNull DocProperty<I> upstream,
  @NonNull Function<I, O> readRewriteFunction,
  @Nullable Function<O, I> writeRewriteFunction
) implements DocProperty<O> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String key() {
    return this.upstream.key();
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
  public @NonNull DocProperty<O> asReadOnly() {
    return new ReadOnlyDocProperty<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<O> withDefault(@Nullable O def) {
    return new DefaultingDocProperty<>(this, def);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <V> DocProperty<V> withReadRewrite(@NonNull Function<O, V> rewriteFunction) {
    return this.withReadWriteRewrite(rewriteFunction, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <V> DocProperty<V> withReadWriteRewrite(
    @NonNull Function<O, V> readRewriteFunction,
    @Nullable Function<V, O> writeRewriteFunction
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
  public @Nullable O readFrom(@NonNull Document document) {
    // read the value from the upstream property and pass it the rewrite function unless it's null
    // we're not rewriting null through the function at all as explained in the documentation
    var upstreamValue = this.upstream.readFrom(document);
    return upstreamValue == null ? null : this.readRewriteFunction.apply(upstreamValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocProperty<O> writeTo(@NonNull Document.Mutable document, @Nullable O value) {
    // check if this property is able to write the value to the document
    // in normal cases this call should not happen if no rewrite function is given as this property should be
    // wrapped as read only, but we need to make sure in case someone gets around that
    if (this.writeRewriteFunction == null) {
      throw new UnsupportedOperationException("Unable to write property to document - missing write rewrite function");
    }

    // write null directly without passing it through the rewrite function first
    if (value == null) {
      this.upstream.writeTo(document, null);
    } else {
      var convertedValue = this.writeRewriteFunction.apply(value);
      this.upstream.writeTo(document, convertedValue);
    }

    return this;
  }
}
