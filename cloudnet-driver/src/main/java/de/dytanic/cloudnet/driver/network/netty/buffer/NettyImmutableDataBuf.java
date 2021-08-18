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
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NettyImmutableDataBuf implements DataBuf {

  protected final ByteBuf byteBuf;

  public NettyImmutableDataBuf(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
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
  public @NotNull String readString() {
    return NettyUtils.readString(this.byteBuf);
  }

  @Override
  public <T> @Nullable T readNullable(@NotNull Function<DataBuf, T> readerWhenNonNull) {
    boolean isNonNull = this.readBoolean();
    return isNonNull ? readerWhenNonNull.apply(this) : null;
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

  @Internal
  public @NotNull ByteBuf getByteBuf() {
    return this.byteBuf;
  }
}
