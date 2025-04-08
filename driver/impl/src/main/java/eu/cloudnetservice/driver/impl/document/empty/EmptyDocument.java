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

package eu.cloudnetservice.driver.impl.document.empty;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.SerialisationStyle;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import io.leangen.geantyref.TypeToken;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A document implementation that just ignores all calls made to it.
 *
 * @since 4.0
 */
public final class EmptyDocument implements Document.Mutable, DefaultedDocPropertyHolder.Mutable<Document.Mutable> {

  /**
   * The jvm static instance of the empty document. There should never be a different instance of this class during the
   * jvm lifetime.
   * <p>
   * Note: do not use this field directly, prefer {@link Document#emptyDocument()} instead.
   */
  public static final Document.Mutable INSTANCE = new EmptyDocument();

  /**
   * Constructs an empty document instance. This constructor is public to all serialization of this document and should
   * not be called. Obtain the singleton instance of this implementation via {@link Document#emptyDocument()}.
   */
  public EmptyDocument() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String factoryName() {
    return "empty";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean empty() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int elementCount() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(@NonNull String key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsNonNull(@NonNull String key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocumentSend send() {
    return EmptyDocumentSend.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document immutableCopy() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable mutableCopy() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Set<String> keys() {
    return Set.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Collection<? extends Element> elements() {
    return Set.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Type type) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Class<T> type) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull TypeToken<T> type) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Type type, @Nullable T def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Class<T> type, @Nullable T def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull TypeToken<T> type, @Nullable T def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability Document readDocument(@NonNull String key, @Nullable Document def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Document.@UnknownNullability Mutable readMutableDocument(@NonNull String key, @Nullable Document.Mutable def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte getByte(@NonNull String key, byte def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getShort(@NonNull String key, short def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInt(@NonNull String key, int def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLong(@NonNull String key, long def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getFloat(@NonNull String key, float def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDouble(@NonNull String key, double def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getBoolean(@NonNull String key, boolean def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability String getString(@NonNull String key, @Nullable String def) {
    return def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull Path path, @NonNull SerialisationStyle style) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull OutputStream stream, @NonNull SerialisationStyle style) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull Appendable appendable, @NonNull SerialisationStyle style) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull DataBuf.Mutable dataBuf, @NonNull SerialisationStyle style) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String serializeToString(@NonNull SerialisationStyle style) {
    throw new UnsupportedOperationException("Not supported on empty document");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable clear() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable remove(@NonNull String key) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable receive(@NonNull DocumentSend send) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable appendNull(@NonNull String key) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable appendTree(@Nullable Object value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull Document document) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Object value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Number value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Boolean value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable String value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Document value) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable propertyHolder() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof EmptyDocument;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "[empty]";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeExternal(@NonNull ObjectOutput out) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readExternal(@NonNull ObjectInput in) {
  }
}
