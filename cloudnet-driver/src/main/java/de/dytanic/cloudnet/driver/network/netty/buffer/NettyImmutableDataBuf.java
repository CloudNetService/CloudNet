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

package de.dytanic.cloudnet.driver.network.netty.buffer;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NettyImmutableDataBuf implements DataBuf {

  protected final ByteBuf byteBuf;
  protected boolean releasable;

  public NettyImmutableDataBuf(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
    this.enableReleasing();
  }

  @Override
  public boolean readBoolean() {
    return this.byteBuf.readBoolean();
  }

  @Override
  public byte readByte() {
    return this.byteBuf.readByte();
  }

  @Override
  public int readInt() {
    return this.byteBuf.readInt();
  }

  @Override
  public short readShort() {
    return this.byteBuf.readShort();
  }

  @Override
  public long readLong() {
    return this.byteBuf.readLong();
  }

  @Override
  public float readFloat() {
    return this.byteBuf.readFloat();
  }

  @Override
  public double readDouble() {
    return this.byteBuf.readDouble();
  }

  @Override
  public char readChar() {
    return this.byteBuf.readChar();
  }

  @Override
  public byte[] readByteArray() {
    byte[] bytes = new byte[this.readInt()];
    this.byteBuf.readBytes(bytes);
    return bytes;
  }

  @Override
  public @NotNull UUID readUniqueId() {
    return new UUID(this.readLong(), this.readLong());
  }

  @Override
  public @NotNull String readString() {
    return new String(this.readByteArray(), StandardCharsets.UTF_8);
  }

  @Override
  public @NotNull DataBuf readDataBuf() {
    return new NettyImmutableDataBuf(this.byteBuf.readBytes(this.readInt()));
  }

  @Override
  public <T> @Nullable T readObject(@NotNull Class<T> type) {
    return DefaultObjectMapper.DEFAULT_MAPPER.readObject(this, type);
  }

  @Override
  public <T> T readObject(@NotNull Type type) {
    return DefaultObjectMapper.DEFAULT_MAPPER.readObject(this, type);
  }

  @Override
  public <T> @Nullable T readNullable(@NotNull Function<DataBuf, T> readerWhenNonNull) {
    return this.readNullable(readerWhenNonNull, null);
  }

  @Override
  public <T> T readNullable(@NotNull Function<DataBuf, T> readerWhenNonNull, T valueWhenNull) {
    boolean isNonNull = this.readBoolean();
    return isNonNull ? readerWhenNonNull.apply(this) : valueWhenNull;
  }

  @Override
  public int getReadableBytes() {
    return this.byteBuf.readableBytes();
  }

  @Override
  public @NotNull DataBuf startTransaction() {
    this.byteBuf.markReaderIndex();
    this.byteBuf.markWriterIndex();

    return this;
  }

  @Override
  public @NotNull DataBuf redoTransaction() {
    this.byteBuf.resetReaderIndex();
    this.byteBuf.resetWriterIndex();

    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable asMutable() {
    return new NettyMutableDataBuf(this.byteBuf);
  }

  @Override
  public @NotNull DataBuf disableReleasing() {
    this.releasable = false;
    return this;
  }

  @Override
  public @NotNull DataBuf enableReleasing() {
    this.releasable = true;
    return this;
  }

  @Override
  public void release() {
    if (this.releasable && this.byteBuf.refCnt() > 0) {
      this.byteBuf.release(this.byteBuf.refCnt());
    }
  }

  @Internal
  public @NotNull ByteBuf getByteBuf() {
    return this.byteBuf;
  }
}
