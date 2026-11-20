/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;

/**
 * Implements all unsafe operations that are using off-heap memory.
 *
 * @since 4.0
 */
final class MemoryOps {

  /**
   * Memory segment that represents the full memory of the current host.
   */
  private static final MemorySegment ALL = MemorySegment.ofAddress(0L).reinterpret(Long.MAX_VALUE);

  // var handles for values with a single byte, alignment is irrelevant for these
  private static final VarHandle BYTE = ValueLayout.JAVA_BYTE.varHandle().withInvokeExactBehavior();
  private static final VarHandle BOOL = ValueLayout.JAVA_BOOLEAN.varHandle().withInvokeExactBehavior();

  // var handles for plain access of multibyte values at any address
  private static final VarHandle SHORT_U = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle().withInvokeExactBehavior();
  private static final VarHandle CHAR_U = ValueLayout.JAVA_CHAR_UNALIGNED.varHandle().withInvokeExactBehavior();
  private static final VarHandle INT_U = ValueLayout.JAVA_INT_UNALIGNED.varHandle().withInvokeExactBehavior();
  private static final VarHandle LONG_U = ValueLayout.JAVA_LONG_UNALIGNED.varHandle().withInvokeExactBehavior();
  private static final VarHandle FLOAT_U = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle().withInvokeExactBehavior();
  private static final VarHandle DOUBLE_U = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle().withInvokeExactBehavior();

  // var handles for access of multibyte values at aligned addresses, supports all access modes
  private static final VarHandle SHORT_A = ValueLayout.JAVA_SHORT.varHandle().withInvokeExactBehavior();
  private static final VarHandle CHAR_A = ValueLayout.JAVA_CHAR.varHandle().withInvokeExactBehavior();
  private static final VarHandle INT_A = ValueLayout.JAVA_INT.varHandle().withInvokeExactBehavior();
  private static final VarHandle LONG_A = ValueLayout.JAVA_LONG.varHandle().withInvokeExactBehavior();
  private static final VarHandle FLOAT_A = ValueLayout.JAVA_FLOAT.varHandle().withInvokeExactBehavior();
  private static final VarHandle DOUBLE_A = ValueLayout.JAVA_DOUBLE.varHandle().withInvokeExactBehavior();

  /**
   * Fixed stripes for 128-byte memory regions. Scalar accesses lock the inclusive range between their first and last
   * stripe. Locks are always acquired in ascending order.
   */
  private static final ReentrantLock[] ACCESS_LOCKS = new ReentrantLock[64];

  static {
    for (var index = 0; index < ACCESS_LOCKS.length; index++) {
      ACCESS_LOCKS[index] = new ReentrantLock();
    }
  }

  private MemoryOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the lock stripe for the 128-byte region containing the given address.
   *
   * @param address the off-heap address to map to a stripe.
   * @return the index of the stripe protecting the region.
   */
  private static int accessStripe(long address) {
    return (int) ((address >>> 7) & (ACCESS_LOCKS.length - 1));
  }

  /**
   * Locks the stripes covering a scalar access. A scalar touches at most two adjacent regions; an access wrapping from
   * the last stripe to the first conservatively locks all stripes. The caller must close the returned guard.
   *
   * @param address   the start address of the access.
   * @param byteCount the width of the scalar in bytes, between one and eight.
   * @return a guard holding the stripes until closed.
   */
  private static MemoryAccess lockAccess(long address, int byteCount) {
    var first = accessStripe(address);
    var last = accessStripe(address + byteCount - 1);
    return new MemoryAccess(Math.min(first, last), Math.max(first, last));
  }

  /**
   * Holds an inclusive range of memory stripes for the creating thread and releases them when closed.
   *
   * @param first the lowest stripe index to lock.
   * @param last  the highest stripe index to lock.
   */
  private record MemoryAccess(int first, int last) implements AutoCloseable {

    /**
     * Acquires the stripes in ascending index order to prevent deadlocks.
     *
     * @param first the lowest stripe index to lock.
     * @param last  the highest stripe index to lock.
     */
    private MemoryAccess {
      for (var index = first; index <= last; index++) {
        ACCESS_LOCKS[index].lock();
      }
    }

    /**
     * Releases the held stripes in reverse acquisition order. Must be called exactly once by the creating thread.
     */
    @Override
    public void close() {
      for (var index = this.last; index >= this.first; index--) {
        ACCESS_LOCKS[index].unlock();
      }
    }
  }

  /**
   * Reads a {@code byte} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static byte getByte(long address, int mode) {
    try (var _ = lockAccess(address, Byte.BYTES)) {
      return mode == OpConstants.MODE_PLAIN ? (byte) BYTE.get(ALL, address) : (byte) BYTE.getVolatile(ALL, address);
    }
  }

  /**
   * Reads a {@code boolean} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static boolean getBoolean(long address, int mode) {
    try (var _ = lockAccess(address, Byte.BYTES)) {
      return mode == OpConstants.MODE_PLAIN ? (boolean) BOOL.get(ALL, address) : (boolean) BOOL.getVolatile(ALL, address);
    }
  }

  /**
   * Reads a {@code short} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static short getShort(long address, int mode) {
    try (var _ = lockAccess(address, Short.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (short) SHORT_U.get(ALL, address);
      }
      if ((address & 1) == 0) {
        return (short) SHORT_A.getVolatile(ALL, address);
      }
      return (short) misalignedGet(ValueTypeKind.SHORT, address);
    }
  }

  /**
   * Reads a {@code char} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static char getChar(long address, int mode) {
    try (var _ = lockAccess(address, Character.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (char) CHAR_U.get(ALL, address);
      }
      if ((address & 1) == 0) {
        return (char) CHAR_A.getVolatile(ALL, address);
      }
      return (char) misalignedGet(ValueTypeKind.CHAR, address);
    }
  }

  /**
   * Reads an {@code int} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static int getInt(long address, int mode) {
    try (var _ = lockAccess(address, Integer.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (int) INT_U.get(ALL, address);
      }
      if ((address & 3) == 0) {
        return (int) INT_A.getVolatile(ALL, address);
      }
      return (int) misalignedGet(ValueTypeKind.INT, address);
    }
  }

  /**
   * Reads a {@code long} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static long getLong(long address, int mode) {
    try (var _ = lockAccess(address, Long.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (long) LONG_U.get(ALL, address);
      }
      if ((address & 7) == 0) {
        return (long) LONG_A.getVolatile(ALL, address);
      }
      return (long) misalignedGet(ValueTypeKind.LONG, address);
    }
  }

  /**
   * Reads a {@code float} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static float getFloat(long address, int mode) {
    try (var _ = lockAccess(address, Float.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (float) FLOAT_U.get(ALL, address);
      }
      if ((address & 3) == 0) {
        return (float) FLOAT_A.getVolatile(ALL, address);
      }
      return (float) misalignedGet(ValueTypeKind.FLOAT, address);
    }
  }

  /**
   * Reads a {@code double} from the given off-heap address.
   *
   * @param address the address to read from.
   * @param mode    the access mode to use, either plain or volatile.
   * @return the value at the given address.
   */
  public static double getDouble(long address, int mode) {
    try (var _ = lockAccess(address, Double.BYTES)) {
      if (mode == OpConstants.MODE_PLAIN) {
        return (double) DOUBLE_U.get(ALL, address);
      }
      if ((address & 7) == 0) {
        return (double) DOUBLE_A.getVolatile(ALL, address);
      }
      return (double) misalignedGet(ValueTypeKind.DOUBLE, address);
    }
  }
  //</editor-fold>

  /**
   * Writes a {@code byte} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putByte(long address, byte value, int mode) {
    try (var _ = lockAccess(address, Byte.BYTES)) {
      switch (mode) {
        case OpConstants.MODE_PLAIN -> BYTE.set(ALL, address, value);
        case OpConstants.MODE_VOLATILE -> BYTE.setVolatile(ALL, address, value);
        default -> BYTE.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code boolean} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putBoolean(long address, boolean value, int mode) {
    try (var _ = lockAccess(address, Byte.BYTES)) {
      switch (mode) {
        case OpConstants.MODE_PLAIN -> BOOL.set(ALL, address, value);
        case OpConstants.MODE_VOLATILE -> BOOL.setVolatile(ALL, address, value);
        default -> BOOL.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code short} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putShort(long address, short value, int mode) {
    try (var _ = lockAccess(address, Short.BYTES)) {
      if ((address & 1) != 0) {
        misalignedPut(ValueTypeKind.SHORT, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        SHORT_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        SHORT_A.setVolatile(ALL, address, value);
      } else {
        SHORT_A.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code char} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putChar(long address, char value, int mode) {
    try (var _ = lockAccess(address, Character.BYTES)) {
      if ((address & 1) != 0) {
        misalignedPut(ValueTypeKind.CHAR, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        CHAR_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        CHAR_A.setVolatile(ALL, address, value);
      } else {
        CHAR_A.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes an {@code int} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putInt(long address, int value, int mode) {
    try (var _ = lockAccess(address, Integer.BYTES)) {
      if ((address & 3) != 0) {
        misalignedPut(ValueTypeKind.INT, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        INT_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        INT_A.setVolatile(ALL, address, value);
      } else {
        INT_A.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code long} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putLong(long address, long value, int mode) {
    try (var _ = lockAccess(address, Long.BYTES)) {
      if ((address & 7) != 0) {
        misalignedPut(ValueTypeKind.LONG, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        LONG_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        LONG_A.setVolatile(ALL, address, value);
      } else {
        LONG_A.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code float} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putFloat(long address, float value, int mode) {
    try (var _ = lockAccess(address, Float.BYTES)) {
      if ((address & 3) != 0) {
        misalignedPut(ValueTypeKind.FLOAT, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        FLOAT_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        FLOAT_A.setVolatile(ALL, address, value);
      } else {
        FLOAT_A.setRelease(ALL, address, value);
      }
    }
  }

  /**
   * Writes a {@code double} to the given off-heap address.
   *
   * @param address the address to write to.
   * @param value   the value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  public static void putDouble(long address, double value, int mode) {
    try (var _ = lockAccess(address, Double.BYTES)) {
      if ((address & 7) != 0) {
        misalignedPut(ValueTypeKind.DOUBLE, address, value, mode);
      } else if (mode == OpConstants.MODE_PLAIN) {
        DOUBLE_U.set(ALL, address, value);
      } else if (mode == OpConstants.MODE_VOLATILE) {
        DOUBLE_A.setVolatile(ALL, address, value);
      } else {
        DOUBLE_A.setRelease(ALL, address, value);
      }
    }
  }
  //</editor-fold>

  /**
   * Atomically sets the {@code int} at the given off-heap address to the given value if it currently holds the expected
   * value.
   *
   * @param address  the address of the value.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  public static boolean casInt(long address, int expected, int value) {
    try (var _ = lockAccess(address, Integer.BYTES)) {
      if ((address & 3) == 0) {
        return INT_A.compareAndSet(ALL, address, expected, value);
      }
      return misalignedCas(ValueTypeKind.INT, address, expected, value);
    }
  }

  /**
   * Atomically sets the {@code long} at the given off-heap address to the given value if it currently holds the
   * expected value.
   *
   * @param address  the address of the value.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  public static boolean casLong(long address, long expected, long value) {
    try (var _ = lockAccess(address, Long.BYTES)) {
      if ((address & 7) == 0) {
        return LONG_A.compareAndSet(ALL, address, expected, value);
      }
      return misalignedCas(ValueTypeKind.LONG, address, expected, value);
    }
  }

  /**
   * Atomically adds the given delta to the {@code int} at the given off-heap address.
   *
   * @param address the address of the value.
   * @param delta   the delta to add to the current value.
   * @return the value before adding the delta.
   */
  public static int getAndAddInt(long address, int delta) {
    try (var _ = lockAccess(address, Integer.BYTES)) {
      if ((address & 3) == 0) {
        return (int) INT_A.getAndAdd(ALL, address, delta);
      }
      return (int) misalignedGetAndAdd(ValueTypeKind.INT, address, delta);
    }
  }

  /**
   * Atomically adds the given delta to the {@code long} at the given off-heap address.
   *
   * @param address the address of the value.
   * @param delta   the delta to add to the current value.
   * @return the value before adding the delta.
   */
  public static long getAndAddLong(long address, long delta) {
    try (var _ = lockAccess(address, Long.BYTES)) {
      if ((address & 7) == 0) {
        return (long) LONG_A.getAndAdd(ALL, address, delta);
      }
      return (long) misalignedGetAndAdd(ValueTypeKind.LONG, address, delta);
    }
  }

  /**
   * Atomically sets the {@code int} at the given off-heap address to the given value.
   *
   * @param address the address of the value.
   * @param value   the new value to set.
   * @return the value before setting the new value.
   */
  public static int getAndSetInt(long address, int value) {
    try (var _ = lockAccess(address, Integer.BYTES)) {
      if ((address & 3) == 0) {
        return (int) INT_A.getAndSet(ALL, address, value);
      }
      return (int) misalignedGetAndSet(ValueTypeKind.INT, address, value);
    }
  }

  /**
   * Atomically sets the {@code long} at the given off-heap address to the given value.
   *
   * @param address the address of the value.
   * @param value   the new value to set.
   * @return the value before setting the new value.
   */
  public static long getAndSetLong(long address, long value) {
    try (var _ = lockAccess(address, Long.BYTES)) {
      if ((address & 7) == 0) {
        return (long) LONG_A.getAndSet(ALL, address, value);
      }
      return (long) misalignedGetAndSet(ValueTypeKind.LONG, address, value);
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Misaligned Access Emulation">

  /**
   * Emulates a volatile read at a misaligned address. The caller holds the memory access stripes to avoid observing
   * partial stores.
   *
   * @param kind    the kind of value to read.
   * @param address the misaligned address to read from.
   * @return the boxed value at the given address.
   */
  private static @NonNull Object misalignedGet(@NonNull ValueTypeKind kind, long address) {
    return kind.unalignedLayoutVarHandle().get(ALL, address);
  }

  /**
   * Emulates a write at a misaligned address. The caller holds the memory access stripes
   * to prevent overlapping read-modify-write operations from overwriting the write.
   *
   * @param kind    the kind of value to write.
   * @param address the misaligned address to write to.
   * @param value   the boxed value to write.
   * @param mode    the access mode to use, either plain, volatile or release.
   */
  private static void misalignedPut(@NonNull ValueTypeKind kind, long address, @NonNull Object value, int mode) {
    kind.unalignedLayoutVarHandle().set(ALL, address, value);
    if (mode != OpConstants.MODE_PLAIN) {
      VarHandle.fullFence();
    }
  }

  /**
   * Emulates a compare-and-set at a misaligned address, called while holding the memory access stripes.
   *
   * @param kind     the kind of value.
   * @param address  the misaligned address of the value.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  private static boolean misalignedCas(@NonNull ValueTypeKind kind, long address, Object expected, Object value) {
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(ALL, address);
    if (!kind.areValuesEqual(current, expected)) {
      return false;
    }
    handle.set(ALL, address, value);

    VarHandle.fullFence();
    return true;
  }

  /**
   * Emulates a get-and-add at a misaligned address, called while holding the memory access stripes.
   *
   * @param kind    the kind of value, either int or long.
   * @param address the misaligned address of the value.
   * @param delta   the delta to add to the current value.
   * @return the value before adding the delta.
   */
  private static @NonNull Object misalignedGetAndAdd(@NonNull ValueTypeKind kind, long address, Object delta) {
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(ALL, address);
    handle.set(ALL, address, kind.add(current, delta));

    VarHandle.fullFence();
    return current;
  }

  /**
   * Emulates a get-and-set at a misaligned address, called while holding the memory access stripes.
   *
   * @param kind    the kind of value.
   * @param address the misaligned address of the value.
   * @param value   the new value to set.
   * @return the value before setting the new value.
   */
  private static @NonNull Object misalignedGetAndSet(@NonNull ValueTypeKind kind, long address, Object value) {
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(ALL, address);
    handle.set(ALL, address, value);

    VarHandle.fullFence();
    return current;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Copy/Fill Operations">

  /**
   * Copies the given off-heap memory range from the given source offset to the given destination offset.
   *
   * @param srcOffset the source offset to copy from.
   * @param dstOffset the destination offset to copy to.
   * @param byteCount the number of bytes to copy.
   */
  public static void memCopy(long srcOffset, long dstOffset, long byteCount) {
    if (byteCount > 0) {
      MemorySegment.copy(ALL, srcOffset, ALL, dstOffset, byteCount);
    }
  }

  /**
   * Copies the off-heap memory at the given source offset into the given destination on-heap array.
   *
   * @param kind      the kind of array to copy the off-heap memory into.
   * @param dstArray  the destination on-heap array to copy the off-heap memory into.
   * @param srcOffset the source offset to copy from.
   * @param dstOffset the destination offset to copy to.
   * @param byteCount the number of bytes to copy.
   * @throws NullPointerException if the given kind or array is null.
   */
  public static void memToHeapCopy(
    @NonNull ValueTypeKind kind,
    @NonNull Object dstArray,
    long srcOffset,
    long dstOffset,
    long byteCount
  ) {
    synchronized (ArrayOps.accessLock(dstArray)) {
      if (byteCount > 0 && kind != ValueTypeKind.REF) {
        var dstSegment = kind.createArrayMemorySegment(dstArray);
        MemorySegment.copy(ALL, srcOffset, dstSegment, dstOffset, byteCount);
        ArrayOps.copyByteSegmentToBoolArray(dstSegment, dstArray, dstOffset, byteCount);
      }
    }
  }

  /**
   * Copies the given on-heap array into the off-heap memory at the given destination offset.
   *
   * @param kind      the kind of array to copy the on-heap array from.
   * @param srcArray  the source on-heap array to copy the off-heap memory from.
   * @param srcOffset the source offset to copy from.
   * @param dstOffset the destination offset to copy to.
   * @param byteCount the number of bytes to copy.
   * @throws NullPointerException if the given kind or array is null.
   */
  public static void memFromHeapCopy(
    @NonNull ValueTypeKind kind,
    @NonNull Object srcArray,
    long srcOffset,
    long dstOffset,
    long byteCount
  ) {
    synchronized (ArrayOps.accessLock(srcArray)) {
      if (byteCount > 0 && kind != ValueTypeKind.REF) {
        var srcSegment = kind.createArrayMemorySegment(srcArray);
        MemorySegment.copy(srcSegment, srcOffset, ALL, dstOffset, byteCount);
      }
    }
  }

  /**
   * Fills the given off-heap memory region with the given value.
   *
   * @param offset    the offset into the off-heap memory to fill.
   * @param byteCount the number of bytes to fill.
   * @param value     the value to fill the region with.
   */
  public static void memFill(long offset, long byteCount, byte value) {
    if (byteCount > 0) {
      var slice = ALL.asSlice(offset, byteCount);
      slice.fill(value);
    }
  }
  //</editor-fold>
}
