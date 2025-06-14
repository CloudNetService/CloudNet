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

package eu.cloudnetservice.driver.impl.network.netty.memory;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import lombok.NonNull;

/**
 * Unsafe allocator implementation for memory segments. Introduced to prevent checks when a shared arena (usually used
 * to allocate memory segments) is closed. This implementation just frees the provided memory segment, regardless of
 * possible in-flight usages.
 *
 * @since 4.0
 */
final class MemorySegmentAllocator {

  private static final MethodHandle MALLOC;
  private static final MethodHandle FREE;

  static {
    var linker = Linker.nativeLinker();

    // void* malloc(size_t byte_size)
    var mallocAddress = linker.defaultLookup().findOrThrow("malloc");
    MALLOC = linker.downcallHandle(mallocAddress, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // void free(void* mem_block)
    var freeAddress = linker.defaultLookup().findOrThrow("free");
    FREE = linker.downcallHandle(freeAddress, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Validates that the given memory segment was successfully allocated, throwing an exception if that is not the case.
   *
   * @param segment   the segment to validate.
   * @param byteCount the requested byte count that should be allocated.
   * @throws NullPointerException if the given segment is null.
   * @throws OutOfMemoryError     if the given segment was incorrectly allocated.
   */
  private static void validateAllocatedSegment(@NonNull MemorySegment segment, long byteCount) {
    if (segment.address() == 0L) {
      throw new OutOfMemoryError("Could not allocate " + byteCount + " bytes of memory");
    }
  }

  /**
   * Allocates an off-heap memory block of the given requested size and returns an {@code MemorySegment} that wraps it.
   * The memory is zeroed out.
   *
   * @param byteCount the count of bytes to allocate.
   * @return a memory segment wrapping the allocated block of memory.
   * @throws IllegalStateException if the invocation of the native malloc method failed.
   * @throws OutOfMemoryError      if the given byte count couldn't be reserved by the operating system.
   */
  public static @NonNull MemorySegment malloc(long byteCount) {
    try {
      var segment = (MemorySegment) MALLOC.invokeExact(byteCount);
      validateAllocatedSegment(segment, byteCount);
      return segment.reinterpret(byteCount).fill((byte) 0);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Could not malloc memory", throwable);
    }
  }

  /**
   * Frees the memory block associated with the given memory segment.
   *
   * @param segment the segment representing the memory block to free.
   * @throws NullPointerException  if the given memory segment is null.
   * @throws IllegalStateException if the invocation of the native free method failed.
   */
  public static void free(@NonNull MemorySegment segment) {
    try {
      FREE.invokeExact(segment);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Could not free memory", throwable);
    }
  }
}
