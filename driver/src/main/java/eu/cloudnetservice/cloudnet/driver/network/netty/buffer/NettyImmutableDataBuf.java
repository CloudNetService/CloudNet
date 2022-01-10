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
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtils;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import io.netty.buffer.ByteBuf;
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

  protected final ByteBuf byteBuf;
  protected boolean releasable;

  /**
   * Constructs a new netty immutable data buf instance.
   *
   * @param byteBuf the netty buffer to wrap.
   * @throws NullPointerException if the given buffer is null.
   */
  public NettyImmutableDataBuf(@NonNull ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
    this.enableReleasing();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readBoolean() {
    return this.hotRead(ByteBuf::readBoolean);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte readByte() {
    return this.hotRead(ByteBuf::readByte);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readInt() {
    return this.hotRead(ByteBuf::readInt);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short readShort() {
    return this.hotRead(ByteBuf::readShort);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long readLong() {
    return this.hotRead(ByteBuf::readLong);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float readFloat() {
    return this.hotRead(ByteBuf::readFloat);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double readDouble() {
    return this.hotRead(ByteBuf::readDouble);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public char readChar() {
    return this.hotRead(ByteBuf::readChar);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] readByteArray() {
    return this.hotRead(buf -> {
      var bytes = new byte[NettyUtils.readVarInt(buf)];
      buf.readBytes(bytes);
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
    return this.hotRead(buf -> new NettyImmutableDataBuf(buf.readBytes(buf.readInt())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] toByteArray() {
    var bytes = new byte[this.readableBytes()];
    return this.hotRead(buf -> {
      buf.readBytes(bytes);
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
    return this.byteBuf.readableBytes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf startTransaction() {
    this.byteBuf.markReaderIndex();
    this.byteBuf.markWriterIndex();

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf redoTransaction() {
    this.byteBuf.resetReaderIndex();
    this.byteBuf.resetWriterIndex();

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable asMutable() {
    return new NettyMutableDataBuf(this.byteBuf);
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
    if (this.releasable) {
      NettyUtils.safeRelease(this.byteBuf);
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
  @Internal
  public @NonNull ByteBuf byteBuf() {
    return this.byteBuf;
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
  protected @NonNull <T> T hotRead(@NonNull Function<ByteBuf, T> reader) {
    // get the result
    var result = reader.apply(this.byteBuf);
    // check if the reader index reached the end and try to release the message then
    if (!this.byteBuf.isReadable()) {
      this.release();
    }
    // return the read result
    return result;
  }
}
