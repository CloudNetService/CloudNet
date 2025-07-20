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

/**
 * Implements all unsafe operations that are using off-heap memory.
 *
 * @since 4.0
 */
final class MemoryOps {

  /**
   * Memory segment that represents the full memory of the current host.
   */
  private static final MemorySegment FULL_MEMORY_SEGMENT = MemorySegment.ofAddress(0L).reinterpret(Long.MAX_VALUE);

  private MemoryOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if the given address allows for aligned access of the given type kind.
   *
   * @param kind    the kind to check for.
   * @param address the address to check.
   * @return true if the access is aligned, false otherwise.
   * @throws NullPointerException if the given value kind is null.
   */
  private static boolean isAlignedAccess(@NonNull ValueTypeKind kind, long address) {
    var byteSize = kind.byteSize();
    return address % byteSize == 0;
  }

  /**
   * Get the given type kind value at the given offset in off-heap memory using the given get operation type.
   *
   * @param kind   the type kind to get the value at the given offset of.
   * @param op     the get operation type to use for getting the value.
   * @param offset the offset into the off-heap memory to get the value from.
   * @return the value of the given kind at the given offset in off-heap memory.
   * @throws NullPointerException if the given type kind or get operation type is null.
   */
  public static @NonNull Object memGet(@NonNull ValueTypeKind kind, @NonNull OpConstants.GetOp op, long offset) {
    if (isAlignedAccess(kind, offset)) {
      var handle = kind.layoutVarHandle();
      return switch (op) {
        case DEFAULT -> handle.get(FULL_MEMORY_SEGMENT, offset);
        case VOLATILE -> handle.getVolatile(FULL_MEMORY_SEGMENT, offset);
      };
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      return handle.get(FULL_MEMORY_SEGMENT, offset);
    }
  }

  /**
   * Puts the given value at the given offset in off-heap memory using the given set operation type.
   *
   * @param kind   the type kind to put the value at the given offset of.
   * @param op     the set operation type to use for setting the value.
   * @param offset the offset into the off-heap memory to set the value at.
   * @param value  the value to set at the given offset in off-heap memory.
   * @throws NullPointerException if the given type kind, set operation type or value is null.
   */
  public static void memPut(
    @NonNull ValueTypeKind kind,
    @NonNull OpConstants.SetOp op,
    long offset,
    @NonNull Object value
  ) {
    if (isAlignedAccess(kind, offset)) {
      var handle = kind.layoutVarHandle();
      switch (op) {
        case DEFAULT -> handle.set(FULL_MEMORY_SEGMENT, offset, value);
        case VOLATILE -> handle.setVolatile(FULL_MEMORY_SEGMENT, offset, value);
        case RELEASE -> handle.setRelease(FULL_MEMORY_SEGMENT, offset, value);
      }
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      handle.set(FULL_MEMORY_SEGMENT, offset, value);
    }
  }

  /**
   * Gets the value at the given offset in off-heap memory and sets it to the given value.
   *
   * @param kind   the type kind to get and set the value at the given offset of.
   * @param offset the offset into the off-heap memory to get and set the value at.
   * @param value  the value to set at the given offset in off-heap memory.
   * @return the old value at the given offset in off-heap memory.
   * @throws NullPointerException if the given type kind or value is null.
   */
  public static @NonNull Object memGetPut(@NonNull ValueTypeKind kind, long offset, @NonNull Object value) {
    if (isAlignedAccess(kind, offset)) {
      var handle = kind.layoutVarHandle();
      return handle.getAndSet(FULL_MEMORY_SEGMENT, offset, value);
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      var current = handle.get(FULL_MEMORY_SEGMENT, offset);
      handle.set(FULL_MEMORY_SEGMENT, offset, value);
      return current;
    }
  }

  /**
   * Gets the value at the given offset in off-heap memory and adds the given value to it.
   *
   * @param kind   the type kind to get and add the value at the given offset of.
   * @param offset the offset into the off-heap memory to get and add the value at.
   * @param delta  the value to add at the given offset in off-heap memory.
   * @return the old value at the given offset in off-heap memory.
   * @throws NullPointerException if the given type kind or value is null.
   */
  // NOTE: kind can only be INT or LONG
  public static @NonNull Object memGetAdd(@NonNull ValueTypeKind kind, long offset, @NonNull Number delta) {
    if (isAlignedAccess(kind, offset)) {
      var handle = kind.layoutVarHandle();
      return handle.getAndAdd(FULL_MEMORY_SEGMENT, offset, delta);
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      return switch (kind) {
        case INT -> {
          var current = (int) handle.get(FULL_MEMORY_SEGMENT, offset);
          var newValue = current + delta.intValue();
          handle.set(FULL_MEMORY_SEGMENT, offset, newValue);
          yield current;
        }
        case LONG -> {
          var current = (long) handle.get(FULL_MEMORY_SEGMENT, offset);
          var newValue = current + delta.longValue();
          handle.set(FULL_MEMORY_SEGMENT, offset, newValue);
          yield current;
        }
        default -> throw new AssertionError();
      };
    }
  }

  /**
   * Sets the value at the given offset in off-heap memory to the given value if the current value is equal to the given
   * expected value.
   *
   * @param kind     the type kind to get and possibly set the value at the given offset of.
   * @param offset   the offset into the off-heap memory to get and possibly set the value at.
   * @param expected the expected value to compare with the current value.
   * @param value    the value to set at the given offset in off-heap memory.
   * @return true if the swap was successful, false otherwise.
   * @throws NullPointerException if the given type kind, expected value or new value is null.
   */
  public static boolean memComparePut(
    @NonNull ValueTypeKind kind,
    long offset,
    @NonNull Object expected,
    @NonNull Object value
  ) {
    if (isAlignedAccess(kind, offset)) {
      var handle = kind.layoutVarHandle();
      return handle.compareAndSet(FULL_MEMORY_SEGMENT, offset, expected, value);
    } else {
      var handle = kind.unalignedLayoutVarHandle();
      var current = handle.get(FULL_MEMORY_SEGMENT, offset);
      if (kind.areValuesEqual(expected, current)) {
        handle.set(FULL_MEMORY_SEGMENT, offset, value);
        return true;
      }
      return false;
    }
  }

  /**
   * Copies the given off-heap memory range from the given source offset to the given destination offset.
   *
   * @param srcOffset the source offset to copy from.
   * @param dstOffset the destination offset to copy to.
   * @param byteCount the number of bytes to copy.
   */
  public static void memCopy(long srcOffset, long dstOffset, long byteCount) {
    if (byteCount > 0) {
      // checks are not needed here, they are all performed by MemorySegment
      MemorySegment.copy(FULL_MEMORY_SEGMENT, srcOffset, FULL_MEMORY_SEGMENT, dstOffset, byteCount);
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
    if (byteCount > 0 && kind != ValueTypeKind.REF) {
      var dstSegment = kind.createArrayMemorySegment(dstArray);
      MemorySegment.copy(FULL_MEMORY_SEGMENT, srcOffset, dstSegment, dstOffset, byteCount);

      // boolean arrays need special post-copy handling, they are not natively supported by
      // memory segments, so we copy the array into a byte array - now we need to copy back
      if (kind == ValueTypeKind.BOOL) {
        var dst = (boolean[]) dstArray;
        var src = (byte[]) dstSegment.heapBase().orElseThrow();
        for (var index = 0; index < dst.length; index++) {
          var idx = Math.toIntExact(dstOffset + index);
          dst[idx] = src[idx] != 0;
        }
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
    if (byteCount > 0 && kind != ValueTypeKind.REF) {
      var srcSegment = kind.createArrayMemorySegment(srcArray);
      MemorySegment.copy(srcSegment, srcOffset, FULL_MEMORY_SEGMENT, dstOffset, byteCount);
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
      var slice = FULL_MEMORY_SEGMENT.asSlice(offset, byteCount);
      slice.fill(value);
    }
  }
}
