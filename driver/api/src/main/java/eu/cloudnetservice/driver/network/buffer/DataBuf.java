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

package eu.cloudnetservice.driver.network.buffer;

import eu.cloudnetservice.driver.network.object.ObjectMapper;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an immutable buffer which is essentially a wrapper around some kind of readable buffer. By default,
 * CloudNet wraps the netty ByteBuf meaning that read operations are deferred to an underlying byte array of nio byte
 * buffer.
 * <p>
 * However, a data buf does not allow (in comparison to other wrappers) the random access to bytes at specific positions
 * (for example using a netty ByteBuf {@code buf.getByte(index)} would be possible, but isn't in this buffer). But it
 * must be possible for a reader to store the current position of the buffer and return to it (for example after
 * reading). This is done by starting a transaction using {@link #startTransaction()}, reading or writing to the buffer
 * and restoring the previous position by using {@link #redoTransaction()}. Note: This will not remove bytes written to
 * the buffer, other write operations will however start from the original index and override the written bytes.
 * <p>
 * Other operations should work as expected on a buffer, reading should always start from the head of the buffer,
 * reflecting the operation over all other readers. If one reader reads a byte from the buffer, the next one will start
 * at the second byte in the buffer, not the first one.
 * <p>
 * Buffers are not required to be thread safe, they can but should be treated specially in these cases. Concurrent read
 * and/or write operations will therefore produce (by default) different results when spread over threads.
 * <p>
 * Buffers should avoid memory leaks by ensuring to release their content after the last byte of the buffer was read.
 * The behaviour can be influenced by acquiring them using {@link #acquire()}. The buffer will only be released if every
 * place that acquired the buffer has released it using {@link #release()} or {@link #close()}. In rare cases it might
 * be necessary to release a buffer even if it's acquired, use {@link #forceRelease()} in that case.
 * <p>
 * To prevent exceptions during reading, it's worth noting that using {@code readableBytes() > 0} it is possible to
 * verify that there are still bytes left in the buffer to read.
 * <p>
 * It is not recommended using any constructor to create an instance of a data buf - you should obtain a factory for
 * them and create your instance using the given factory methods.
 *
 * @see DataBufFactory
 * @see Mutable
 * @since 4.0
 */
public interface DataBuf extends AutoCloseable {

  /**
   * Creates a new, empty data buffer using the default buffer factory (currently a netty buffer factory).
   *
   * @return a new empty buffer which is mutable.
   */
  static @NonNull DataBuf.Mutable empty() {
    return DataBufFactory.defaultFactory().createEmpty();
  }

  /**
   * Reads a boolean from this buffer at the current reader index. Exactly one byte is read from the buffer.
   *
   * @return the boolean representation of the byte at the current position.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  boolean readBoolean();

  /**
   * Reads a byte from this buffer at the current reader index. Exactly one byte is read from the buffer.
   *
   * @return the byte at the current reader position.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  byte readByte();

  /**
   * Reads a 32-bit integer from this buffer at the current reader index. Exactly four bytes are read from the buffer.
   *
   * @return the next integer in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than four bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  int readInt();

  /**
   * Reads a 16-bit short from this buffer at the current reader index. Exactly two bytes are read from the buffer.
   *
   * @return the next short in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than two bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  short readShort();

  /**
   * Reads a 64-bit long from this buffer at the current reader index. Exactly eight bytes are read from the buffer.
   *
   * @return the next long in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than eight bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  long readLong();

  /**
   * Reads a 32-bit float from this buffer at the current reader index. Exactly four bytes are read from the buffer.
   *
   * @return the next float in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than four bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  float readFloat();

  /**
   * Reads a 64-bit double from this buffer at the current reader index. Exactly eight bytes are read from the buffer.
   *
   * @return the next double in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than eight bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  double readDouble();

  /**
   * Reads a 16-bit (UTF-16) char from this buffer at the current reader index. Exactly two bytes are read from the
   * buffer.
   *
   * @return the next UTF-16 char in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than two bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  char readChar();

  /**
   * Reads the next array of bytes from the buffer. A byte array is serialized in a special way. The bytes in the buffer
   * are prefixed with the number of bytes in the array. Two steps are made to read an array from the buffer:
   * <ol>
   *   <li>The number of bytes in the following array are read from the buffer (by default a var int).
   *   <li>The number of bytes the array is prefixed with are read from the buffer and put into a new array.
   * </ol>
   * <p>
   * As the operation is dynamic there is no way to pre-calculate the amount of bytes needed to read the next byte array.
   *
   * @return the next byte array in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are fewer bytes than expected in the buffer.
   * @throws IllegalStateException     if this buffer was released.
   */
  byte[] readByteArray();

  /**
   * Reads the next unique id from the buffer at the current reader index. The operation reads two longs from the
   * buffer: the most significant bits of the unique id, and the least significant bits of the unique id. This totals to
   * exactly sixteen bytes which are read from the buffer.
   *
   * @return the next unique id in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are less than sixteen bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  @NonNull
  UUID readUniqueId();

  /**
   * Reads the next UTF-8 encoded string from the buffer. A string during write is converted to a byte array containing
   * all bytes in UTF-8 form. Reading just reverses this operation. See {@link #readByteArray()} for an explanation how
   * the read operation works in detail (it's the same operation, the result is just wrapped using the string
   * constructor).
   *
   * @return the next string in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are fewer bytes than expected in the buffer.
   * @throws IllegalStateException     if this buffer was released.
   */
  @NonNull
  String readString();

  /**
   * Reads the next data buf from the buffer. A data buf write works like a byte array write operation because the
   * buffer is essentially just wrapping a byte array. See {@link #readByteArray()} about the expected format.
   * <p>
   * Buffers are not expected to be cross-implementation-compatible. For instance, a netty buffer can only read and
   * write netty buffers.
   *
   * @return the data buf in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are fewer bytes than expected in the buffer.
   * @throws IllegalStateException     if this buffer was released.
   */
  @NonNull
  DataBuf readDataBuf();

  /**
   * Converts the remaining bytes in this buffer into a byte array. This operation moves the reader index to the end of
   * the buffer.
   *
   * @return the remaining bytes in this buffer converted to a byte array.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  byte[] toByteArray();

  /**
   * Reads the next object from the buffer at the current reader index. The object is read using the default object
   * mapper of the system.
   *
   * @param type the type of the object to read.
   * @param <T>  the generic type of the object to read.
   * @return the next object in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   * @see ObjectMapper#readObject(DataBuf, Type)
   */
  <T> T readObject(@NonNull Class<T> type);

  /**
   * Reads the next object from the buffer at the current reader index. The object is read using the default object
   * mapper of the system.
   *
   * @param type the type of the object to read.
   * @param <T>  the generic type of the object to read.
   * @return the next object in the buffer at the current reader index.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   * @see ObjectMapper#readObject(DataBuf, Type)
   */
  <T> T readObject(@NonNull Type type);

  /**
   * Reads the next requested data from the buffer. This method call is equivalent to
   * {@code readNullable(readerWhenNonNull, null)}.
   *
   * @param readerWhenNonNull the reader to read the data from the buffer when the next value is non-null.
   * @param <T>               the generic type of the data to read.
   * @return the value read from the buffer or the fallback value when the buffered contained null at the position.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  @Nullable <T> T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull);

  /**
   * Reads the next requested data from the buffer. To determine whether the given reader for further reading should be
   * called, the boolean before the actual data is read. If the boolean is true, the following data is present
   * (non-null), otherwise the given value to return when null is returned.
   *
   * @param readerWhenNonNull the reader to read the data from the buffer when the next value is non-null.
   * @param valueWhenNull     the value to return when the buffer contains a null value at the current reader index.
   * @param <T>               the generic type of the data to read.
   * @return the value read from the buffer or the fallback value when the buffered contained null at the position.
   * @throws IndexOutOfBoundsException if there are no more bytes to read.
   * @throws IllegalStateException     if this buffer was released.
   */
  <T> T readNullable(@NonNull Function<DataBuf, T> readerWhenNonNull, @Nullable T valueWhenNull);

  // utility for reading

  /**
   * Get the number of remaining bytes in the buffer until the buffer gets released (when enabled).
   *
   * @return the number of remaining bytes in the buffer.
   */
  int readableBytes();

  /**
   * Starts a transaction to the buffer. Starting a transaction while another transaction is active will override the
   * current transaction marker. A transaction can be redone by using {@link #redoTransaction()}.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @NonNull
  DataBuf startTransaction();

  /**
   * Redoes the currently running transaction on the buffer. If no transaction was started before, the reader and writer
   * index will go back to 0.
   *
   * @return the same instance as used to call the method, for chaining.
   * @throws IndexOutOfBoundsException if an illegal action was made to buffer moving the reader or writer index.
   */
  @NonNull
  DataBuf redoTransaction();

  /**
   * Converts this immutable buffer to a mutable one. There is no need to copy the underlying byte tracker, meaning that
   * all writes will be reflected into this buffer and vise-versa.
   *
   * @return a mutable variant of this buffer.
   */
  @NonNull
  DataBuf.Mutable asMutable();

  // direct memory access

  /**
   * Get if the current buffer is still accessible or if it was released already.
   *
   * @return if the current buffer is still accessible.
   */
  boolean accessible();

  /**
   * Get the amount of acquires that this data buf has. Initially a data buf is acquired once.
   *
   * @return the amount of acquires. A value smaller or equal to zero means that the buffer was released.
   */
  int acquires();

  /**
   * Acquires this buffer once. If a buffer gets acquired, further calls to {@link #release()} will decrease the count,
   * but only release the buffer if there were more release than acquire calls.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @NonNull
  DataBuf acquire();

  /**
   * Explicitly releases all data associated with this buffer making it unavailable for further reads. This method only
   * decreases the acquire count of the buffer in case it was acquired at least once.
   */
  void release();

  /**
   * Explicitly releases all data associated with this buffer making it unavailable for further reads. This method does
   * not check if anyone acquired the buffer, it will be released in any case.
   */
  void forceRelease();

  /**
   * Explicitly releases all data associated with this buffer making it unavailable for further reads. This method does
   * nothing if releasing was disables before calling this method.
   */
  @Override
  void close();

  /**
   * Represents a mutable version of a data buf.
   *
   * @since 4.0
   */
  interface Mutable extends DataBuf {

    /**
     * Writes the given boolean at the current writer index, increasing the index by one.
     *
     * @param b the boolean to write.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeBoolean(boolean b);

    /**
     * Writes the given integer at the current writer index, increasing the index by four.
     *
     * @param integer the integer to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeInt(int integer);

    /**
     * Writes the given byte at the current writer index, increasing the index by one.
     *
     * @param b the byte to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeByte(byte b);

    /**
     * Writes the given short at the current writer index, increasing the index by two.
     *
     * @param s the short to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeShort(short s);

    /**
     * Writes the given long at the current writer index, increasing the index by eight.
     *
     * @param l the long to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeLong(long l);

    /**
     * Writes the given float at the current writer index, increasing the index by four.
     *
     * @param f the float to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeFloat(float f);

    /**
     * Writes the given double at the current writer index, increasing the index by eight.
     *
     * @param d the double to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeDouble(double d);

    /**
     * Writes the given UTF-16 char at the current writer index, increasing the index by two.
     *
     * @param c the char to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeChar(char c);

    /**
     * Writes the given byte array into the buffer, prefixed by an integer containing the amount of bytes following in
     * the array.
     * <p>
     * This method call is equivalent to {@code writeByteArray(b, b.length)}.
     *
     * @param b the byte array to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeByteArray(byte[] b);

    /**
     * Writes the given byte array into the buffer, prefixed by an integer containing the amount of bytes following in
     * the array.
     *
     * @param b      the byte array to write into the buffer.
     * @param amount the amount of bytes of the array to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeByteArray(byte[] b, int amount);

    /**
     * Writes the unique id into the buffer by first writing the most significant bits of the id followed by the last
     * significant bits of the id.
     *
     * @param uuid the id to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeUniqueId(@NonNull UUID uuid);

    /**
     * Writes the string into the buffer. This method does the same thing as {@link #writeByteArray(byte[])}. The string
     * gets converted into it's byte array representation and then written into the buffer like that.
     *
     * @param string the string to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeString(@NonNull String string);

    /**
     * Writes all data of the given data buffer into this data buffer starting at the current reader index of the given
     * buffer.
     * <p>
     * Buffers are not expected to be cross-implementation-compatible. For instance, a netty buffer can only be written
     * to netty buffers.
     *
     * @param buf the buffer to write into this buffer.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    DataBuf.Mutable writeDataBuf(@NonNull DataBuf buf);

    /**
     * Writes the given object into this buffer. The object is written using the default object mapper of the system.
     *
     * @param obj the object to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @see ObjectMapper#writeObject(Mutable, Object)
     */
    @NonNull
    DataBuf.Mutable writeObject(@Nullable Object obj);

    /**
     * Writes the given object null-safe into this buffer. It appends a boolean before the actual object data (if the
     * object is present) whether the data is present. The writer consumer is only called when the data is present and
     * can then safely proceed to write all the required data into the buffer. The supplied buffer is the same buffer
     * used for calling the method.
     *
     * @param object             the object which should be safely written into this buffer.
     * @param handlerWhenNonNull the writer of the object when it's non-null.
     * @param <T>                the generic type of the object being written.
     * @return the same buffer used to call the method, for chaining.
     */
    @NonNull
    <T> DataBuf.Mutable writeNullable(
      @Nullable T object,
      @NonNull BiConsumer<Mutable, T> handlerWhenNonNull);

    /**
     * Ensures that this buffer has at least the given amount of bytes unused for writing data. If the buffer already
     * has the amount of bytes present, this method returns immediately.
     *
     * @param bytes the bytes that must be available in the buffer.
     * @return this buffer, for chaining.
     * @throws IllegalArgumentException if the given byte count is negative.
     */
    @NonNull
    DataBuf.Mutable ensureWriteable(int bytes);

    // utility for reading

    /**
     * Converts this buffer into an immutable version of it. The underlying buffer is not expected to be clones, that
     * means that writes to this buffer are still reflected into the immutable version of it and vise-versa.
     *
     * @return an immutable version of this buffer.
     */
    @NonNull
    DataBuf asImmutable();
  }
}
