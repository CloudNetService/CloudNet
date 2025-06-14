/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.netty.memory;

import io.netty5.buffer.AllocatorControl;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferClosedException;
import io.netty5.buffer.BufferComponent;
import io.netty5.buffer.BufferReadOnlyException;
import io.netty5.buffer.ByteCursor;
import io.netty5.buffer.ComponentIterator;
import io.netty5.buffer.Drop;
import io.netty5.buffer.Owned;
import io.netty5.buffer.internal.AdaptableBuffer;
import io.netty5.buffer.internal.InternalBufferUtils;
import io.netty5.util.internal.ObjectUtil;
import io.netty5.util.internal.PlatformDependent;
import io.netty5.util.internal.SWARUtil;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

// Modified version of the MemSegBuffer implementation by netty (The Netty Project)
// Licensed under the Apache 2.0 License
// https://github.com/netty/netty/blob/6d442645e0e1f7e3ee541fc82c283ca8e1df83e2/buffer-memory-segment/src/main/java/io/netty5/buffer/memseg/MemSegBuffer.java

/**
 * Netty buffer implementation that is backed by a memory segment.
 *
 * @since 4.0
 */
final class MemorySegmentBuffer
  extends AdaptableBuffer<MemorySegmentBuffer>
  implements BufferComponent, ComponentIterator<MemorySegmentBuffer>, ComponentIterator.Next {

  // Memory segment that indicates that the associated buffer has been closed and cannot be accessed anymore.
  private static final MemorySegment CLOSED_SEGMENT = MemorySegment.ofAddress(0L);

  // VarHandles for unaligned, big endian access into a memory segment
  private static final VarHandle LAYOUT_BYTE = layoutVarHandle(ValueLayout.JAVA_BYTE);
  private static final VarHandle LAYOUT_SHORT = layoutVarHandle(ValueLayout.JAVA_SHORT);
  private static final VarHandle LAYOUT_INT = layoutVarHandle(ValueLayout.JAVA_INT);
  private static final VarHandle LAYOUT_LONG = layoutVarHandle(ValueLayout.JAVA_LONG);
  private static final VarHandle LAYOUT_FLOAT = layoutVarHandle(ValueLayout.JAVA_FLOAT);
  private static final VarHandle LAYOUT_DOUBLE = layoutVarHandle(ValueLayout.JAVA_DOUBLE);
  private static final VarHandle LAYOUT_CHAR = layoutVarHandle(ValueLayout.JAVA_CHAR);

  // util to unchecked load a byte from a buffer, without access checks
  private static final InternalBufferUtils.UncheckedLoadByte UNCHECKED_LOAD_BYTE = (buffer, offset) -> {
    var memSegBuffer = (MemorySegmentBuffer) buffer;
    return (byte) LAYOUT_BYTE.get(memSegBuffer.readSegment, offset);
  };

  // constructor for an UOE if a component array is accessed that actually doesn't exist
  private static final Supplier<UnsupportedOperationException> NO_BACKING_ARRAY =
    () -> new UnsupportedOperationException("This component has no backing array");

  // original memory segment, memory segment for reading, memory segment for writing
  private MemorySegment baseSegment;
  private MemorySegment readSegment;
  private MemorySegment writeSegment;

  // current reader and writer offset into the buffer, applied capacity limit
  private int readerOffset;
  private int writerOffset;
  private int capacityLimit;

  /**
   * Constructs a new memory segment buffer based on the given base segment.
   *
   * @param baseSegment the base segment for the buffer, also used for reading and writing.
   * @param control     the controller for the allocator that requested the allocation.
   * @param drop        the drop which is associated with this buffer.
   * @throws NullPointerException if the given base segment, control or drop is null.
   */
  MemorySegmentBuffer(
    @NonNull MemorySegment baseSegment,
    @NonNull AllocatorControl control,
    @NonNull Drop<MemorySegmentBuffer> drop
  ) {
    this(baseSegment, baseSegment, control, drop);
  }

  /**
   * Constructs a new memory segment buffer based on the given memory segments.
   *
   * @param baseSegment the base segment for the buffer.
   * @param rwSegment   the segment to use for reading and writing.
   * @param control     the controller for the allocator that requested the allocation.
   * @param drop        the drop which is associated with this buffer.
   * @throws NullPointerException if the given base segment, rw-segment, control or drop is null.
   */
  private MemorySegmentBuffer(
    @NonNull MemorySegment baseSegment,
    @NonNull MemorySegment rwSegment,
    @NonNull AllocatorControl control,
    @NonNull Drop<MemorySegmentBuffer> drop
  ) {
    super(drop, control);
    this.baseSegment = baseSegment;
    this.readSegment = rwSegment;
    this.writeSegment = rwSegment;
    this.capacityLimit = InternalBufferUtils.MAX_BUFFER_SIZE;
  }

  /**
   * Constructs a buffer that represents a constant child of the given parent buffer.
   *
   * @param parent the parent buffer of this constant child buffer.
   * @param drop   the drop that is associated with this child buffer.
   * @throws NullPointerException if the given parent buffer or drop is null.
   */
  private MemorySegmentBuffer(@NonNull MemorySegmentBuffer parent, @NonNull Drop<MemorySegmentBuffer> drop) {
    super(drop, parent.control);

    this.baseSegment = parent.baseSegment;
    this.readSegment = parent.readSegment;
    this.writeSegment = parent.writeSegment;

    this.readerOffset = parent.readerOffset;
    this.writerOffset = parent.writerOffset;
    this.capacityLimit = parent.capacityLimit;
  }

  /**
   * Creates a {@code VarHandle} that can be used for unaligned, big endian access into a memory segment.
   *
   * @param layout the layout to get the described var handle for.
   * @return a var handle for unaligned, big endian access into a memory segment.
   * @throws NullPointerException if the given value layout is null.
   */
  private static @NonNull VarHandle layoutVarHandle(@NonNull ValueLayout layout) {
    return layout.withByteAlignment(Byte.BYTES).withOrder(ByteOrder.BIG_ENDIAN).varHandle();
  }

  // <editor-fold defaultstate="collapsed" desc="Buffer Implementation">

  /**
   * {@inheritDoc}
   */
  @Override
  public int capacity() {
    return (int) this.readSegment.byteSize();
  }

  @Override
  public int readerOffset() {
    return this.readerOffset;
  }

  @Override
  public @NonNull MemorySegmentBuffer skipReadableBytes(int delta) {
    return (MemorySegmentBuffer) super.skipReadableBytes(delta);
  }

  @Override
  public @NonNull MemorySegmentBuffer readerOffset(int offset) {
    this.ensureReadable(offset, 0);
    this.readerOffset = offset;
    return this;
  }

  @Override
  public int writerOffset() {
    return this.writerOffset;
  }

  @Override
  public @NonNull MemorySegmentBuffer skipWritableBytes(int delta) {
    return (MemorySegmentBuffer) super.skipWritableBytes(delta);
  }

  @Override
  public @NonNull MemorySegmentBuffer writerOffset(int offset) {
    this.ensureWriteable(offset, 0, false);
    this.writerOffset = offset;
    return this;
  }

  @Override
  public int readableBytes() {
    return super.readableBytes();
  }

  @Override
  public int writableBytes() {
    return super.writableBytes();
  }

  @Override
  public @NonNull MemorySegmentBuffer fill(byte value) {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(); // ensure not read-only
    this.writeSegment.fill(value);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer makeReadOnly() {
    this.writeSegment = CLOSED_SEGMENT;
    return this;
  }

  @Override
  public boolean readOnly() {
    return this.readSegment != this.writeSegment;
  }

  @Override
  public boolean isDirect() {
    return this.readSegment.isNative();
  }

  @Override
  public @NonNull MemorySegmentBuffer implicitCapacityLimit(int limit) {
    InternalBufferUtils.checkImplicitCapacity(limit, this.capacity());
    this.capacityLimit = limit;
    return this;
  }

  @Override
  public int implicitCapacityLimit() {
    return this.capacityLimit;
  }

  @Override
  public void copyInto(int srcPos, byte[] dest, int destPos, int length) {
    this.ensureAccessible(); // ensure not closed
    this.ensureReadable(srcPos, length); // ensure at least length bytes are in this buffer
    Objects.checkFromIndexSize(destPos, length, dest.length); // ensure target can contain at least length bytes

    if (this.hasReadableArray()) {
      var srcArray = this.readableArray();
      System.arraycopy(srcArray, srcPos, dest, destPos, length);
    } else {
      var destSegment = MemorySegment.ofArray(dest);
      MemorySegment.copy(this.readSegment, srcPos, destSegment, destPos, length);
    }
  }

  @Override
  public void copyInto(int srcPos, @NonNull ByteBuffer dest, int destPos, int length) {
    this.ensureAccessible(); // ensure not closed
    this.ensureReadable(srcPos, length); // ensure at least length bytes are in this buffer
    Objects.checkFromIndexSize(destPos, length, dest.limit()); // ensure target can contain at least length bytes
    if (dest.isReadOnly()) {
      throw new ReadOnlyBufferException();
    }

    if (dest.hasArray() && this.hasReadableArray()) {
      var srcArray = this.readableArray();
      var destArray = dest.array();
      var destFrom = dest.arrayOffset() + destPos;
      System.arraycopy(srcArray, srcPos, destArray, destFrom, length);
    } else {
      var cleanedBuffer = dest.duplicate().clear();
      var destSegment = MemorySegment.ofBuffer(cleanedBuffer);
      MemorySegment.copy(this.readSegment, srcPos, destSegment, destPos, length);
    }
  }

  @Override
  public void copyInto(int srcPos, @NonNull Buffer dest, int destPos, int length) {
    this.ensureAccessible(); // ensure not closed
    this.ensureReadable(srcPos, length); // ensure at least length bytes are in this buffer
    if (dest.readOnly()) {
      throw InternalBufferUtils.bufferIsReadOnly(dest);
    }

    if (dest instanceof MemorySegmentBuffer buffer) {
      buffer.ensureInSegmentBounds(destPos, length); // ensure dest not closed and has enough space
      MemorySegment.copy(this.readSegment, srcPos, buffer.writeSegment, destPos, length);
      return;
    }

    InternalBufferUtils.copyToViaReverseLoop(this, srcPos, dest, destPos, length);
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // exact same impl as other transferTo() method
  public int transferTo(@NonNull WritableByteChannel channel, int length) throws IOException {
    this.ensureAccessible(); // ensure not closed

    var actualLength = Math.min(this.readableBytes(), length);
    if (actualLength == 0) {
      return 0;
    }

    var readerOffset = this.readerOffset();
    this.ensureReadable(readerOffset, actualLength);

    var srcBuffer = this.readableBuffer().limit(readerOffset + actualLength);
    var written = channel.write(srcBuffer);
    this.skipReadableBytes(written);
    return written;
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // exact same impl as other transferTo() method
  public int transferTo(@NonNull FileChannel channel, long position, int length) throws IOException {
    this.ensureAccessible(); // ensure not closed

    var actualLength = Math.min(this.readableBytes(), length);
    if (actualLength == 0) {
      return 0;
    }

    var readerOffset = this.readerOffset();
    this.ensureReadable(readerOffset, actualLength);

    var srcBuffer = this.readableBuffer().limit(readerOffset + actualLength);
    var written = channel.write(srcBuffer);
    this.skipReadableBytes(written);
    return written;
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // exact same impl as other transferFrom() method
  public int transferFrom(@NonNull FileChannel channel, long position, int length) throws IOException {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(); // ensure not read-only
    ObjectUtil.checkPositiveOrZero(position, "position");
    ObjectUtil.checkPositiveOrZero(length, "length");

    var actualLength = Math.min(this.writableBytes(), length);
    if (actualLength == 0) {
      return 0;
    }

    var writerOffset = this.writerOffset();
    var destBuffer = this.writableBuffer().limit(writerOffset + actualLength);
    var read = channel.read(destBuffer, position);
    if (read > 0) {
      this.skipReadableBytes(read);
    }

    return read;
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // exact same impl as other transferFrom() method
  public int transferFrom(@NonNull ReadableByteChannel channel, int length) throws IOException {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(); // ensure not read-only
    ObjectUtil.checkPositiveOrZero(length, "length");

    var actualLength = Math.min(this.writableBytes(), length);
    if (actualLength == 0) {
      return 0;
    }

    var writerOffset = this.writerOffset();
    var destBuffer = this.writableBuffer().limit(writerOffset + actualLength);
    var read = channel.read(destBuffer);
    if (read > 0) {
      this.skipReadableBytes(read);
    }

    return read;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeCharSequence(@NonNull CharSequence source, @NonNull Charset charset) {
    InternalBufferUtils.writeCharSequence(source, this, charset);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeBytes(byte[] source) {
    return this.writeBytes(source, 0, source.length);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeBytes(byte[] source, int srcPos, int length) {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(this.writerOffset, length, true); // ensure writeable and enough space

    if (this.hasWritableArray()) {
      var destArray = this.writableArray();
      var destArrayOff = this.writableArrayOffset();
      System.arraycopy(source, srcPos, destArray, destArrayOff, length);
    } else {
      var srcSegment = MemorySegment.ofArray(source);
      MemorySegment.copy(srcSegment, srcPos, this.writeSegment, this.writerOffset, length);
    }

    this.skipWritableBytes(length);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeBytes(@NonNull ByteBuffer source) {
    var length = source.remaining();
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(this.writerOffset, length, true); // ensure writeable and enough space

    if (this.hasWritableArray()) {
      var destArray = this.writableArray();
      var destArrayOff = this.writableArrayOffset();
      source.get(destArray, destArrayOff, length);
    } else {
      var srcSegment = MemorySegment.ofBuffer(source);
      MemorySegment.copy(srcSegment, source.position(), this.writeSegment, this.writerOffset, length);
    }

    this.skipWritableBytes(length);
    return this;
  }

  @Override
  public int bytesBefore(byte needle) {
    this.ensureAccessible(); // ensure not closed

    var max = this.writerOffset();
    var offset = this.readerOffset();

    var readable = this.readableBytes();
    if (readable > 7) {
      // For the details of this algorithm, see Hacker's Delight, Chapter 6, Searching Words.
      // Richard Startin also describes this on his blog: https://richardstartin.github.io/posts/finding-bytes
      var pattern = SWARUtil.compilePattern(needle);
      for (var end = offset + (readable >>> 3) * Long.BYTES; offset < end; offset += Long.BYTES) {
        var word = (long) LAYOUT_LONG.get(this.readSegment, offset);
        var result = SWARUtil.applyPattern(word, pattern);
        if (result != 0) {
          var idx = SWARUtil.getIndex(result, true);
          return offset - this.readerOffset + idx;
        }
      }
    }

    for (; offset < max; offset++) {
      var word = (byte) LAYOUT_BYTE.get(this.readSegment, offset);
      if (word == needle) {
        return offset - this.readerOffset;
      }
    }

    return -1;
  }

  @Override
  public int bytesBefore(@NonNull Buffer needle) {
    var needleUncheckedGetByte = needle instanceof MemorySegmentBuffer ? UNCHECKED_LOAD_BYTE : null;
    return InternalBufferUtils.bytesBefore(this, UNCHECKED_LOAD_BYTE, needle, needleUncheckedGetByte);
  }

  @Override
  public @NonNull ByteCursor openCursor() {
    return this.openCursor(this.readerOffset(), this.readableBytes());
  }

  @Override
  public @NonNull ByteCursor openCursor(int fromOffset, int length) {
    this.ensureAccessible(); // ensure not closed
    Objects.checkFromIndexSize(fromOffset, length, this.capacity());
    return new ByteCursor() {
      final MemorySegment segment = MemorySegmentBuffer.this.readSegment;
      final int max = fromOffset + length;

      int index = fromOffset;
      byte currentValue = -1;

      @Override
      public boolean readByte() {
        if (index < max) {
          this.currentValue = (byte) LAYOUT_BYTE.get(this.segment, this.index);
          this.index++;
          return true;
        }
        return false;
      }

      @Override
      public byte getByte() {
        return this.currentValue;
      }

      @Override
      public int currentOffset() {
        return this.index;
      }

      @Override
      public int bytesLeft() {
        return this.max - this.index;
      }
    };
  }

  @Override
  public @NonNull ByteCursor openReverseCursor() {
    int writerOff = this.writerOffset();
    return openReverseCursor(writerOff == 0 ? 0 : writerOff - 1, this.readableBytes());
  }

  @Override
  public @NonNull ByteCursor openReverseCursor(int fromOffset, int length) {
    this.ensureAccessible(); // ensure not closed
    Objects.checkIndex(fromOffset - length, this.capacity());
    return new ByteCursor() {
      final MemorySegment segment = MemorySegmentBuffer.this.readSegment;
      final int max = fromOffset - length;

      int index = fromOffset;
      byte currentValue = -1;

      @Override
      public boolean readByte() {
        if (index > max) {
          this.currentValue = (byte) LAYOUT_BYTE.get(this.segment, this.index);
          this.index--;
          return true;
        }
        return false;
      }

      @Override
      public byte getByte() {
        return this.currentValue;
      }

      @Override
      public int currentOffset() {
        return this.index;
      }

      @Override
      public int bytesLeft() {
        return this.index - this.max;
      }
    };
  }

  @Override
  public @NonNull MemorySegmentBuffer ensureWritable(int size) {
    return this.ensureWritable(size, this.capacity(), true);
  }

  @Override
  public @NonNull MemorySegmentBuffer ensureWritable(int size, int minimumGrowth, boolean allowCompaction) {
    this.ensureAccessible(); // ensure not closed
    this.ensureOwned(); // ensure owned
    this.ensureWriteable(); // ensure not read-only
    ObjectUtil.checkPositive(size, "size");
    ObjectUtil.checkPositive(minimumGrowth, "minimumGrowth");

    // check if buffer has enough space already, nothing to do in this case
    var currentWritable = this.writableBytes();
    if (currentWritable >= size) {
      return this;
    }

    // check if removing the already read bytes (compaction) would allow the buffer to reach the expected size
    var readerOffset = this.readerOffset();
    var removableBytes = currentWritable + readerOffset;
    if (allowCompaction && removableBytes > size) {
      return this.compact();
    }

    // allocate a new, bigger buffer
    var capacity = this.capacity();
    var growth = (long) Math.max(minimumGrowth, size - currentWritable);
    var newBufferSize = capacity + growth;
    InternalBufferUtils.assertValidBufferSize(newBufferSize);
    var newBuffer = (MemorySegmentBuffer) this.control.getAllocator().allocate((int) newBufferSize);

    // copy current buffer content into the new buffer
    this.copyInto(0, newBuffer, 0, capacity);

    // disconnect old Drop and set Drop of the new buffer in this buffer
    var writerOffset = this.writerOffset();
    var newDrop = newBuffer.unsafeGetDrop();
    this.unsafeGetDrop().drop(this);
    this.unsafeSetDrop(newDrop);
    this.readerOffset = readerOffset;
    this.writerOffset = writerOffset;

    // attach the memory segment of new buffer to this buffer
    this.baseSegment = newBuffer.baseSegment;
    this.readSegment = newBuffer.readSegment;
    this.writeSegment = newBuffer.writeSegment;

    // actually attach the Drop of the new buffer to this buffer
    newDrop.attach(this);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer copy() {
    return this.copy(false);
  }

  @Override
  public @NonNull MemorySegmentBuffer copy(int offset, int length) {
    return this.copy(offset, length, false);
  }

  @Override
  public @NonNull MemorySegmentBuffer copy(boolean readOnly) {
    return this.copy(this.readerOffset(), this.readableBytes(), readOnly);
  }

  @Override
  public @NonNull MemorySegmentBuffer copy(int offset, int length, boolean readOnly) {
    this.ensureReadable(offset, length);
    ObjectUtil.checkPositiveOrZero(length, "length");

    // if this buffer is read-only and a read-only buffer was requested, they can share the underlying memory
    if (readOnly && this.readOnly()) {
      var child = this.newConstChild();
      child.readSegment = this.readSegment.asSlice(offset, length);
      child.readerOffset = 0;
      child.writerOffset = length;
      return child;
    }

    // allocate a new buffer and copy this buffer data into the new buffer
    var child = (MemorySegmentBuffer) this.control.getAllocator().allocate(length);
    try {
      this.copyInto(offset, child, 0, length);
      child.readerOffset(0);
      child.writerOffset(length);
      if (readOnly) {
        child.makeReadOnly();
      }
    } catch (Throwable throwable) {
      child.close();
      throw throwable;
    }

    return child;
  }

  @Override
  public @NonNull MemorySegmentBuffer readSplit(int length) {
    return this.split(this.readerOffset() + length);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeSplit(int length) {
    return this.split(this.writerOffset() + length);
  }

  @Override
  public @NonNull MemorySegmentBuffer split() {
    return this.split(this.writerOffset());
  }

  @Override
  public @NonNull MemorySegmentBuffer split(int splitOffset) {
    this.ensureAccessible(); // ensure not closed
    this.ensureOwned(); // ensure owned
    ObjectUtil.checkPositiveOrZero(splitOffset, "splitOffset");
    Objects.checkIndex(splitOffset, this.capacity());

    // construct a new buffer containing the split region
    var drop = this.unsafeGetDrop().fork();
    var segment = this.readSegment.asSlice(0, splitOffset);
    var splitBuf = new MemorySegmentBuffer(this.baseSegment, segment, this.control, drop);
    drop.attach(splitBuf);

    // move reader/writer offset of the new buffer to the current position or the split position
    splitBuf.readerOffset = Math.min(this.readerOffset(), splitOffset);
    splitBuf.writerOffset = Math.min(this.writerOffset(), splitOffset);

    // keep read-only characteristics of this buffer in the new buffer
    var readOnly = this.readOnly();
    if (readOnly) {
      splitBuf.makeReadOnly();
    }

    // update the backing segment of this buffer to the new owned region
    this.readSegment = this.readSegment.asSlice(splitOffset, this.capacity() - splitOffset);
    if (!readOnly) {
      this.writeSegment = this.readSegment;
    }

    // move the reader/writer index of this buffer to fit the new region
    this.readerOffset = Math.max(this.readerOffset, splitOffset) - splitOffset;
    this.writerOffset = Math.max(this.writerOffset, splitOffset) - splitOffset;
    return splitBuf;
  }

  @Override
  public @NonNull MemorySegmentBuffer compact() {
    this.ensureAccessible(); // ensure not closed
    this.ensureOwned(); // ensure owned
    this.ensureWriteable(); // ensure not read-only

    // check if there are any bytes that can be discarded at all
    var readerOffset = this.readerOffset();
    if (readerOffset == 0) {
      return this;
    }

    // copy the bytes starting from the current reader offset to the beginning of the buffer
    var writerOffset = this.writerOffset();
    var length = writerOffset - readerOffset;
    MemorySegment.copy(this.readSegment, readerOffset, this.writeSegment, 0, length);

    // move the reader/writer index back to according the count of copied bytes
    this.readerOffset(0);
    this.writerOffset(writerOffset - readerOffset);
    return this;
  }

  @Override
  public int countComponents() {
    return 1;
  }

  @Override
  public int countReadableComponents() {
    return this.readableBytes() > 0 ? 1 : 0;
  }

  @Override
  public int countWritableComponents() {
    return this.writableBytes() > 0 ? 1 : 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BufferComponent & Next> @NonNull ComponentIterator<T> forEachComponent() {
    return (ComponentIterator<T>) this.acquire();
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="BufferAccessor Implementation">

  @Override
  public byte readByte() {
    this.ensureReadable(this.readerOffset, Byte.BYTES);
    var b = (byte) LAYOUT_BYTE.get(this.readSegment, this.readerOffset);
    this.readerOffset += Byte.BYTES;
    return b;
  }

  @Override
  public byte getByte(int roff) {
    this.ensureInSegmentBounds(roff, Byte.BYTES);
    return (byte) LAYOUT_BYTE.get(this.readSegment, roff);
  }

  @Override
  public int readUnsignedByte() {
    var b = this.readByte();
    return b & 0xFF;
  }

  @Override
  public int getUnsignedByte(int roff) {
    var b = this.getByte(roff);
    return b & 0xFF;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeByte(byte value) {
    this.ensureWriteable(this.writerOffset, Byte.BYTES, true);
    LAYOUT_BYTE.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Byte.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setByte(int woff, byte value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Byte.BYTES);
    LAYOUT_BYTE.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeUnsignedByte(int value) {
    return this.writeByte((byte) (value & 0xFF));
  }

  @Override
  public @NonNull MemorySegmentBuffer setUnsignedByte(int woff, int value) {
    return this.setByte(woff, (byte) (value & 0xFF));
  }

  @Override
  public char readChar() {
    this.ensureReadable(this.readerOffset, Character.BYTES);
    var c = (char) LAYOUT_CHAR.get(this.readSegment, this.readerOffset);
    this.readerOffset += Character.BYTES;
    return c;
  }

  @Override
  public char getChar(int roff) {
    this.ensureInSegmentBounds(roff, Character.BYTES);
    return (char) LAYOUT_CHAR.get(this.readSegment, roff);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeChar(char value) {
    this.ensureWriteable(this.writerOffset, Character.BYTES, true);
    LAYOUT_CHAR.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Character.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setChar(int woff, char value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Character.BYTES);
    LAYOUT_BYTE.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public short readShort() {
    this.ensureReadable(this.readerOffset, Short.BYTES);
    var s = (short) LAYOUT_SHORT.get(this.readSegment, this.readerOffset);
    this.readerOffset += Short.BYTES;
    return s;
  }

  @Override
  public short getShort(int roff) {
    this.ensureInSegmentBounds(roff, Short.BYTES);
    return (short) LAYOUT_SHORT.get(this.readSegment, roff);
  }

  @Override
  public int readUnsignedShort() {
    var s = this.readShort();
    return s & 0xFFFF;
  }

  @Override
  public int getUnsignedShort(int roff) {
    var s = this.getShort(roff);
    return s & 0xFFFF;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeShort(short value) {
    this.ensureWriteable(this.writerOffset, Short.BYTES, true);
    LAYOUT_SHORT.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Short.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setShort(int woff, short value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Short.BYTES);
    LAYOUT_SHORT.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeUnsignedShort(int value) {
    return this.writeShort((short) (value & 0xFFFF));
  }

  @Override
  public @NonNull MemorySegmentBuffer setUnsignedShort(int woff, int value) {
    return this.setShort(woff, (short) (value & 0xFFFF));
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // no, readUnsignedMedium() is actually not the same
  public int readMedium() {
    this.ensureReadable(this.readerOffset, 3); // 3 bytes wide
    var b0 = (byte) LAYOUT_BYTE.get(this.readSegment, this.readerOffset);
    var b1 = (byte) LAYOUT_BYTE.get(this.readSegment, this.readerOffset + 1);
    var b2 = (byte) LAYOUT_BYTE.get(this.readSegment, this.readerOffset + 2);
    this.readerOffset += 3;
    return (b0 << 16) | ((b1 & 0xFF) << 8) | (b2 & 0xFF);
  }

  @Override
  public int getMedium(int roff) {
    this.ensureInSegmentBounds(roff, 3); // 3 bytes wide
    var b0 = (byte) LAYOUT_BYTE.get(this.readSegment, roff);
    var b1 = (byte) LAYOUT_BYTE.get(this.readSegment, roff + 1);
    var b2 = (byte) LAYOUT_BYTE.get(this.readSegment, roff + 2);
    return (b0 << 16) | ((b1 & 0xFF) << 8) | (b2 & 0xFF);
  }

  @Override
  public int readUnsignedMedium() {
    var m = this.readMedium();
    return m & 0xFFFFFF;
  }

  @Override
  public int getUnsignedMedium(int roff) {
    var m = this.getMedium(roff);
    return m & 0xFFFFFF;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeMedium(int value) {
    this.ensureWriteable(this.writerOffset, 3, true); // 3 bytes wide
    LAYOUT_BYTE.set(this.writeSegment, this.writerOffset, (byte) (value >> 16));
    LAYOUT_BYTE.set(this.writeSegment, this.writerOffset + 1, (byte) (value >> 8 & 0xFF));
    LAYOUT_BYTE.set(this.writeSegment, this.writerOffset + 2, (byte) (value & 0xFF));
    this.writerOffset += 3;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setMedium(int woff, int value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, 3); // 3 bytes wide
    LAYOUT_BYTE.set(this.writeSegment, woff, (byte) (value >> 16));
    LAYOUT_BYTE.set(this.writeSegment, woff + 1, (byte) (value >> 8 & 0xFF));
    LAYOUT_BYTE.set(this.writeSegment, woff + 2, (byte) (value & 0xFF));
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeUnsignedMedium(int value) {
    return this.writeMedium(value & 0xFFFFFF);
  }

  @Override
  public @NonNull MemorySegmentBuffer setUnsignedMedium(int woff, int value) {
    return this.setMedium(woff, value & 0xFFFFFF);
  }

  @Override
  public int readInt() {
    this.ensureReadable(this.readerOffset, Integer.BYTES);
    var i = (int) LAYOUT_INT.get(this.readSegment, this.readerOffset);
    this.readerOffset += Integer.BYTES;
    return i;
  }

  @Override
  public int getInt(int roff) {
    this.ensureInSegmentBounds(roff, Integer.BYTES);
    return (int) LAYOUT_INT.get(this.readSegment, roff);
  }

  @Override
  public long readUnsignedInt() {
    var i = this.readInt();
    return i & 0xFFFFFFFFL;
  }

  @Override
  public long getUnsignedInt(int roff) {
    var i = this.getInt(roff);
    return i & 0xFFFFFFFFL;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeInt(int value) {
    this.ensureWriteable(this.writerOffset, Integer.BYTES, true);
    LAYOUT_INT.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Integer.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setInt(int woff, int value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Integer.BYTES);
    LAYOUT_INT.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer writeUnsignedInt(long value) {
    return this.writeInt((int) (value & 0xFFFFFFFFL));
  }

  @Override
  public @NonNull MemorySegmentBuffer setUnsignedInt(int woff, long value) {
    return this.setInt(woff, (int) (value & 0xFFFFFFFFL));
  }

  @Override
  public float readFloat() {
    this.ensureReadable(this.readerOffset, Float.BYTES);
    var f = (float) LAYOUT_FLOAT.get(this.readSegment, this.readerOffset);
    this.readerOffset += Float.BYTES;
    return f;
  }

  @Override
  public float getFloat(int roff) {
    this.ensureInSegmentBounds(roff, Float.BYTES);
    return (float) LAYOUT_FLOAT.get(this.readSegment, roff);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeFloat(float value) {
    this.ensureWriteable(this.writerOffset, Float.BYTES, true);
    LAYOUT_FLOAT.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Float.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setFloat(int woff, float value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Float.BYTES);
    LAYOUT_FLOAT.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public long readLong() {
    this.ensureReadable(this.readerOffset, Long.BYTES);
    var l = (long) LAYOUT_LONG.get(this.readSegment, this.readerOffset);
    this.readerOffset += Long.BYTES;
    return l;
  }

  @Override
  public long getLong(int roff) {
    this.ensureInSegmentBounds(roff, Long.BYTES);
    return (long) LAYOUT_LONG.get(this.readSegment, roff);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeLong(long value) {
    this.ensureWriteable(this.writerOffset, Long.BYTES, true);
    LAYOUT_LONG.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Long.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setLong(int woff, long value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Long.BYTES);
    LAYOUT_LONG.set(this.writeSegment, woff, value);
    return this;
  }

  @Override
  public double readDouble() {
    this.ensureReadable(this.readerOffset, Double.BYTES);
    var d = (double) LAYOUT_DOUBLE.get(this.readSegment, this.readerOffset);
    this.readerOffset += Double.BYTES;
    return d;
  }

  @Override
  public double getDouble(int roff) {
    this.ensureInSegmentBounds(roff, Double.BYTES);
    return (double) LAYOUT_DOUBLE.get(this.readSegment, roff);
  }

  @Override
  public @NonNull MemorySegmentBuffer writeDouble(double value) {
    this.ensureWriteable(this.writerOffset, Double.BYTES, true);
    LAYOUT_DOUBLE.set(this.writeSegment, this.writerOffset, value);
    this.writerOffset += Double.BYTES;
    return this;
  }

  @Override
  public @NonNull MemorySegmentBuffer setDouble(int woff, double value) {
    this.ensureWriteable(); // ensure not read-only
    this.ensureInSegmentBounds(woff, Double.BYTES);
    LAYOUT_DOUBLE.set(this.writeSegment, woff, value);
    return this;
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="BufferComponent Implementation">

  @Override
  public boolean hasReadableArray() {
    return this.readSegment.heapBase().isPresent();
  }

  @Override
  public boolean hasWritableArray() {
    return this.writeSegment.heapBase().isPresent();
  }

  @Override
  public byte[] readableArray() {
    this.ensureAccessible(); // ensure not closed
    var readHeapBase = this.readSegment.heapBase().orElseThrow(NO_BACKING_ARRAY);
    return (byte[]) readHeapBase;
  }

  @Override
  public byte[] writableArray() {
    this.ensureAccessible(); // ensure not closed
    var readHeapBase = this.writeSegment.heapBase().orElseThrow(NO_BACKING_ARRAY);
    return (byte[]) readHeapBase;
  }

  @Override
  public int readableArrayOffset() {
    if (this.hasReadableArray()) {
      var segmentAddress = Math.toIntExact(this.readSegment.address());
      return segmentAddress + this.readerOffset;
    } else {
      throw NO_BACKING_ARRAY.get();
    }
  }

  @Override
  public int writableArrayOffset() {
    if (this.hasWritableArray()) {
      var segmentAddress = Math.toIntExact(this.writeSegment.address());
      return segmentAddress + this.writerOffset;
    } else {
      throw NO_BACKING_ARRAY.get();
    }
  }

  @Override
  public int readableArrayLength() {
    return this.readableBytes();
  }

  @Override
  public int writableArrayLength() {
    return this.writableBytes();
  }

  @Override
  public long baseNativeAddress() {
    this.ensureAccessible(); // ensure not closed
    return this.isDirect() ? this.readSegment.address() : 0;
  }

  @Override
  public long readableNativeAddress() {
    var baseAddress = this.baseNativeAddress();
    return baseAddress == 0 ? 0 : baseAddress + this.readerOffset;
  }

  @Override
  public long writableNativeAddress() {
    var baseAddress = this.baseNativeAddress();
    return baseAddress == 0 ? 0 : baseAddress + this.writerOffset;
  }

  @Override
  public @NonNull ByteBuffer readableBuffer() {
    this.ensureAccessible(); // ensure not closed
    var readerOffset = this.readerOffset();
    return this.readSegment.asByteBuffer()
      .asReadOnlyBuffer()
      .position(readerOffset)
      .limit(readerOffset + this.readableBytes());
  }

  @Override
  public @NonNull ByteBuffer writableBuffer() {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(); // ensure not read-only

    var writerOffset = this.writerOffset();
    return this.writeSegment.asByteBuffer().position(writerOffset).limit(writerOffset + this.writableBytes());
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="ComponentIterator Implementation">

  @Override
  public @NonNull MemorySegmentBuffer first() {
    return this;
  }

  @Override
  public @Nullable <N extends Next & BufferComponent> N next() {
    return null;
  }
  // </editor-fold>

  // <editor-fold defaultstate="collapsed" desc="ResourceSupport Implementation">

  @Override
  protected @NonNull RuntimeException createResourceClosedException() {
    return InternalBufferUtils.bufferIsClosed(this);
  }

  @Override
  protected @NonNull Owned<MemorySegmentBuffer> prepareSend() {
    var readerOffset = this.readerOffset();
    var writerOffset = this.writerOffset();
    var readOnly = this.readOnly();
    var limit = this.implicitCapacityLimit();

    var control = this.control;
    var baseSegment = this.baseSegment;
    var writeSegment = this.writeSegment;

    return drop -> {
      var buffer = new MemorySegmentBuffer(baseSegment, writeSegment, control, drop);
      buffer.readerOffset = readerOffset;
      buffer.writerOffset = writerOffset;
      buffer.capacityLimit = limit;
      if (readOnly) {
        buffer.makeReadOnly();
      }

      return buffer;
    };
  }

  @Override
  protected void makeInaccessible() {
    this.readerOffset = 0;
    this.writerOffset = 0;
    this.baseSegment = CLOSED_SEGMENT;
    this.readSegment = CLOSED_SEGMENT;
    this.writeSegment = CLOSED_SEGMENT;
  }

  // </editor-fold>

  /**
   * Unsafe method to access the base segment used by this buffer. Should only be used by internal methods.
   *
   * @return the base memory segment used by this buffer.
   */
  @NonNull
  MemorySegment unsafeGetBase() {
    return this.baseSegment;
  }

  /**
   * Allocates a constant child which is similar to a read-only buffer but shares the memory region with this buffer.
   *
   * @return a constant child of this buffer.
   * @throws IllegalStateException if this buffer has already been freed.
   */
  @NonNull
  MemorySegmentBuffer newConstChild() {
    var drop = this.unsafeGetDrop().fork();
    var child = new MemorySegmentBuffer(this, drop);
    drop.attach(child);
    return child;
  }

  /**
   * Ensures that this buffer is still accessible (not closed).
   *
   * @throws BufferClosedException if this buffer is closed.
   */
  private void ensureAccessible() {
    if (!this.isAccessible()) {
      var closedException = InternalBufferUtils.bufferIsClosed(this);
      throw this.attachTrace(closedException);
    }
  }

  /**
   * Ensures that this buffer is owned and can be used freely.
   *
   * @throws IllegalStateException if the buffer is not owned.
   */
  private void ensureOwned() {
    if (!this.isOwned()) {
      var notOwnedException = new IllegalStateException("Buffer is not owned");
      throw this.attachTrace(notOwnedException);
    }
  }

  /**
   * Ensures that this buffer is writeable (not read-only).
   *
   * @throws BufferReadOnlyException if this buffer is read-only.
   */
  private void ensureWriteable() {
    if (this.readOnly()) {
      throw InternalBufferUtils.bufferIsReadOnly(this);
    }
  }

  /**
   * Ensures that the visit of the given {@code size} bytes starting at the given index happens within the bounds of the
   * wrapped memory segment.
   *
   * @param index the index where the visit operation will start.
   * @param size  the count of bytes that will be visited by the operation.
   * @throws BufferClosedException     if this buffer is closed.
   * @throws IndexOutOfBoundsException if the given visit operation is beyond the size of the wrapped buffer.
   */
  private void ensureInSegmentBounds(int index, int size) {
    this.ensureAccessible(); // ensure not closed
    Objects.checkFromIndexSize(index, size, this.capacity());
  }

  /**
   * Ensures that at least {@code size} bytes were written into this buffer starting at the given index.
   *
   * @param index the index to check from.
   * @param size  the count of bytes to check for.
   * @throws BufferClosedException     if this buffer is closed.
   * @throws IndexOutOfBoundsException if not enough bytes were written into the buffer to satisfy the read operation.
   */
  private void ensureReadable(int index, int size) {
    this.ensureAccessible(); // ensure not closed
    Objects.checkFromIndexSize(index, size, this.writerOffset);
  }

  /**
   * Ensures that at least {@code size} bytes can be written into this buffer starting at the given index. An attempt
   * can be made to expand the buffer according to the needs if {@code canExpand} is {@code true}.
   *
   * @param index     the index to check from.
   * @param size      the count of bytes that must be available.
   * @param canExpand if the buffer can be expanded to fit the requested byte count.
   * @throws BufferClosedException     if this buffer is closed.
   * @throws BufferReadOnlyException   if this buffer is read-only.
   * @throws IndexOutOfBoundsException if not enough bytes are available in this buffer for the write operation.
   */
  private void ensureWriteable(int index, int size, boolean canExpand) {
    this.ensureAccessible(); // ensure not closed
    this.ensureWriteable(); // ensure not read-only

    var capacity = this.capacity();
    if (index < this.readerOffset || capacity < index + size) {
      var writerOffset = this.writerOffset;
      var limit = this.implicitCapacityLimit();
      if (canExpand && this.isOwned() && index >= 0 & index <= capacity && writerOffset + size <= limit) {
        // grow to next power of two, but not beyond the implicit limit
        var capacityNextPower2 = PlatformDependent.roundToPowerOfTwo(capacity * 2);
        var growth = Math.max(capacityNextPower2, capacity);
        var minimumGrowth = Math.min(growth, limit) - capacity;
        this.ensureWritable(size, minimumGrowth, false);
        this.ensureInSegmentBounds(index, size); // ensure that writing is now possible
        return;
      }
    }

    var message = String.format("Range [%s, %<s + %s) out of bounds for length %s", index, size, capacity);
    throw new IndexOutOfBoundsException(message);
  }
}
