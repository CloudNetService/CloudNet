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
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtil;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty5.buffer.api.Buffer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an immutable data buf wrapping a netty byte buf.
 *
 * @since 4.0
 */
public class NettyImmutableDataBuf implements DataBuf {

  protected final Buffer buffer;
  protected boolean releasable;

  private int markedReaderOffset;
  private int markedWriterOffset;

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
    return new String(this.readByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf readDataBuf() {
    return this.hotRead(buf -> {
      var dataLength = buf.readInt();
      var buffer = new NettyImmutableDataBuf(buf.copy(buf.readerOffset(), dataLength));

      // we need to move the reader offset manually as copy didn't do it for us
      buf.skipReadable(dataLength);
      return buffer;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] toByteArray() {
    var bytes = new byte[this.readableBytes()];
    return this.hotRead(buf -> {
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
    return this.hotRead(buf -> buf.readBoolean() ? readerWhenNonNull.apply(this) : valueWhenNull);
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
    this.markedReaderOffset = this.buffer.readerOffset();
    this.markedWriterOffset = this.buffer.writerOffset();

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf redoTransaction() {
    this.buffer.readerOffset(this.markedReaderOffset);
    this.buffer.writerOffset(this.markedWriterOffset);

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable asMutable() {
    return new NettyMutableDataBuf(this.buffer);
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
   * Get the wrapped netty buffer of this buffer, for internal use only.
   *
   * @return the wrapped netty buffer.
   */
  @Internal
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
