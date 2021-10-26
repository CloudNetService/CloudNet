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

package de.dytanic.cloudnet.driver.network.buffer;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DataBuf extends AutoCloseable {

  /**
   * @see DataBufFactory#createEmpty()
   */
  @NotNull
  static DataBuf.Mutable empty() {
    return DataBufFactory.defaultFactory().createEmpty();
  }

  boolean readBoolean();

  byte readByte();

  int readInt();

  short readShort();

  long readLong();

  float readFloat();

  double readDouble();

  char readChar();

  byte[] readByteArray();

  @NotNull UUID readUniqueId();

  @NotNull String readString();

  @NotNull DataBuf readDataBuf();

  <T> T readObject(@NotNull Class<T> type);

  <T> T readObject(@NotNull Type type);

  @Nullable <T> T readNullable(@NotNull Function<DataBuf, T> readerWhenNonNull);

  <T> T readNullable(@NotNull Function<DataBuf, T> readerWhenNonNull, T valueWhenNull);

  // utility for reading

  int getReadableBytes();

  @NotNull DataBuf startTransaction();

  @NotNull DataBuf redoTransaction();

  @NotNull DataBuf.Mutable asMutable();

  // direct memory access

  @NotNull DataBuf disableReleasing();

  @NotNull DataBuf enableReleasing();

  void release();

  @Override
  void close();

  interface Mutable extends DataBuf {

    @NotNull DataBuf.Mutable writeBoolean(boolean b);

    @NotNull DataBuf.Mutable writeInt(int integer);

    @NotNull DataBuf.Mutable writeByte(byte b);

    @NotNull DataBuf.Mutable writeShort(short s);

    @NotNull DataBuf.Mutable writeLong(long l);

    @NotNull DataBuf.Mutable writeFloat(float f);

    @NotNull DataBuf.Mutable writeDouble(double d);

    @NotNull DataBuf.Mutable writeChar(char c);

    @NotNull DataBuf.Mutable writeByteArray(byte[] b);

    @NotNull DataBuf.Mutable writeByteArray(byte[] b, int amount);

    @NotNull DataBuf.Mutable writeUniqueId(@NotNull UUID uuid);

    @NotNull DataBuf.Mutable writeString(@NotNull String string);

    @NotNull DataBuf.Mutable writeDataBuf(@NotNull DataBuf buf);

    @NotNull DataBuf.Mutable writeObject(@Nullable Object obj);

    @NotNull <T> DataBuf.Mutable writeNullable(@Nullable T object,
      @NotNull BiConsumer<DataBuf.Mutable, T> handlerWhenNonNull);

    // utility for reading

    @NotNull DataBuf asImmutable();
  }
}
