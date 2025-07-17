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
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an immutable buffer, which is essentially a wrapper around some kind of readable buffer. By default,
 * CloudNet wraps netty buffer instances and delegates each method call to them.
 * <p>
 * However, a data buf does not allow (in comparison to other wrappers) the random access to bytes at specific
 * positions. But it must be possible for a reader to store the current position of the buffer and return to it (for
 * example, after reading). This is done by starting a transaction using {@link #startTransaction()}, reading or writing
 * to the buffer and restoring the previous position by using {@link #redoTransaction()}. Note: This will not remove
 * bytes written to the buffer, other writes will, however, start from the original index and override the written
 * bytes.
 * <p>
 * Other operations should work as expected on a buffer, reading should always start from the head of the buffer,
 * reflecting the operation over all other readers. If one reader reads a byte from the buffer, the next one will start
 * at the second byte in the buffer, not the first one.
 * <p>
 * Buffers are not required to be thread safe, they should be treated specially in these cases. Concurrent read and/or
 * write operations might therefore produce (by default) unspecified results when data buffers are accessed
 * concurrently.
 * <p>
 * To prevent exceptions during reading, it's worth noting that using {@code readableBytes() > 0} it is possible to
 * verify that there are still bytes left in the buffer to read.
 * <p>
 * It is not recommended using any constructor to create an instance of a data buf - you should get a factory instance
 * for them and create your instance using the given factory methods.
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
   * Get the remaining number of bytes that are filled with readable content.
   *
   * @return the remaining number of bytes that are filled with readable content.
   */
  int readableBytes();

  /**
   * Get the current reader offset. The next read to this buffer will happen at the returned offset.
   *
   * @return the current reader offset.
   */
  int readerOffset();

  /**
   * Sets the current reader offset of this buffer. The next read will happen from the given offset.
   *
   * @param offset the new reader offset to use.
   * @return this buffer, for chaining.
   * @throws IllegalStateException     if this buffer was released.
   * @throws IndexOutOfBoundsException if the given offset is beyond the end of this buffer.
   */
  @NonNull
  @Contract("_ -> this")
  DataBuf readerOffset(int offset);

  /**
   * Advances the current reader offset of this buffer by the given delta. The next read to this buffer happens at the
   * current reader index plus the given delta. Note: the given delta cannot be negative.
   *
   * @param delta the number of bytes to move the reader index by.
   * @return this buffer, for chaining.
   * @throws IllegalArgumentException  if the given delta is negative.
   * @throws IllegalStateException     if this buffer was released.
   * @throws IndexOutOfBoundsException if advancing by the given delta would move beyond the end of this buffer.
   */
  @NonNull
  @Contract("_ -> this")
  DataBuf advanceReaderOffset(int delta);

  /**
   * Get a byte buffer instance that shares the memory region of this buffer. The returned buffer is marked as read-only
   * which prevents write operations to it. The initial byte buffer offset is the current reader offset, and it's
   * limited to the number of readable bytes beyond the current reader offset.
   * <p>
   * Note: this api is marked as experimental as the lifecycle of the returned buffer cannot be controlled. This means
   * that a returned byte buffer instance can still refer to memory already released by this buffer.
   *
   * @return a read-only byte buffer sharing the memory region of this buffer, but with a separate position.
   * @throws IllegalStateException if this buffer was released.
   */
  @NonNull
  @ApiStatus.Experimental
  ByteBuffer readableNioBuffer();

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
   * @throws IllegalStateException     if this buffer was released.
   * @throws IndexOutOfBoundsException if an illegal action was made to buffer moving the reader or writer index.
   */
  @NonNull
  DataBuf redoTransaction();

  /**
   * Converts this immutable buffer to a mutable one. The underlying memory is not shared between this buffer and the
   * newly constructed mutable one. The returned buffer range starts at the current reader position of this buffer.
   *
   * @return a mutable variant of this buffer.
   * @throws IllegalStateException if this buffer was released.
   */
  @NonNull
  DataBuf.Mutable asMutable();

  // lifecycle management

  /**
   * Get if the current buffer is still accessible or if it was released already.
   *
   * @return if the current buffer is still accessible.
   */
  boolean accessible();

  /**
   * Get the amount of times this buffer was acquired. A number greater than zero indicates that this buffer is
   * accessible and not released, a number equal or less than zero indicates that this buffer was released and is
   * inaccessible.
   *
   * @return the amount of times this buffer was acquired.
   */
  int acquires();

  /**
   * Acquires this buffer once. If a buffer gets acquired, further calls to {@link #release()} will decrease the count,
   * but only release the buffer if there were more release than acquire calls.
   *
   * @return the same instance as used to call the method, for chaining.
   * @throws IllegalStateException if this buffer was released or was acquired too many times.
   */
  @NonNull
  DataBuf acquire();

  /**
   * Closes this buffer. In case the acquire count is exactly {@code 1}, the buffer content will be released and this
   * buffer becomes inaccessible. If the acquire count is greater than {@code 1}, the acquire count is decreased by one
   * and the buffer stays accessible. If the buffer was already released, this method does nothing.
   */
  void release();

  /**
   * Forcibly closes the buffer, ignoring the current acquire count. The buffer will always be inaccessible after this
   * method was invoked.
   */
  void forceRelease();

  /**
   * Closes this buffer. In case the acquire count is exactly {@code 1}, the buffer content will be released and this
   * buffer becomes inaccessible. If the acquire count is greater than {@code 1}, the acquire count is decreased by one
   * and the buffer stays accessible. If the buffer was already released, this method does nothing.
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
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeBoolean(boolean b);

    /**
     * Writes the given integer at the current writer index, increasing the index by four.
     *
     * @param integer the integer to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeInt(int integer);

    /**
     * Writes the given byte at the current writer index, increasing the index by one.
     *
     * @param b the byte to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeByte(byte b);

    /**
     * Writes the given short at the current writer index, increasing the index by two.
     *
     * @param s the short to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeShort(short s);

    /**
     * Writes the given long at the current writer index, increasing the index by eight.
     *
     * @param l the long to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeLong(long l);

    /**
     * Writes the given float at the current writer index, increasing the index by four.
     *
     * @param f the float to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeFloat(float f);

    /**
     * Writes the given double at the current writer index, increasing the index by eight.
     *
     * @param d the double to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeDouble(double d);

    /**
     * Writes the given UTF-16 char at the current writer index, increasing the index by two.
     *
     * @param c the char to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeChar(char c);

    /**
     * Writes the given byte array into the buffer, prefixed by an integer containing the number of bytes following in
     * the array.
     * <p>
     * This method call is equivalent to {@code writeByteArray(b, b.length)}.
     *
     * @param b the byte array to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeByteArray(byte[] b);

    /**
     * Writes the given byte array into the buffer, prefixed by an integer containing the number of bytes following in
     * the array.
     *
     * @param b      the byte array to write into the buffer.
     * @param amount the number of bytes to copy from the given byte array into this buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeByteArray(byte[] b, int amount);

    /**
     * Writes the unique id into the buffer.
     *
     * @param uuid the id to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeUniqueId(@NonNull UUID uuid);

    /**
     * Writes the string into the buffer.
     *
     * @param string the string to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable writeString(@NonNull String string);

    /**
     * Writes all data of the given data buffer into this data buffer starting at the current reader index of the given
     * buffer. The reader and writer index of the given buffer are not modified by this method. The given buffer is
     * released after being written into this buffer.
     * <p>
     * Buffers are not expected to be cross-implementation-compatible. For instance, a netty buffer can only be written
     * to netty buffers.
     *
     * @param buf the buffer to write into this buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if either this or the given buffer were released.
     */
    @NonNull
    DataBuf.Mutable writeDataBuf(@NonNull DataBuf buf);

    /**
     * Writes the given object into this buffer. The object is written using the default object mapper of the system.
     *
     * @param obj the object to write into the buffer.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     * @see ObjectMapper#writeObject(Mutable, Object)
     */
    @NonNull
    DataBuf.Mutable writeObject(@Nullable Object obj);

    /**
     * Writes the given object null-safe into this buffer. It appends a boolean before the actual object data to
     * indicate if the object is non-null. The writer consumer is only called when the data is present and can then
     * safely proceed to write all the required data into the buffer. The supplied buffer is the same buffer used for
     * calling the method.
     *
     * @param object             the object which should be safely written into this buffer.
     * @param handlerWhenNonNull the writer of the object when it's non-null.
     * @param <T>                the generic type of the object being written.
     * @return the same buffer used to call the method, for chaining.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    <T> DataBuf.Mutable writeNullable(
      @Nullable T object,
      @NonNull BiConsumer<Mutable, T> handlerWhenNonNull);

    // utility for writing

    /**
     * Ensures that this buffer has at least the given number of bytes available for writing data. If the buffer already
     * has the number of bytes present, this method returns immediately.
     *
     * @param bytes the number of bytes that should be available for writing.
     * @return this buffer, for chaining.
     * @throws IllegalArgumentException if the given byte count is negative.
     * @throws IllegalStateException    if this buffer was released.
     */
    @NonNull
    DataBuf.Mutable ensureWriteable(int bytes);

    /**
     * Get the remaining number of bytes available for write operations.
     *
     * @return the remaining number of bytes available for write operations.
     */
    int writeableBytes();

    /**
     * Get the current writer offset. The next write operation to this buffer will happen at the returned offset.
     *
     * @return the current writer offset.
     */
    int writerOffset();

    /**
     * Sets the current writer offset of this buffer. The next write operation will happen from the given offset.
     *
     * @param offset the new writer offset to use.
     * @return this buffer, for chaining.
     * @throws IllegalStateException     if this buffer was released.
     * @throws IndexOutOfBoundsException if the given offset is beyond the end of this buffer.
     */
    @NonNull
    @Contract("_ -> this")
    DataBuf writerOffset(int offset);

    /**
     * Advances the current writer offset of this buffer by the given delta. The next write operation to this buffer
     * happens at the current writer index plus the given delta. Note: the given delta cannot be negative.
     *
     * @param delta the number of bytes to move the writer index by.
     * @return this buffer, for chaining.
     * @throws IllegalArgumentException  if the given delta is negative.
     * @throws IllegalStateException     if this buffer was released.
     * @throws IndexOutOfBoundsException if advancing by the given delta would move beyond the end of this buffer.
     */
    @NonNull
    @Contract("_ -> this")
    DataBuf advanceWriterOffset(int delta);

    /**
     * Get a byte buffer instance that shares the memory region of this buffer. The returned buffer can be used to read
     * and write data to this buffer. The initial byte buffer offset is the current writer offset, and it's limited to
     * the number of writable bytes beyond the current writer offset.
     * <p>
     * Note: this api is marked as experimental as the lifecycle of the returned buffer cannot be controlled. This means
     * that a returned byte buffer instance can still refer to memory already released by this buffer.
     *
     * @return a byte buffer sharing the memory region of this buffer, but with a separate position.
     * @throws IllegalStateException if this buffer was released.
     */
    @NonNull
    @ApiStatus.Experimental
    ByteBuffer writeableNioBuffer();

    /**
     * Wraps the underlying buffer into a read-only variant. The underlying memory, lifetime and reader/writer positions
     * are shared between this buffer and the read-only variant.
     *
     * @return an immutable wrap of this buffer.
     */
    @NonNull
    DataBuf asImmutable();
  }
}
