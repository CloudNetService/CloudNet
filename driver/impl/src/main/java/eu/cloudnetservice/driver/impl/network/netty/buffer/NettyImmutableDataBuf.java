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

package eu.cloudnetservice.driver.impl.network.netty.buffer;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferComponent;
import io.netty5.buffer.internal.InternalBufferUtils;
import io.netty5.buffer.internal.ResourceSupport;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an immutable data buf wrapping a netty byte buf.
 *
 * @since 4.0
 */
public sealed class NettyImmutableDataBuf implements DataBuf permits NettyMutableDataBuf {

  protected final Buffer buffer;

  // transaction offset data
  protected int readOffset;
  protected int writeOffset;

  /**
   * Constructs a new netty immutable data buf instance.
   *
   * @param buffer the netty buffer to wrap.
   * @throws NullPointerException     if the given buffer is null.
   * @throws IllegalArgumentException if the given buffer cannot be wrapped into a data buf.
   */
  public NettyImmutableDataBuf(@NonNull Buffer buffer) {
    Preconditions.checkArgument(buffer instanceof BufferComponent, "buffer must implement BufferComponent");
    Preconditions.checkArgument(buffer instanceof ResourceSupport<?, ?>, "buffer must extend ResourceSupport");
    this.buffer = buffer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readBoolean() {
    return this.buffer.readBoolean();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte readByte() {
    return this.buffer.readByte();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readInt() {
    return this.buffer.readInt();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short readShort() {
    return this.buffer.readShort();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long readLong() {
    return this.buffer.readLong();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float readFloat() {
    return this.buffer.readFloat();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double readDouble() {
    return this.buffer.readDouble();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public char readChar() {
    return this.buffer.readChar();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] readByteArray() {
    var buf = this.buffer;
    var bytes = new byte[NettyUtil.readVarInt(buf)];
    buf.readBytes(bytes, 0, bytes.length);
    return bytes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull UUID readUniqueId() {
    return new UUID(this.readLong(), this.readLong());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String readString() {
    var stringBytes = this.readByteArray();
    return new String(stringBytes, StandardCharsets.UTF_8);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf readDataBuf() {
    var buf = this.buffer;
    var length = NettyUtil.readVarInt(buf);
    var content = new NettyImmutableDataBuf(buf.copy(buf.readerOffset(), length));
    buf.skipReadableBytes(length);
    return content;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] toByteArray() {
    var buf = this.buffer;
    var bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes, 0, bytes.length);
    return bytes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @Nullable T readObject(@NonNull Class<T> type) {
    return DefaultObjectMapper.DEFAULT_MAPPER.readObject(this, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T readObject(@NonNull Type type) {
    return DefaultObjectMapper.DEFAULT_MAPPER.readObject(this, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @Nullable T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull) {
    return this.readNullable(readerWhenNonNull, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull, T valueWhenNull) {
    var isNonNull = this.readBoolean();
    return isNonNull ? readerWhenNonNull.apply(this) : valueWhenNull;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readableBytes() {
    return this.buffer.readableBytes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readerOffset() {
    return this.buffer.readerOffset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf readerOffset(int offset) {
    this.buffer.readerOffset(offset);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf advanceReaderOffset(int delta) {
    this.buffer.skipReadableBytes(delta);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ByteBuffer readableNioBuffer() {
    var bufferAsBufferComponent = (BufferComponent) this.buffer;
    return bufferAsBufferComponent.readableBuffer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf startTransaction() {
    this.readOffset = this.buffer.readerOffset();
    this.writeOffset = this.buffer.writerOffset();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf redoTransaction() {
    this.buffer.readerOffset(this.readOffset);
    if (!this.buffer.readOnly()) {
      this.buffer.writerOffset(this.writeOffset);
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable asMutable() {
    var buf = this.buffer.copy(false);
    return new NettyMutableDataBuf(buf);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accessible() {
    return this.buffer.isAccessible();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int acquires() {
    var resourceSupport = this.bufferAsResourceSupport();
    return InternalBufferUtils.countBorrows(resourceSupport);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf acquire() {
    var resourceSupport = this.bufferAsResourceSupport();
    InternalBufferUtils.acquire(resourceSupport);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    try {
      this.buffer.close();
    } catch (IllegalStateException _) {
      // possible double-free error due to a race, ignore
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forceRelease() {
    try {
      var buffer = this.buffer;
      while (buffer.isAccessible()) {
        buffer.close();
      }
    } catch (IllegalStateException _) {
      // possible double-free error due to a race, ignore
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.release();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "NettyImmutableDataBuf[buffer=" + this.buffer + "]";
  }

  /**
   * Get the wrapped netty byte buf of this buffer, for internal use only.
   *
   * @return the wrapped netty byte buf.
   */
  public @NonNull Buffer buffer() {
    return this.buffer;
  }

  /**
   * Gets the backing buffer instance cast to a {@code ResourceSupport} instance.
   *
   * @return the backing buffer instance cast to a {@code ResourceSupport} instance.
   */
  private @NonNull ResourceSupport<?, ?> bufferAsResourceSupport() {
    return (ResourceSupport<?, ?>) this.buffer;
  }
}
