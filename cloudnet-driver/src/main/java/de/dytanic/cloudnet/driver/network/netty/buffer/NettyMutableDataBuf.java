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
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NettyMutableDataBuf extends NettyImmutableDataBuf implements DataBuf.Mutable {

  public NettyMutableDataBuf(ByteBuf byteBuf) {
    super(byteBuf);
  }

  @Override
  public @NotNull DataBuf.Mutable writeBoolean(boolean b) {
    this.byteBuf.writeBoolean(b);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeInt(int integer) {
    this.byteBuf.writeInt(integer);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeByte(byte b) {
    this.byteBuf.writeByte(b);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeShort(short s) {
    this.byteBuf.writeShort(s);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeLong(long l) {
    this.byteBuf.writeLong(l);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeFloat(float f) {
    this.byteBuf.writeFloat(f);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeDouble(double d) {
    this.byteBuf.writeDouble(d);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeChar(char c) {
    this.byteBuf.writeChar(c);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeByteArray(byte[] b) {
    return this.writeByteArray(b, b.length);
  }

  @Override
  public @NotNull DataBuf.Mutable writeByteArray(byte[] b, int amount) {
    this.byteBuf.writeBytes(b, 0, amount);
    return this;
  }

  @Override
  public @NotNull DataBuf.Mutable writeUniqueId(@NotNull UUID uuid) {
    return this.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
  }

  @Override
  public @NotNull DataBuf.Mutable writeString(@NotNull String string) {
    return this.writeByteArray(string.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public @NotNull <T> Mutable writeNullable(@Nullable T object, @NotNull BiConsumer<Mutable, T> handlerWhenNonNull) {
    this.writeBoolean(object != null);
    if (object != null) {
      handlerWhenNonNull.accept(this, object);
    }
    return this;
  }

  @Override
  public @NotNull DataBuf asImmutable() {
    return new NettyImmutableDataBuf(this.byteBuf);
  }
}
