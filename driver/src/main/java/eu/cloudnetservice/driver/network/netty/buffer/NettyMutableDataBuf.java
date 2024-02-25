/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.netty.buffer;

import com.google.common.base.Utf8;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty5.buffer.Buffer;
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
public class NettyMutableDataBuf extends NettyImmutableDataBuf implements DataBuf.Mutable {

  /**
   * Constructs a new mutable data buf instance.
   *
   * @param buffer the netty buffer to wrap.
   * @throws NullPointerException if the given buffer is null.
   */
  public NettyMutableDataBuf(@NonNull Buffer buffer) {
    super(buffer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeBoolean(boolean b) {
    this.buffer.writeBoolean(b);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeInt(int integer) {
    this.buffer.writeInt(integer);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeByte(byte b) {
    this.buffer.writeByte(b);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeShort(short s) {
    this.buffer.writeShort(s);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeLong(long l) {
    this.buffer.writeLong(l);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeFloat(float f) {
    this.buffer.writeFloat(f);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeDouble(double d) {
    this.buffer.writeDouble(d);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeChar(char c) {
    this.buffer.writeChar(c);
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
    NettyUtil.writeVarInt(this.buffer, amount);
    this.buffer.writeBytes(b, 0, amount);
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
    NettyUtil.writeVarInt(this.buffer, Utf8.encodedLength(string));
    this.buffer.writeCharSequence(string, StandardCharsets.UTF_8);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable writeDataBuf(@NonNull DataBuf buf) {
    buf.startTransaction();
    try {
      // write the content
      this.writeInt(buf.readableBytes());
      this.buffer.writeBytes(((NettyImmutableDataBuf) buf).buffer);
    } finally {
      // reset the data for later use & release the content if it isn't disabled
      buf.redoTransaction().release();
    }

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
    return new NettyImmutableDataBuf(this.buffer);
  }
}
