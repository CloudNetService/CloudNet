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

package eu.cloudnetservice.common.document;

import eu.cloudnetservice.common.document.property.DocProperty;
import eu.cloudnetservice.common.document.property.DocPropertyHolder;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A document is a wrapper to persistence data or read data in the heap or easy into the following implementation format
 * of this interface.
 */
public interface Document<R extends Document<R>>
  extends Serializable, DocPropertyHolder, Persistable, Readable, Iterable<String>, Cloneable {

  @NonNull Collection<String> keys();

  int size();

  @NonNull R clear();

  @NonNull R remove(@NonNull String key);

  boolean contains(@NonNull String key);

  @UnknownNullability <T> T toInstanceOf(@NonNull Class<T> clazz);

  @UnknownNullability <T> T toInstanceOf(@NonNull Type clazz);

  @NonNull R append(@NonNull String key, @Nullable Object value);

  @NonNull R append(@NonNull String key, @Nullable Number value);

  @NonNull R append(@NonNull String key, @Nullable Boolean value);

  @NonNull R append(@NonNull String key, @Nullable String value);

  @NonNull R append(@NonNull String key, @Nullable Character value);

  @NonNull R append(@NonNull String key, @Nullable R value);

  @NonNull R append(@Nullable R t);

  @NonNull R appendNull(@NonNull String key);

  @NonNull R getDocument(@NonNull String key);

  default int getInt(@NonNull String key) {
    return this.getInt(key, 0);
  }

  default double getDouble(@NonNull String key) {
    return this.getDouble(key, 0);
  }

  default float getFloat(@NonNull String key) {
    return this.getFloat(key, 0);
  }

  default byte getByte(@NonNull String key) {
    return this.getByte(key, (byte) 0);
  }

  default short getShort(@NonNull String key) {
    return this.getShort(key, (short) 0);
  }

  default long getLong(@NonNull String key) {
    return this.getLong(key, 0);
  }

  default boolean getBoolean(@NonNull String key) {
    return this.getBoolean(key, false);
  }

  default @UnknownNullability String getString(@NonNull String key) {
    return this.getString(key, null);
  }

  default char getChar(@NonNull String key) {
    return this.getChar(key, (char) 0);
  }

  default @UnknownNullability Object get(@NonNull String key) {
    return this.get(key, (Object) null);
  }

  default @UnknownNullability <T> T get(@NonNull String key, @NonNull Class<T> clazz) {
    return this.get(key, clazz, null);
  }

  default @UnknownNullability <T> T get(@NonNull String key, @NonNull Type type) {
    return this.get(key, type, null);
  }

  @UnknownNullability R getDocument(@NonNull String key, @Nullable R def);

  int getInt(@NonNull String key, int def);

  double getDouble(@NonNull String key, double def);

  float getFloat(@NonNull String key, float def);

  byte getByte(@NonNull String key, byte def);

  short getShort(@NonNull String key, short def);

  long getLong(@NonNull String key, long def);

  boolean getBoolean(@NonNull String key, boolean def);

  @UnknownNullability String getString(@NonNull String key, @Nullable String def);

  char getChar(@NonNull String key, char def);

  @UnknownNullability Object get(@NonNull String key, @Nullable Object def);

  @UnknownNullability <T> T get(@NonNull String key, @NonNull Class<T> clazz, @Nullable T def);

  @UnknownNullability <T> T get(@NonNull String key, @NonNull Type type, @Nullable T def);

  default boolean empty() {
    return this.size() == 0;
  }

  default @NonNull Stream<String> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  @Override
  @SuppressWarnings("unchecked")
  default @NonNull <E> R property(@NonNull DocProperty<E> docProperty, @Nullable E val) {
    docProperty.append(this, val);
    return (R) this;
  }

  @Override
  default <E> @UnknownNullability E property(@NonNull DocProperty<E> docProperty) {
    return docProperty.get(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  default @NonNull <E> R removeProperty(@NonNull DocProperty<E> docProperty) {
    docProperty.remove(this);
    return (R) this;
  }

  @Override
  default <E> boolean hasProperty(@NonNull DocProperty<E> docProperty) {
    return docProperty.isAppendedTo(this);
  }
}
