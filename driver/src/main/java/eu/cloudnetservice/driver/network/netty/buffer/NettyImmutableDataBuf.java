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

package eu.cloudnetservice.driver.network.netty.buffer;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty5.buffer.Buffer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an immutable data buf wrapping a netty byte buf.
 *
 * @since 4.0
 */
public class NettyImmutableDataBuf implements DataBuf {

  protected final Buffer buffer;
  protected boolean releasable;

  // transaction offset data
  protected volatile int readOffset;
  protected volatile int writeOffset;

  /**
   * Constructs a new netty immutable data buf instance.
   *
   * @param buffer the netty buffer to wrap.
   * @throws NullPointerException if the given buffer is null.
   */
  public NettyImmutableDataBuf(@NonNull Buffer buffer) {
    this.buffer = buffer;
    this.enableReleasing();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readBoolean() {
    return this.hotRead(Buffer::readBoolean);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte readByte() {
    return this.hotRead(Buffer::readByte);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readInt() {
    return this.hotRead(Buffer::readInt);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short readShort() {
    return this.hotRead(Buffer::readShort);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long readLong() {
    return this.hotRead(Buffer::readLong);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float readFloat() {
    return this.hotRead(Buffer::readFloat);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double readDouble() {
    return this.hotRead(Buffer::readDouble);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public char readChar() {
    return this.hotRead(Buffer::readChar);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] readByteArray() {
    return this.hotRead(buf -> {
      var bytes = new byte[NettyUtil.readVarInt(buf)];
      buf.readBytes(bytes, 0, bytes.length);
      return bytes;
    });
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
    return this.hotRead(buf -> {
      var length = NettyUtil.readVarInt(buf);
      return buf.readCharSequence(length, StandardCharsets.UTF_8).toString();
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf readDataBuf() {
    return this.hotRead(buf -> {
      // copy out the data
      var length = buf.readInt();
      var content = new NettyImmutableDataBuf(buf.copy(buf.readerOffset(), length, true));

      // skip the amount of bytes we're read and return the content
      buf.skipReadableBytes(length);
      return content;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] toByteArray() {
    return this.hotRead(buf -> {
      var bytes = new byte[buf.readableBytes()];
      buf.readBytes(bytes, 0, bytes.length);
      return bytes;
    });
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
    return this.hotRead(buf -> {
      var isNonNull = buf.readBoolean();
      return isNonNull ? readerWhenNonNull.apply(this) : valueWhenNull;
    });
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
    // we can only set the writer offset if the backing buffer is not read-only
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
    // we need to copy the underlying buffer when the wrapped one is read only, if not we can just use the given buffer
    return this.buffer.readOnly() ? new NettyMutableDataBuf(this.buffer.copy()) : new NettyMutableDataBuf(this.buffer);
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
  public @NonNull DataBuf disableReleasing() {
    this.releasable = false;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf enableReleasing() {
    this.releasable = true;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    if (this.releasable && this.buffer.isAccessible()) {
      this.buffer.close();
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
   * Get the wrapped netty byte buf of this buffer, for internal use only.
   *
   * @return the wrapped netty byte buf.
   */
  @ApiStatus.Internal
  public @NonNull Buffer buffer() {
    return this.buffer;
  }

  /**
   * Reads from this buffer, releasing it when the end of the input has been reached and releasing is enabled to prevent
   * memory leaks.
   *
   * @param reader the function which reads the requested data from the buffer.
   * @param <T>    the type of data to read.
   * @return the data read from the buffer.
   * @throws NullPointerException if the given reader function is null.
   */
  protected @NonNull <T> T hotRead(@NonNull Function<Buffer, T> reader) {
    // get the result
    var result = reader.apply(this.buffer);
    // check if the reader index reached the end and try to release the message then
    if (this.buffer.readableBytes() <= 0) {
      this.release();
    }
    // return the read result
    return result;
  }
}
