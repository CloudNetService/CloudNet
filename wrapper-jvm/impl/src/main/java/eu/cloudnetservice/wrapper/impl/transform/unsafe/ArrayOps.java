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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;

/**
 * Implements all unsafe operations that are using on-heap arrays.
 *
 * @since 4.0
 */
final class ArrayOps {

  // var handles to access the elements of arrays
  private static final VarHandle BYTES = elementHandle(byte[].class);
  private static final VarHandle BOOLS = elementHandle(boolean[].class);
  private static final VarHandle SHORTS = elementHandle(short[].class);
  private static final VarHandle CHARS = elementHandle(char[].class);
  private static final VarHandle INTS = elementHandle(int[].class);
  private static final VarHandle LONGS = elementHandle(long[].class);
  private static final VarHandle FLOATS = elementHandle(float[].class);
  private static final VarHandle DOUBLES = elementHandle(double[].class);
  private static final VarHandle REFS = elementHandle(Object[].class);

  // var handles for plain access to multibyte values in byte arrays at any index. other access modes are not supported
  // by byte array views (the alignment of the array data is not guaranteed), these accesses use the slow path
  private static final VarHandle BYTES_AS_SHORT = byteViewHandle(short[].class);
  private static final VarHandle BYTES_AS_CHAR = byteViewHandle(char[].class);
  private static final VarHandle BYTES_AS_INT = byteViewHandle(int[].class);
  private static final VarHandle BYTES_AS_LONG = byteViewHandle(long[].class);
  private static final VarHandle BYTES_AS_FLOAT = byteViewHandle(float[].class);
  private static final VarHandle BYTES_AS_DOUBLE = byteViewHandle(double[].class);

  // Primitive array accesses share their array's lock because overlapping accesses of another width may require
  // emulation. Reference accesses use var handles directly. Bulk copies acquire stripes in ascending order.
  private static final Object[] ACCESS_LOCKS = new Object[64];

  static {
    for (var index = 0; index < ACCESS_LOCKS.length; index++) {
      ACCESS_LOCKS[index] = new Object();
    }
  }

  private ArrayOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the lock shared by primitive accesses to the given array, including copies between heap and off-heap memory.
   *
   * @param array the array to get the lock for.
   * @return the lock shared by accesses to the given array.
   * @throws NullPointerException if the given array is null.
   */
  static @NonNull Object accessLock(@NonNull Object array) {
    return ACCESS_LOCKS[System.identityHashCode(array) & (ACCESS_LOCKS.length - 1)];
  }

  /**
   * Creates a var handle with exact invocation behavior to access the elements of the given array type.
   *
   * @param arrayType the array type to create the var handle for.
   * @return a var handle to access the elements of the given array type.
   * @throws NullPointerException if the given array type is null.
   */
  private static @NonNull VarHandle elementHandle(@NonNull Class<?> arrayType) {
    return MethodHandles.arrayElementVarHandle(arrayType).withInvokeExactBehavior();
  }

  /**
   * Creates a var handle with exact invocation behavior to access the elements of the given array type in a byte array,
   * using the native byte order.
   *
   * @param viewArrayType the array type whose elements should be accessed in a byte array.
   * @return a var handle to access the elements of the given array type in a byte array.
   * @throws NullPointerException if the given view array type is null.
   */
  private static @NonNull VarHandle byteViewHandle(@NonNull Class<?> viewArrayType) {
    return MethodHandles.byteArrayViewVarHandle(viewArrayType, ByteOrder.nativeOrder()).withInvokeExactBehavior();
  }

  /**
   * Converts the given byte offset into an array index for an array with the given index scale shift.
   *
   * @param offset the byte offset to convert.
   * @param shift  the shift of the index scale of the array.
   * @return the array index for the given offset.
   * @throws IndexOutOfBoundsException if the resulting index does not fit into an int.
   */
  private static int index(long offset, int shift) {
    var index = offset >> shift;
    if (index != (int) index) {
      throw indexOutOfBounds(offset);
    }
    return (int) index;
  }

  /**
   * Constructs the exception to throw when an offset is out of the bounds of every possible array.
   *
   * @param offset the offset that was passed by the caller.
   * @return the exception to throw for the given offset.
   */
  private static @NonNull IndexOutOfBoundsException indexOutOfBounds(long offset) {
    return new IndexOutOfBoundsException("Array offset " + offset + " is out of bounds");
  }

  /**
   * Converts the given byte offset into an index into a reference array.
   *
   * @param offset the byte offset to convert.
   * @return the array index for the given offset.
   * @throws IllegalArgumentException  if the given offset is misaligned.
   * @throws IndexOutOfBoundsException if the resulting index does not fit into an int.
   */
  private static int refIndex(long offset) {
    if ((offset & 3) != 0) {
      throw new IllegalArgumentException("misaligned access to reference array at offset " + offset);
    }
    return index(offset, 2);
  }

  /**
   * Reads a {@code byte} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static byte getByte(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof byte[] a) {
        var index = index(offset, 0);
        return mode == OpConstants.MODE_PLAIN ? (byte) BYTES.get(a, index) : (byte) BYTES.getVolatile(a, index);
      }
      return (byte) slowGet(array, offset, ValueTypeKind.BYTE, mode);
    }
  }

  /**
   * Reads a {@code boolean} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static boolean getBoolean(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof boolean[] a) {
        var index = index(offset, 0);
        return mode == OpConstants.MODE_PLAIN ? (boolean) BOOLS.get(a, index) : (boolean) BOOLS.getVolatile(a, index);
      }
      return (boolean) slowGet(array, offset, ValueTypeKind.BOOL, mode);
    }
  }

  /**
   * Reads a {@code short} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static short getShort(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof short[] a && (offset & 1) == 0) {
        var index = index(offset, 1);
        return mode == OpConstants.MODE_PLAIN ? (short) SHORTS.get(a, index) : (short) SHORTS.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (short) BYTES_AS_SHORT.get(a, index(offset, 0));
      }
      return (short) slowGet(array, offset, ValueTypeKind.SHORT, mode);
    }
  }

  /**
   * Reads a {@code char} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static char getChar(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof char[] a && (offset & 1) == 0) {
        var index = index(offset, 1);
        return mode == OpConstants.MODE_PLAIN ? (char) CHARS.get(a, index) : (char) CHARS.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (char) BYTES_AS_CHAR.get(a, index(offset, 0));
      }
      return (char) slowGet(array, offset, ValueTypeKind.CHAR, mode);
    }
  }

  /**
   * Reads an {@code int} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static int getInt(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof int[] a && (offset & 3) == 0) {
        var index = index(offset, 2);
        return mode == OpConstants.MODE_PLAIN ? (int) INTS.get(a, index) : (int) INTS.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (int) BYTES_AS_INT.get(a, index(offset, 0));
      }
      return (int) slowGet(array, offset, ValueTypeKind.INT, mode);
    }
  }

  /**
   * Reads a {@code long} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static long getLong(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof long[] a && (offset & 7) == 0) {
        var index = index(offset, 3);
        return mode == OpConstants.MODE_PLAIN ? (long) LONGS.get(a, index) : (long) LONGS.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (long) BYTES_AS_LONG.get(a, index(offset, 0));
      }
      return (long) slowGet(array, offset, ValueTypeKind.LONG, mode);
    }
  }

  /**
   * Reads a {@code float} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static float getFloat(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof float[] a && (offset & 3) == 0) {
        var index = index(offset, 2);
        return mode == OpConstants.MODE_PLAIN ? (float) FLOATS.get(a, index) : (float) FLOATS.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (float) BYTES_AS_FLOAT.get(a, index(offset, 0));
      }
      return (float) slowGet(array, offset, ValueTypeKind.FLOAT, mode);
    }
  }

  /**
   * Reads a {@code double} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static double getDouble(Object array, long offset, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof double[] a && (offset & 7) == 0) {
        var index = index(offset, 3);
        return mode == OpConstants.MODE_PLAIN ? (double) DOUBLES.get(a, index) : (double) DOUBLES.getVolatile(a, index);
      }
      if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        return (double) BYTES_AS_DOUBLE.get(a, index(offset, 0));
      }
      return (double) slowGet(array, offset, ValueTypeKind.DOUBLE, mode);
    }
  }

  /**
   * Reads an {@code Object} from the given array at the given byte offset.
   *
   * @param array  the array to read from.
   * @param offset the byte offset into the array.
   * @param mode   the access mode to use, either plain or volatile.
   * @return the value at the given offset in the given array.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static Object getObject(Object array, long offset, int mode) {
    if (array instanceof Object[] a && (offset & 3) == 0) {
      var index = index(offset, 2);
      return mode == OpConstants.MODE_PLAIN ? (Object) REFS.get(a, index) : (Object) REFS.getVolatile(a, index);
    }
    return slowGet(array, offset, ValueTypeKind.REF, mode);
  }
  //</editor-fold>

  /**
   * Writes a {@code byte} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putByte(Object array, long offset, byte value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof byte[] a) {
        var index = index(offset, 0);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> BYTES.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> BYTES.setVolatile(a, index, value);
          default -> BYTES.setRelease(a, index, value);
        }
      } else {
        slowPut(array, offset, ValueTypeKind.BYTE, value, mode);
      }
    }
  }

  /**
   * Writes a {@code boolean} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putBoolean(Object array, long offset, boolean value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof boolean[] a) {
        var index = index(offset, 0);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> BOOLS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> BOOLS.setVolatile(a, index, value);
          default -> BOOLS.setRelease(a, index, value);
        }
      } else {
        slowPut(array, offset, ValueTypeKind.BOOL, value, mode);
      }
    }
  }

  /**
   * Writes a {@code short} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putShort(Object array, long offset, short value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof short[] a && (offset & 1) == 0) {
        var index = index(offset, 1);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> SHORTS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> SHORTS.setVolatile(a, index, value);
          default -> SHORTS.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_SHORT.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.SHORT, value, mode);
      }
    }
  }

  /**
   * Writes a {@code char} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putChar(Object array, long offset, char value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof char[] a && (offset & 1) == 0) {
        var index = index(offset, 1);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> CHARS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> CHARS.setVolatile(a, index, value);
          default -> CHARS.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_CHAR.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.CHAR, value, mode);
      }
    }
  }

  /**
   * Writes an {@code int} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putInt(Object array, long offset, int value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof int[] a && (offset & 3) == 0) {
        var index = index(offset, 2);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> INTS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> INTS.setVolatile(a, index, value);
          default -> INTS.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_INT.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.INT, value, mode);
      }
    }
  }

  /**
   * Writes a {@code long} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putLong(Object array, long offset, long value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof long[] a && (offset & 7) == 0) {
        var index = index(offset, 3);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> LONGS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> LONGS.setVolatile(a, index, value);
          default -> LONGS.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_LONG.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.LONG, value, mode);
      }
    }
  }

  /**
   * Writes a {@code float} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putFloat(Object array, long offset, float value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof float[] a && (offset & 3) == 0) {
        var index = index(offset, 2);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> FLOATS.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> FLOATS.setVolatile(a, index, value);
          default -> FLOATS.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_FLOAT.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.FLOAT, value, mode);
      }
    }
  }

  /**
   * Writes a {@code double} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putDouble(Object array, long offset, double value, int mode) {
    synchronized (accessLock(array)) {
      if (array instanceof double[] a && (offset & 7) == 0) {
        var index = index(offset, 3);
        switch (mode) {
          case OpConstants.MODE_PLAIN -> DOUBLES.set(a, index, value);
          case OpConstants.MODE_VOLATILE -> DOUBLES.setVolatile(a, index, value);
          default -> DOUBLES.setRelease(a, index, value);
        }
      } else if (array instanceof byte[] a && mode == OpConstants.MODE_PLAIN) {
        BYTES_AS_DOUBLE.set(a, index(offset, 0), value);
      } else {
        slowPut(array, offset, ValueTypeKind.DOUBLE, value, mode);
      }
    }
  }

  /**
   * Writes an {@code Object} into the given array at the given byte offset.
   *
   * @param array  the array to write to.
   * @param offset the byte offset into the array.
   * @param value  the value to write.
   * @param mode   the access mode to use, either plain, volatile or release.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static void putObject(Object array, long offset, Object value, int mode) {
    if (array instanceof Object[] a && (offset & 3) == 0) {
      var index = index(offset, 2);
      switch (mode) {
        case OpConstants.MODE_PLAIN -> REFS.set(a, index, value);
        case OpConstants.MODE_VOLATILE -> REFS.setVolatile(a, index, value);
        default -> REFS.setRelease(a, index, value);
      }
    } else {
      slowPut(array, offset, ValueTypeKind.REF, value, mode);
    }
  }
  //</editor-fold>

  /**
   * Atomically sets the {@code int} at the given byte offset in the given array to the given value if it currently
   * holds the expected value.
   *
   * @param array    the array that holds the value.
   * @param offset   the byte offset into the array.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static boolean casInt(Object array, long offset, int expected, int value) {
    synchronized (accessLock(array)) {
      if (array instanceof int[] a && (offset & 3) == 0) {
        return INTS.compareAndSet(a, index(offset, 2), expected, value);
      }
      return slowCas(array, offset, ValueTypeKind.INT, expected, value);
    }
  }

  /**
   * Atomically sets the {@code long} at the given byte offset in the given array to the given value if it currently
   * holds the expected value.
   *
   * @param array    the array that holds the value.
   * @param offset   the byte offset into the array.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static boolean casLong(Object array, long offset, long expected, long value) {
    synchronized (accessLock(array)) {
      if (array instanceof long[] a && (offset & 7) == 0) {
        return LONGS.compareAndSet(a, index(offset, 3), expected, value);
      }
      return slowCas(array, offset, ValueTypeKind.LONG, expected, value);
    }
  }

  /**
   * Atomically sets the {@code Object} at the given byte offset in the given array to the given value if it currently
   * holds the expected value.
   *
   * @param array    the array that holds the value.
   * @param offset   the byte offset into the array.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static boolean casObject(Object array, long offset, Object expected, Object value) {
    if (array instanceof Object[] a && (offset & 3) == 0) {
      return REFS.compareAndSet(a, index(offset, 2), expected, value);
    }
    return slowCas(array, offset, ValueTypeKind.REF, expected, value);
  }

  /**
   * Atomically adds the given delta to the {@code int} at the given byte offset in the given array.
   *
   * @param array  the array that holds the value.
   * @param offset the byte offset into the array.
   * @param delta  the delta to add to the current value.
   * @return the value before adding the delta.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static int getAndAddInt(Object array, long offset, int delta) {
    synchronized (accessLock(array)) {
      if (array instanceof int[] a && (offset & 3) == 0) {
        return (int) INTS.getAndAdd(a, index(offset, 2), delta);
      }
      return (int) slowGetAndAdd(array, offset, ValueTypeKind.INT, delta);
    }
  }

  /**
   * Atomically adds the given delta to the {@code long} at the given byte offset in the given array.
   *
   * @param array  the array that holds the value.
   * @param offset the byte offset into the array.
   * @param delta  the delta to add to the current value.
   * @return the value before adding the delta.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static long getAndAddLong(Object array, long offset, long delta) {
    synchronized (accessLock(array)) {
      if (array instanceof long[] a && (offset & 7) == 0) {
        return (long) LONGS.getAndAdd(a, index(offset, 3), delta);
      }
      return (long) slowGetAndAdd(array, offset, ValueTypeKind.LONG, delta);
    }
  }

  /**
   * Atomically sets the {@code int} at the given byte offset in the given array to the given value.
   *
   * @param array  the array that holds the value.
   * @param offset the byte offset into the array.
   * @param value  the new value to set.
   * @return the value before setting the new value.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static int getAndSetInt(Object array, long offset, int value) {
    synchronized (accessLock(array)) {
      if (array instanceof int[] a && (offset & 3) == 0) {
        return (int) INTS.getAndSet(a, index(offset, 2), value);
      }
      return (int) slowGetAndSet(array, offset, ValueTypeKind.INT, value);
    }
  }

  /**
   * Atomically sets the {@code long} at the given byte offset in the given array to the given value.
   *
   * @param array  the array that holds the value.
   * @param offset the byte offset into the array.
   * @param value  the new value to set.
   * @return the value before setting the new value.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static long getAndSetLong(Object array, long offset, long value) {
    synchronized (accessLock(array)) {
      if (array instanceof long[] a && (offset & 7) == 0) {
        return (long) LONGS.getAndSet(a, index(offset, 3), value);
      }
      return (long) slowGetAndSet(array, offset, ValueTypeKind.LONG, value);
    }
  }

  /**
   * Atomically sets the {@code Object} at the given byte offset in the given array to the given value.
   *
   * @param array  the array that holds the value.
   * @param offset the byte offset into the array.
   * @param value  the new value to set.
   * @return the value before setting the new value.
   * @throws IndexOutOfBoundsException if the given offset is out of bounds of the given array.
   */
  public static Object getAndSetObject(Object array, long offset, Object value) {
    if (array instanceof Object[] a && (offset & 3) == 0) {
      return (Object) REFS.getAndSet(a, index(offset, 2), value);
    }
    return slowGetAndSet(array, offset, ValueTypeKind.REF, value);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Slow Path">

  /**
   * Checks if aligned access to an element of the given value kind in an array of the given kind is possible, that is
   * if an element can be read from the array without requiring two operations to do so.
   *
   * @param arrayKind the kind of array.
   * @param valueKind the kind of value to read/put into the target array.
   * @param offset    the offset into the array to access.
   * @return true if aligned access is possible, false otherwise.
   */
  private static boolean isAlignedAccess(ValueTypeKind arrayKind, ValueTypeKind valueKind, long offset) {
    var valueScale = valueKind.byteSize();
    return arrayKind.byteSize() >= valueScale && offset % valueScale == 0;
  }

  /**
   * Validates that the given value kind can be read from or written to an array of the given kind using the slow path.
   *
   * @param arrayKind the kind of array.
   * @param valueKind the kind of value to read/put into the target array.
   * @param offset    the offset into the array to access.
   * @throws UnsupportedOperationException if a reference is accessed in a primitive array or vice versa.
   * @throws IllegalArgumentException      if a reference array is accessed at a misaligned offset.
   */
  private static void validateSlowAccess(ValueTypeKind arrayKind, ValueTypeKind valueKind, long offset) {
    if (valueKind == ValueTypeKind.REF && arrayKind == ValueTypeKind.REF) {
      refIndex(offset);
    }
    if (valueKind == ValueTypeKind.REF || arrayKind == ValueTypeKind.REF) {
      throw new UnsupportedOperationException(
        "Unsupported access to value of kind " + valueKind + " in array of kind " + arrayKind);
    }
  }

  /**
   * Reads a value of the given kind from the given array in the case that no fast path is available.
   *
   * @param array  the array to read the value from.
   * @param offset the offset into the array to read the value from.
   * @param kind   the kind of value to read.
   * @param mode   the access mode to use for reading.
   * @return the boxed value read from the array.
   */
  private static Object slowGet(Object array, long offset, ValueTypeKind kind, int mode) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (arrayKind == ValueTypeKind.REF && kind != ValueTypeKind.REF) {
      // primitive read from an object array, convert the (possibly boxed) element
      var refArray = (Object[]) array;
      var index = refIndex(offset);
      var element = mode == OpConstants.MODE_PLAIN
        ? (Object) REFS.get(refArray, index)
        : (Object) REFS.getVolatile(refArray, index);
      return kind.fromBits(ValueTypeKind.toBits(element));
    }

    validateSlowAccess(arrayKind, kind, offset);
    if (mode == OpConstants.MODE_PLAIN) {
      var segment = arrayKind.createArrayMemorySegment(array);
      return kind.unalignedLayoutVarHandle().get(segment, offset);
    }

    if (arrayKind != ValueTypeKind.BOOL && isAlignedAccess(arrayKind, kind, offset)) {
      var segment = arrayKind.createArrayMemorySegment(array);
      return kind.layoutVarHandle().getVolatile(segment, offset);
    }

    // The public accessors hold the array lock, including while creating boolean array snapshots,
    // so emulated reads cannot observe partial stores.
    var segment = arrayKind.createArrayMemorySegment(array);
    return kind.unalignedLayoutVarHandle().get(segment, offset);
  }

  /**
   * Writes a value of the given kind into the given array in the case that no fast path is available.
   *
   * @param array  the array to write the value to.
   * @param offset the offset into the array to write the value to.
   * @param kind   the kind of value to write.
   * @param value  the boxed value to write.
   * @param mode   the access mode to use for writing.
   */
  private static void slowPut(Object array, long offset, ValueTypeKind kind, Object value, int mode) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (arrayKind == ValueTypeKind.REF && kind != ValueTypeKind.REF) {
      // primitive write into an object array, store the boxed value
      var refArray = (Object[]) array;
      var index = refIndex(offset);
      switch (mode) {
        case OpConstants.MODE_PLAIN -> REFS.set(refArray, index, value);
        case OpConstants.MODE_VOLATILE -> REFS.setVolatile(refArray, index, value);
        default -> REFS.setRelease(refArray, index, value);
      }
      return;
    }

    validateSlowAccess(arrayKind, kind, offset);
    if (arrayKind != ValueTypeKind.BOOL && isAlignedAccess(arrayKind, kind, offset)) {
      var segment = arrayKind.createArrayMemorySegment(array);
      switch (mode) {
        case OpConstants.MODE_PLAIN -> kind.layoutVarHandle().set(segment, offset, value);
        case OpConstants.MODE_VOLATILE -> kind.layoutVarHandle().setVolatile(segment, offset, value);
        default -> kind.layoutVarHandle().setRelease(segment, offset, value);
      }
      return;
    }

    var segment = arrayKind.createArrayMemorySegment(array);
    kind.unalignedLayoutVarHandle().set(segment, offset, value);
    copyByteSegmentToBoolArray(segment, array, offset, kind.byteSize());

    if (mode != OpConstants.MODE_PLAIN) {
      VarHandle.fullFence();
    }
  }

  /**
   * Compares and sets a value of the given kind in the given array in the case that no fast path is available. Uses an
   * atomic operation if possible, emulates the operation while holding the lock for the array otherwise.
   *
   * @param array    the array to compare and set the value in.
   * @param offset   the offset into the array of the value.
   * @param kind     the kind of value.
   * @param expected the expected current value.
   * @param value    the new value to set.
   * @return true if the value was set, false otherwise.
   */
  private static boolean slowCas(Object array, long offset, ValueTypeKind kind, Object expected, Object value) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    validateSlowAccess(arrayKind, kind, offset);
    if (arrayKind != ValueTypeKind.BOOL && isAlignedAccess(arrayKind, kind, offset)) {
      var segment = arrayKind.createArrayMemorySegment(array);
      return kind.layoutVarHandle().compareAndSet(segment, offset, expected, value);
    }

    var segment = arrayKind.createArrayMemorySegment(array);
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(segment, offset);
    if (!kind.areValuesEqual(current, expected)) {
      return false;
    }

    handle.set(segment, offset, value);
    copyByteSegmentToBoolArray(segment, array, offset, kind.byteSize());

    VarHandle.fullFence();
    return true;
  }

  /**
   * Adds the given delta to a value of the given kind in the given array in the case that no fast path is available.
   * Uses an atomic operation if possible, emulates the operation while holding the lock for the array otherwise.
   *
   * @param array  the array to add the value in.
   * @param offset the offset into the array of the value.
   * @param kind   the kind of value, either int or long.
   * @param delta  the delta to add to the current value.
   * @return the value before adding the delta.
   */
  private static Object slowGetAndAdd(Object array, long offset, ValueTypeKind kind, Object delta) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    validateSlowAccess(arrayKind, kind, offset);
    if (arrayKind != ValueTypeKind.BOOL && isAlignedAccess(arrayKind, kind, offset)) {
      var segment = arrayKind.createArrayMemorySegment(array);
      return kind.layoutVarHandle().getAndAdd(segment, offset, delta);
    }

    var segment = arrayKind.createArrayMemorySegment(array);
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(segment, offset);
    handle.set(segment, offset, kind.add(current, delta));
    copyByteSegmentToBoolArray(segment, array, offset, kind.byteSize());

    VarHandle.fullFence();
    return current;
  }

  /**
   * Sets a value of the given kind in the given array in the case that no fast path is available. Uses an atomic
   * operation if possible, emulates the operation while holding the lock for the array otherwise.
   *
   * @param array  the array to set the value in.
   * @param offset the offset into the array of the value.
   * @param kind   the kind of value.
   * @param value  the value to set.
   * @return the value before setting the new value.
   */
  private static Object slowGetAndSet(Object array, long offset, ValueTypeKind kind, Object value) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    validateSlowAccess(arrayKind, kind, offset);
    if (arrayKind != ValueTypeKind.BOOL && isAlignedAccess(arrayKind, kind, offset)) {
      var segment = arrayKind.createArrayMemorySegment(array);
      return kind.layoutVarHandle().getAndSet(segment, offset, value);
    }

    var segment = arrayKind.createArrayMemorySegment(array);
    var handle = kind.unalignedLayoutVarHandle();
    var current = handle.get(segment, offset);
    handle.set(segment, offset, value);
    copyByteSegmentToBoolArray(segment, array, offset, kind.byteSize());

    VarHandle.fullFence();
    return current;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Copy/Fill Operations">

  /**
   * Copies the given byte memory segment into the given boolean array, if the given array is a boolean array. Only the
   * given range of bytes is copied.
   *
   * @param segment the segment to copy the data from.
   * @param array   the array to copy the data to.
   * @param off     the offset into the array to start copying from.
   * @param count   the number of bytes to copy.
   * @throws NullPointerException if the given segment or array is null.
   */
  static void copyByteSegmentToBoolArray(
    @NonNull MemorySegment segment,
    @NonNull Object array,
    long off,
    long count
  ) {
    if (array instanceof boolean[] dst) {
      var src = (byte[]) segment.heapBase().orElseThrow();
      for (var index = 0L; index < count; index++) {
        var idx = Math.toIntExact(off + index);
        dst[idx] = src[idx] != 0;
      }
    }
  }

  /**
   * Copies the given src array into the given destination array.
   *
   * @param srcArrayKind the kind of the source array.
   * @param srcArray     the source array.
   * @param srcOffset    the offset into the source array to copy from.
   * @param dstArrayKind the kind of the destination array.
   * @param dstArray     the destination array.
   * @param dstOffset    the offset into the destination array to copy to.
   * @param byteCount    the number of bytes to copy.
   * @throws NullPointerException if the given source kind or array or destination kind or array is null.
   */
  public static void arrayCopy(
    @NonNull ValueTypeKind srcArrayKind,
    @NonNull Object srcArray,
    long srcOffset,
    @NonNull ValueTypeKind dstArrayKind,
    @NonNull Object dstArray,
    long dstOffset,
    long byteCount
  ) {
    if (byteCount > 0 && srcArrayKind != ValueTypeKind.REF && dstArrayKind != ValueTypeKind.REF) {
      var srcStripe = System.identityHashCode(srcArray) & (ACCESS_LOCKS.length - 1);
      var dstStripe = System.identityHashCode(dstArray) & (ACCESS_LOCKS.length - 1);
      synchronized (ACCESS_LOCKS[Math.min(srcStripe, dstStripe)]) {
        synchronized (ACCESS_LOCKS[Math.max(srcStripe, dstStripe)]) {
          var srcSegment = srcArrayKind.createArrayMemorySegment(srcArray);
          var dstSegment = dstArrayKind.createArrayMemorySegment(dstArray);
          MemorySegment.copy(srcSegment, srcOffset, dstSegment, dstOffset, byteCount);
          copyByteSegmentToBoolArray(dstSegment, dstArray, dstOffset, byteCount);
        }
      }
    }
  }

  /**
   * Fills the given array with the given value.
   *
   * @param kind      the kind of the array to fill.
   * @param array     the array to fill.
   * @param offset    the offset into the array to fill from.
   * @param byteCount the number of bytes to fill.
   * @param value     the value to fill the array with.
   */
  public static void arrayFill(
    @NonNull ValueTypeKind kind,
    @NonNull Object array,
    long offset,
    long byteCount,
    byte value
  ) {
    synchronized (accessLock(array)) {
      if (byteCount > 0 && kind != ValueTypeKind.REF) {
        if (kind == ValueTypeKind.BOOL) {
          // boolean arrays need special treatment, they are not natively supported by
          // memory segments, so we fill them using the array utilities instead
          var arr = (boolean[]) array;
          Objects.checkFromIndexSize(offset, byteCount, arr.length);
          Arrays.fill(arr, (int) offset, (int) (offset + byteCount), value != 0);
          return;
        }

        var segment = kind.createArrayMemorySegment(array);
        segment.asSlice(offset, byteCount).fill(value);
      }
    }
  }
  //</editor-fold>
}
