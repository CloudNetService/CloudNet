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

package eu.cloudnetservice.driver.document;

import eu.cloudnetservice.driver.document.empty.EmptyDocument;
import eu.cloudnetservice.driver.document.property.DocPropertyHolder;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import io.leangen.geantyref.TypeToken;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

public interface Document extends DocPropertyHolder, Cloneable, Serializable {

  static @NonNull Document.Mutable emptyDocument() {
    return EmptyDocument.INSTANCE;
  }

  static @NonNull Document.Mutable newJsonDocument() {
    return newDocument(DocumentFactory.json());
  }

  static @NonNull Document.Mutable newDocument(@NonNull DocumentFactory factory) {
    return factory.newDocument();
  }

  boolean empty();

  int elementCount();

  boolean contains(@NonNull String key);

  @NonNull DocumentSend send();

  @CheckReturnValue
  @NonNull Document immutableCopy();

  @CheckReturnValue
  @NonNull Document.Mutable mutableCopy();

  @Unmodifiable
  @NonNull Set<String> keys();

  @Unmodifiable
  @NonNull Collection<? extends Element> elements();

  @UnknownNullability <T> T toInstanceOf(@NonNull Type type);

  @UnknownNullability <T> T toInstanceOf(@NonNull Class<T> type);

  @UnknownNullability <T> T toInstanceOf(@NonNull TypeToken<T> type);

  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Type type) {
    return this.readObject(key, type, null);
  }

  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Class<T> type) {
    return this.readObject(key, type, null);
  }

  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull TypeToken<T> type) {
    return this.readObject(key, type, null);
  }

  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Type type, @Nullable T def);

  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Class<T> type, @Nullable T def);

  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull TypeToken<T> type, @Nullable T def);

  default @UnknownNullability Document readDocument(@NonNull String key) {
    return this.readDocument(key, Document.emptyDocument());
  }

  @UnknownNullability Document readDocument(@NonNull String key, @Nullable Document def);

  default Document.@UnknownNullability Mutable readMutableDocument(@NonNull String key) {
    return this.readMutableDocument(key, null);
  }

  Document.@UnknownNullability Mutable readMutableDocument(@NonNull String key, @Nullable Document.Mutable def);

  default byte getByte(@NonNull String key) {
    return this.getByte(key, (byte) 0);
  }

  default short getShort(@NonNull String key) {
    return this.getShort(key, (short) 0);
  }

  default int getInt(@NonNull String key) {
    return this.getInt(key, 0);
  }

  default long getLong(@NonNull String key) {
    return this.getLong(key, 0);
  }

  default float getFloat(@NonNull String key) {
    return this.getFloat(key, 0);
  }

  default double getDouble(@NonNull String key) {
    return this.getDouble(key, 0);
  }

  default boolean getBoolean(@NonNull String key) {
    return this.getBoolean(key, false);
  }

  default @UnknownNullability String getString(@NonNull String key) {
    return this.getString(key, null);
  }

  byte getByte(@NonNull String key, byte def);

  short getShort(@NonNull String key, short def);

  int getInt(@NonNull String key, int def);

  long getLong(@NonNull String key, long def);

  float getFloat(@NonNull String key, float def);

  double getDouble(@NonNull String key, double def);

  boolean getBoolean(@NonNull String key, boolean def);

  @UnknownNullability String getString(@NonNull String key, @Nullable String def);

  default void writeTo(@NonNull Path path) {
    this.writeTo(path, SerialisationStyle.PRETTY);
  }

  default void writeTo(@NonNull OutputStream stream) {
    this.writeTo(stream, SerialisationStyle.PRETTY);
  }

  default void writeTo(@NonNull Appendable appendable) {
    this.writeTo(appendable, SerialisationStyle.PRETTY);
  }

  default void writeTo(@NonNull DataBuf.Mutable dataBuf) {
    this.writeTo(dataBuf, SerialisationStyle.COMPACT);
  }

  default @NonNull String serializeToString() {
    return this.serializeToString(SerialisationStyle.PRETTY);
  }

  void writeTo(@NonNull Path path, @NonNull SerialisationStyle style);

  void writeTo(@NonNull OutputStream stream, @NonNull SerialisationStyle style);

  void writeTo(@NonNull Appendable appendable, @NonNull SerialisationStyle style);

  void writeTo(@NonNull DataBuf.Mutable dataBuf, @NonNull SerialisationStyle style);

  @NonNull String serializeToString(@NonNull SerialisationStyle style);

  interface Mutable extends Document, DocPropertyHolder.Mutable<Document.Mutable> {

    @NonNull Document.Mutable clear();

    @NonNull Document.Mutable remove(@NonNull String key);

    @NonNull Document.Mutable receive(@NonNull DocumentSend send);

    @NonNull Document.Mutable appendNull(@NonNull String key);

    @NonNull Document.Mutable appendTree(@Nullable Object value);

    @NonNull Document.Mutable append(@NonNull Document document);

    @NonNull Document.Mutable append(@NonNull String key, @Nullable Object value);

    @NonNull Document.Mutable append(@NonNull String key, @Nullable Number value);

    @NonNull Document.Mutable append(@NonNull String key, @Nullable Boolean value);

    @NonNull Document.Mutable append(@NonNull String key, @Nullable String value);

    @NonNull Document.Mutable append(@NonNull String key, @Nullable Document value);
  }
}
