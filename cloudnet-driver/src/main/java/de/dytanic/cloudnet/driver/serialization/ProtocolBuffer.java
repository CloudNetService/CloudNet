/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProtocolBuffer extends ByteBuf {

  public static final ProtocolBuffer EMPTY = create();

  public static ProtocolBuffer create() {
    return wrap(Unpooled.buffer());
  }

  public static ProtocolBuffer readAll(InputStream inputStream) {
    return wrap(FileUtils.toByteArray(inputStream));
  }

  public static ProtocolBuffer wrap(byte[] bytes) {
    return wrap(Unpooled.wrappedBuffer(bytes));
  }

  public static ProtocolBuffer wrap(ByteBuf buf) {
    return new DefaultProtocolBuffer(buf);
  }


  @NotNull
  public abstract String readString();

  public abstract ProtocolBuffer writeString(@NotNull String stringToWrite);

  @Nullable
  public abstract String readOptionalString();

  public abstract ProtocolBuffer writeOptionalString(@Nullable String stringToWrite);

  @NotNull
  public abstract byte[] readArray();

  public abstract ProtocolBuffer writeArray(@NotNull byte[] bytes);

  @Nullable
  public abstract byte[] readOptionalArray();

  public abstract ProtocolBuffer writeOptionalArray(@Nullable byte[] bytes);

  @NotNull
  public abstract byte[] toArray();

  @NotNull
  public abstract Collection<String> readStringCollection();

  public abstract ProtocolBuffer writeStringCollection(@NotNull Collection<String> list);

  @NotNull
  public abstract String[] readStringArray();

  public abstract ProtocolBuffer writeStringArray(@NotNull String[] array);

  public abstract int readVarInt();

  public abstract ProtocolBuffer writeVarInt(int value);

  public abstract long readVarLong();

  public abstract ProtocolBuffer writeVarLong(long value);

  @NotNull
  public abstract UUID readUUID();

  public abstract ProtocolBuffer writeUUID(@NotNull UUID uuid);

  @Nullable
  public abstract UUID readOptionalUUID();

  public abstract ProtocolBuffer writeOptionalUUID(@Nullable UUID uuid);

  @NotNull
  public abstract Collection<UUID> readUUIDCollection();

  public abstract ProtocolBuffer writeUUIDCollection(@NotNull Collection<UUID> uuids);

  @NotNull
  public abstract JsonDocument readJsonDocument();

  public abstract ProtocolBuffer writeJsonDocument(@NotNull JsonDocument document);

  @Nullable
  public abstract JsonDocument readOptionalJsonDocument();

  public abstract ProtocolBuffer writeOptionalJsonDocument(@Nullable JsonDocument document);

  @NotNull
  public abstract <T extends SerializableObject> T readObject(@NotNull Class<T> objectClass);

  @NotNull
  public abstract <T extends SerializableObject> T readObject(@NotNull T targetObject);

  public abstract ProtocolBuffer writeObject(@NotNull SerializableObject object);

  @Nullable
  public abstract <T extends SerializableObject> T readOptionalObject(@NotNull Class<T> objectClass);

  @Nullable
  public abstract <T extends SerializableObject> T readOptionalObject(@NotNull T targetObject);

  public abstract ProtocolBuffer writeOptionalObject(@Nullable SerializableObject object);

  @NotNull
  public abstract <T extends SerializableObject> Collection<T> readObjectCollection(@NotNull Class<T> objectClass);

  public abstract ProtocolBuffer writeObjectCollection(@NotNull Collection<? extends SerializableObject> objects);

  @NotNull
  public abstract <T extends SerializableObject> T[] readObjectArray(@NotNull Class<T> objectClass);

  public abstract <T extends SerializableObject> ProtocolBuffer writeObjectArray(@NotNull T[] objects);

  public abstract <E extends Enum<E>> E readEnumConstant(@NotNull Class<E> enumClass);

  public abstract <E extends Enum<E>> ProtocolBuffer writeEnumConstant(@NotNull E enumConstant);

  public abstract <E extends Enum<E>> E readOptionalEnumConstant(@NotNull Class<E> enumClass);

  public abstract <E extends Enum<E>> ProtocolBuffer writeOptionalEnumConstant(@Nullable E enumConstant);

  public abstract ProtocolBuffer writeThrowable(@Nullable Throwable throwable);

  @Nullable
  public abstract Throwable readThrowable();


  @Override
  public abstract ProtocolBuffer writeBoolean(boolean value);

  @Override
  public abstract ProtocolBuffer writeByte(int value);

  @Override
  public abstract ProtocolBuffer writeShort(int value);

  @Override
  public abstract ProtocolBuffer writeInt(int value);

  @Override
  public abstract ProtocolBuffer writeLong(long value);

  @Override
  public abstract ProtocolBuffer writeFloat(float value);

  @Override
  public abstract ProtocolBuffer writeDouble(double value);
}
