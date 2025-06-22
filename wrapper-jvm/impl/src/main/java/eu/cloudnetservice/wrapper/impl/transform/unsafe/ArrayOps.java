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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.foreign.MemorySegment;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements all unsafe operations that are using on-heap arrays.
 *
 * @since 4.0
 */
final class ArrayOps {

  private ArrayOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if an array can be directly accessed without needing to wrap it into a memory segment.
   *
   * @param arrayKind the kind of array.
   * @param valueKind the kind of value to read/put into the target array.
   * @param off       the offset into the array to access.
   * @return true if direct access is possible, false otherwise.
   * @throws NullPointerException if the given array kind or value kind is null.
   */
  private static boolean canUseDirectArrayAccess(
    @NonNull ValueTypeKind arrayKind,
    @NonNull ValueTypeKind valueKind,
    long off
  ) {
    if (arrayKind == ValueTypeKind.REF || valueKind == ValueTypeKind.REF) {
      // object arrays cannot be wrapped by memory segments, so they must use direct access
      return true;
    }

    // otherwise, array access can be done directly for the same kind of array with aligned access
    var valueScale = valueKind.byteSize();
    return valueKind == arrayKind && off % valueScale == 0;
  }

  /**
   * Checks if the given offset is aligned access into a memory segment.
   *
   * @param arrayKind the kind of array.
   * @param valueKind the kind of value to read/put into the target array.
   * @param off       the offset into the array to access.
   * @return true if direct access is possible, false otherwise.
   * @throws NullPointerException if the given array kind or value kind is null.
   */
  private static boolean isAlignedAccess(@NonNull ValueTypeKind arrayKind, @NonNull ValueTypeKind valueKind, long off) {
    var arrayScale = arrayKind.byteSize();
    var valueScale = valueKind.byteSize();
    return arrayScale >= valueScale && off % valueScale == 0;
  }

  /**
   * Copies the byte memory segment into the given boolean array.
   *
   * @param segment the byte segment to copy from.
   * @param array   the boolean array to copy into.
   * @param off     the offset into the array to start copying from.
   * @param count   the count of bytes to copy.
   * @throws NullPointerException if the given segment or array is null.
   */
  private static void copyByteSegmentToBoolArray(
    @NonNull MemorySegment segment,
    @NonNull Object array,
    long off,
    long count
  ) {
    var componentType = array.getClass().getComponentType();
    if (componentType == boolean.class) {
      var dst = (boolean[]) array;
      var src = (byte[]) segment.heapBase().orElseThrow();
      for (var index = 0L; index < count; index++) {
        var idx = Math.toIntExact(off + index);
        dst[idx] = src[idx] != 0;
      }
    }
  }

  /**
   * Get an element of the given requested type at the given offset in the given array.
   *
   * @param kind   the kind of element to get from the array.
   * @param op     the operation kind to use when getting the element, ignored on unaligned access.
   * @param array  the array to get the value from.
   * @param offset the offset into the array to get the value at.
   * @return the value in the array at the given offset.
   * @throws NullPointerException if the given kind, get operation or array is null.
   */
  public static @Nullable Object arrayGet(
    @NonNull ValueTypeKind kind,
    @NonNull OpConstants.GetOp op,
    @NonNull Object array,
    long offset
  ) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (canUseDirectArrayAccess(arrayKind, kind, offset)) {
      // can access the given array directly without wrapping
      var handle = arrayKind.arrayElementVarHandle();
      var arrayIndex = Math.toIntExact(offset / arrayKind.byteSize());
      return switch (op) {
        case DEFAULT -> handle.get(array, arrayIndex);
        case VOLATILE -> handle.getVolatile(array, arrayIndex);
      };
    }

    // check if aligned access to the elements in the array is possible, that is
    // if an element can read from the array without requiring two operations to do so
    var arrayMemorySegment = arrayKind.createArrayMemorySegment(array);
    if (isAlignedAccess(arrayKind, kind, offset)) {
      var handle = kind.layoutVarHandle();
      return switch (op) {
        case DEFAULT -> handle.get(arrayMemorySegment, offset);
        case VOLATILE -> handle.getVolatile(arrayMemorySegment, offset);
      };
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      return handle.get(arrayMemorySegment, offset);
    }
  }

  /**
   * Puts the given value at the given offset in the given array.
   *
   * @param kind   the kind of array.
   * @param op     the operation to use for setting the value.
   * @param array  the array instance.
   * @param offset the offset into the given array to set the value at.
   * @param value  the value to set at the given offset in the given array.
   * @throws NullPointerException if the given array kind, set operation type or array is null.
   */
  public static void arrayPut(
    @NonNull ValueTypeKind kind,
    @NonNull OpConstants.SetOp op,
    @NonNull Object array,
    long offset,
    @Nullable Object value
  ) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (canUseDirectArrayAccess(arrayKind, kind, offset)) {
      // can access the given array directly without wrapping
      var handle = arrayKind.arrayElementVarHandle();
      var arrayIndex = Math.toIntExact(offset / arrayKind.byteSize());
      switch (op) {
        case DEFAULT -> handle.set(array, arrayIndex, value);
        case VOLATILE -> handle.setVolatile(array, arrayIndex, value);
        case RELEASE -> handle.setRelease(array, arrayIndex, value);
      }
      return;
    }

    // check if aligned access to the elements in the array is possible, that is
    // if an element can read from the array without requiring two operations to do so
    var arrayMemorySegment = arrayKind.createArrayMemorySegment(array);
    if (isAlignedAccess(arrayKind, kind, offset)) {
      var handle = kind.layoutVarHandle();
      switch (op) {
        case DEFAULT -> handle.set(arrayMemorySegment, offset, value);
        case VOLATILE -> handle.setVolatile(arrayMemorySegment, offset, value);
        case RELEASE -> handle.setRelease(arrayMemorySegment, offset, value);
      }
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      handle.set(arrayMemorySegment, offset, value);
    }

    // copy back a byte array segment into the source boolean array if necessary
    copyByteSegmentToBoolArray(arrayMemorySegment, array, offset, kind.byteSize());
  }

  /**
   * Get the value at the given offset in the given array, setting the value at the offset to the given value.
   *
   * @param kind   the kind of value to get and set.
   * @param array  the array to get and set the value on.
   * @param offset the offset into the array to get/set the value at.
   * @param value  the value to set after getting.
   * @return the value in the array before the set operation.
   * @throws NullPointerException if the given type kind or array is null.
   */
  public static @Nullable Object arrayGetPut(
    @NonNull ValueTypeKind kind,
    @NonNull Object array,
    long offset,
    @Nullable Object value
  ) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (canUseDirectArrayAccess(arrayKind, kind, offset)) {
      // can access the given array directly without wrapping
      var handle = arrayKind.arrayElementVarHandle();
      var arrayIndex = Math.toIntExact(offset / arrayKind.byteSize());
      return handle.getAndSet(array, arrayIndex, value);
    }

    // check if aligned access to the elements in the array is possible, that is
    // if an element can read from the array without requiring two operations to do so
    var arrayMemorySegment = arrayKind.createArrayMemorySegment(array);
    if (isAlignedAccess(arrayKind, kind, offset)) {
      var handle = kind.layoutVarHandle();
      var ret = handle.getAndSet(arrayMemorySegment, offset, value);
      copyByteSegmentToBoolArray(arrayMemorySegment, array, offset, kind.byteSize());
      return ret;
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      var current = handle.get(arrayMemorySegment, offset);
      handle.set(arrayMemorySegment, offset, value);
      copyByteSegmentToBoolArray(arrayMemorySegment, array, offset, kind.byteSize());
      return current;
    }
  }

  /**
   * Gets the value at the given offset in the given array and adds the given value to it.
   *
   * @param kind   the kind of array.
   * @param array  the array instance.
   * @param offset the offset into the given array to get the value from.
   * @param delta  the value to add at the given offset in the given array.
   * @return the old value at the given offset in the given array, possibly null.
   * @throws NullPointerException if the given array kind or array is null.
   */
  // NOTE: kind can only be INT or LONG
  public static @Nullable Object arrayGetAdd(
    @NonNull ValueTypeKind kind,
    @NonNull Object array,
    long offset,
    @NonNull Number delta
  ) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (arrayKind == ValueTypeKind.REF || arrayKind == ValueTypeKind.BOOL || arrayKind == ValueTypeKind.CHAR) {
      // cant add to this kind of array value, but can get from it
      var arrayIndex = Math.toIntExact(offset / arrayKind.byteSize());
      var handle = arrayKind.arrayElementVarHandle();
      return handle.get(array, arrayIndex);
    }

    // check if aligned access to the elements in the array is possible, that is
    // if an element can read from the array without requiring two operations to do so
    var arrayMemorySegment = arrayKind.createArrayMemorySegment(array);
    if (isAlignedAccess(arrayKind, kind, offset)) {
      var handle = kind.layoutVarHandle();
      return handle.getAndAdd(arrayMemorySegment, offset, delta);
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      return switch (kind) {
        case INT -> {
          var current = (int) handle.get(arrayMemorySegment, offset);
          var newValue = current + delta.intValue();
          handle.set(arrayMemorySegment, offset, newValue);
          yield current;
        }
        case LONG -> {
          var current = (long) handle.get(arrayMemorySegment, offset);
          var newValue = current + delta.longValue();
          handle.set(arrayMemorySegment, offset, newValue);
          yield current;
        }
        default -> throw new AssertionError();
      };
    }
  }

  /**
   * Sets the value at the given offset in the given array to the given if the current value is equal to the given
   * expected value. This method returns if the swap was successful.
   *
   * @param kind     the kind of array.
   * @param array    the array instance.
   * @param offset   the offset into the given array to get the value from.
   * @param expected the expected value to compare with the current value.
   * @param value    the value to set at the given offset in the given array.
   * @return true if the swap was successful, false otherwise.
   * @throws NullPointerException if the given array kind or array is null.
   */
  public static boolean arrayComparePut(
    @NonNull ValueTypeKind kind,
    @NonNull Object array,
    long offset,
    @Nullable Object expected,
    @Nullable Object value
  ) {
    var arrayKind = ValueTypeKind.of(array.getClass().getComponentType());
    if (canUseDirectArrayAccess(arrayKind, kind, offset)) {
      // cant add to a boolean or object array, but can get from it
      var handle = arrayKind.arrayElementVarHandle();
      var arrayIndex = Math.toIntExact(offset / arrayKind.byteSize());
      return handle.compareAndSet(array, arrayIndex, expected, value);
    }

    // check if aligned access to the elements in the array is possible, that is
    // if an element can read from the array without requiring two operations to do so
    var arrayMemorySegment = arrayKind.createArrayMemorySegment(array);
    if (isAlignedAccess(arrayKind, kind, offset)) {
      var handle = kind.layoutVarHandle();
      var result = handle.compareAndSet(arrayMemorySegment, offset, expected, value);
      if (result) {
        copyByteSegmentToBoolArray(arrayMemorySegment, array, offset, kind.byteSize());
      }
      return result;
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      var current = handle.get(arrayMemorySegment, offset);
      if (kind.areValuesEqual(expected, current)) {
        handle.set(arrayMemorySegment, offset, value);
        copyByteSegmentToBoolArray(arrayMemorySegment, array, offset, kind.byteSize());
        return true;
      }
      return false;
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
      var srcSegment = srcArrayKind.createArrayMemorySegment(srcArray);
      var dstSegment = dstArrayKind.createArrayMemorySegment(dstArray);
      MemorySegment.copy(srcSegment, srcOffset, dstSegment, dstOffset, byteCount);
      copyByteSegmentToBoolArray(dstSegment, dstArray, dstOffset, byteCount);
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
    if (byteCount > 0 && kind != ValueTypeKind.REF) {
      if (kind == ValueTypeKind.BOOL) {
        // boolean arrays need special treatment, they are not natively supported by
        // memory segments, so we fill them manually using a loop instead
        var arr = (boolean[]) array;
        var off = Math.toIntExact(offset);
        var maxWrite = Math.toIntExact(byteCount);
        for (var index = 0; index < maxWrite; index++) {
          arr[off + index] = value != 0;
        }
        return;
      }

      var segment = kind.createArrayMemorySegment(array);
      segment.asSlice(offset, byteCount).fill(value);
    }
  }
}
