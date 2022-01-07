/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.buffer;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface DataBuf extends AutoCloseable {

  /**
   * @see DataBufFactory#createEmpty()
   */
  static @NonNull DataBuf.Mutable empty() {
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

  @NonNull UUID readUniqueId();

  @NonNull String readString();

  @NonNull DataBuf readDataBuf();

  byte[] toByteArray();

  <T> T readObject(@NonNull Class<T> type);

  <T> T readObject(@NonNull Type type);

  @Nullable <T> T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull);

  <T> T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull, T valueWhenNull);

  // utility for reading

  int readableBytes();

  @NonNull DataBuf startTransaction();

  @NonNull DataBuf redoTransaction();

  @NonNull DataBuf.Mutable asMutable();

  // direct memory access

  @NonNull DataBuf disableReleasing();

  @NonNull DataBuf enableReleasing();

  void release();

  @Override
  void close();

  interface Mutable extends DataBuf {

    @NonNull DataBuf.Mutable writeBoolean(boolean b);

    @NonNull DataBuf.Mutable writeInt(int integer);

    @NonNull DataBuf.Mutable writeByte(byte b);

    @NonNull DataBuf.Mutable writeShort(short s);

    @NonNull DataBuf.Mutable writeLong(long l);

    @NonNull DataBuf.Mutable writeFloat(float f);

    @NonNull DataBuf.Mutable writeDouble(double d);

    @NonNull DataBuf.Mutable writeChar(char c);

    @NonNull DataBuf.Mutable writeByteArray(byte[] b);

    @NonNull DataBuf.Mutable writeByteArray(byte[] b, int amount);

    @NonNull DataBuf.Mutable writeUniqueId(@NonNull UUID uuid);

    @NonNull DataBuf.Mutable writeString(@NonNull String string);

    @NonNull DataBuf.Mutable writeDataBuf(@NonNull DataBuf buf);

    @NonNull DataBuf.Mutable writeObject(@Nullable Object obj);

    @NonNull <T> DataBuf.Mutable writeNullable(@Nullable T object,
      @NonNull BiConsumer<DataBuf.Mutable, T> handlerWhenNonNull);

    // utility for reading

    @NonNull DataBuf asImmutable();
  }
}
