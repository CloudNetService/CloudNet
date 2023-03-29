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

package eu.cloudnetservice.driver.document.empty;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.SerialisationStyle;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import io.leangen.geantyref.TypeToken;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

public final class EmptyDocument implements Document.Mutable, DefaultedDocPropertyHolder.Mutable<Document.Mutable> {

  public static final Document.Mutable INSTANCE = new EmptyDocument();

  private EmptyDocument() {
  }

  @Override
  public boolean empty() {
    return true;
  }

  @Override
  public int elementCount() {
    return 0;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return false;
  }

  @Override
  public @NonNull DocumentSend send() {
    return EmptyDocumentSend.INSTANCE;
  }

  @Override
  public @NonNull Document immutableCopy() {
    return this;
  }

  @Override
  public @NonNull Document.Mutable mutableCopy() {
    return this;
  }

  @Override
  public @Unmodifiable @NonNull Set<String> keys() {
    return Set.of();
  }

  @Override
  public @Unmodifiable @NonNull Collection<? extends Element> elements() {
    return Set.of();
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Type type) {
    return null;
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Class<T> type) {
    return null;
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull TypeToken<T> type) {
    return null;
  }

  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Type type, @Nullable T def) {
    return def;
  }

  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Class<T> type, @Nullable T def) {
    return def;
  }

  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull TypeToken<T> type, @Nullable T def) {
    return def;
  }

  @Override
  public @UnknownNullability Document readDocument(@NonNull String key, @Nullable Document def) {
    return def;
  }

  @Override
  public Document.@UnknownNullability Mutable readMutableDocument(@NonNull String key, @Nullable Document.Mutable def) {
    return def;
  }

  @Override
  public byte getByte(@NonNull String key, byte def) {
    return def;
  }

  @Override
  public short getShort(@NonNull String key, short def) {
    return def;
  }

  @Override
  public int getInt(@NonNull String key, int def) {
    return def;
  }

  @Override
  public long getLong(@NonNull String key, long def) {
    return def;
  }

  @Override
  public float getFloat(@NonNull String key, float def) {
    return def;
  }

  @Override
  public double getDouble(@NonNull String key, double def) {
    return def;
  }

  @Override
  public boolean getBoolean(@NonNull String key, boolean def) {
    return def;
  }

  @Override
  public @UnknownNullability String getString(@NonNull String key, @Nullable String def) {
    return def;
  }

  @Override
  public void writeTo(@NonNull Path path, @NonNull SerialisationStyle style) {
  }

  @Override
  public void writeTo(@NonNull OutputStream stream, @NonNull SerialisationStyle style) {
  }

  @Override
  public void writeTo(@NonNull Appendable appendable, @NonNull SerialisationStyle style) {
  }

  @Override
  public void writeTo(@NonNull DataBuf.Mutable dataBuf, @NonNull SerialisationStyle style) {
    dataBuf.writeString("empty");
  }

  @Override
  public @NonNull String serializeToString(@NonNull SerialisationStyle style) {
    throw new UnsupportedOperationException("Not supported on empty document");
  }

  @Override
  public @NonNull Document.Mutable clear() {
    return this;
  }

  @Override
  public @NonNull Document.Mutable remove(@NonNull String key) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable receive(@NonNull DocumentSend send) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable appendNull(@NonNull String key) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable appendTree(@Nullable Object value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull Document document) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Object value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Number value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Boolean value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable String value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable append(@NonNull String key, @Nullable Document value) {
    return this;
  }

  @Override
  public @NonNull Document.Mutable propertyHolder() {
    return this;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof EmptyDocument;
  }
}
