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

package eu.cloudnetservice.cloudnet.driver.network.netty.buffer;

import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf.Mutable;
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtil;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the default implementation of a mutable data buf, wrapping a netty byte buffer.
 *
 * @since 4.0
 */
public class NettyMutableDataBuf extends NettyImmutableDataBuf implements Mutable {

  /**
   * Constructs a new mutable data buf instance.
   *
   * @param byteBuf the netty buffer to wrap.
   * @throws NullPointerException if the given buffer is null.
   */
  public NettyMutableDataBuf(@NonNull ByteBuf byteBuf) {
    super(byteBuf);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeBoolean(boolean b) {
    this.byteBuf.writeBoolean(b);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeInt(int integer) {
    this.byteBuf.writeInt(integer);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeByte(byte b) {
    this.byteBuf.writeByte(b);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeShort(short s) {
    this.byteBuf.writeShort(s);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeLong(long l) {
    this.byteBuf.writeLong(l);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeFloat(float f) {
    this.byteBuf.writeFloat(f);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeDouble(double d) {
    this.byteBuf.writeDouble(d);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeChar(char c) {
    this.byteBuf.writeChar(c);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeByteArray(byte[] b) {
    return this.writeByteArray(b, b.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeByteArray(byte[] b, int amount) {
    NettyUtil.writeVarInt(this.byteBuf, amount);
    this.byteBuf.writeBytes(b, 0, amount);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeUniqueId(@NonNull UUID uuid) {
    return this.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeString(@NonNull String string) {
    return this.writeByteArray(string.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeDataBuf(@NonNull DataBuf buf) {
    buf.startTransaction();
    // write the content
    this.writeInt(buf.readableBytes());
    this.byteBuf.writeBytes(((NettyImmutableDataBuf) buf).byteBuf);
    // reset the data for later use
    buf.redoTransaction();

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeObject(@Nullable Object obj) {
    return DefaultObjectMapper.DEFAULT_MAPPER.writeObject(this, obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> Mutable writeNullable(@Nullable T object, @NonNull BiConsumer<Mutable, T> handlerWhenNonNull) {
    this.writeBoolean(object != null);
    if (object != null) {
      handlerWhenNonNull.accept(this, object);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf asImmutable() {
    return new NettyImmutableDataBuf(this.byteBuf);
  }
}
