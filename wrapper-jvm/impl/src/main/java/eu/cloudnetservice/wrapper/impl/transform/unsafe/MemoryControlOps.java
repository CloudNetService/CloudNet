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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Implements all unsafe operations regarding memory control (allocation, reallocation and freeing).
 *
 * @since 4.0
 */
final class MemoryControlOps {

  private static final MethodHandle MALLOC;
  private static final MethodHandle REALLOC;
  private static final MethodHandle FREE;

  static {
    var linker = Linker.nativeLinker();

    // void* malloc(size_t byte_size)
    var mallocAddress = linker.defaultLookup().findOrThrow("malloc");
    MALLOC = linker.downcallHandle(mallocAddress, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // void* realloc(void* old_mem_block, size_t byte_size)
    var reallocAddress = linker.defaultLookup().findOrThrow("realloc");
    REALLOC = linker.downcallHandle(
      reallocAddress,
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // void free(void* mem_block)
    var freeAddress = linker.defaultLookup().findOrThrow("free");
    FREE = linker.downcallHandle(freeAddress, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Allocates an off-heap memory block of the given requested size and returns a pointer to the allocated memory. The
   * memory is not initialized. The returned pointer can be {@code 0} to indicate that the system refused to allocate
   * the given byte count.
   *
   * @param byteCount the count of bytes to allocate.
   * @return a pointer to the allocated memory block.
   * @throws IllegalStateException if the allocation failed for some reason.
   */
  public static long malloc(long byteCount) {
    try {
      var segment = (MemorySegment) MALLOC.invokeExact(byteCount);
      return segment.address();
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug("Failed to malloc {} bytes of memory", byteCount, throwable);
      throw new IllegalStateException("Could not malloc memory", throwable);
    }
  }

  /**
   * Changes the size of the memory block associated with the given address to the given byte count. The contents of the
   * memory will be unchanged in the range from the start of the region up to the minimum of the old and new sizes. If
   * the new size is larger than the old size, the added memory will not be initialized.The returned pointer can be
   * {@code 0} to indicate that the system refused to allocate the given byte count.
   *
   * @param address   the address of the previously allocated memory block to resize.
   * @param byteCount the new size of the memory block.
   * @return a pointer to the new, resized memory block.
   * @throws IllegalStateException if the reallocation failed for some reason.
   */
  public static long realloc(long address, long byteCount) {
    try {
      var oldSegment = MemorySegment.ofAddress(address);
      var segment = (MemorySegment) REALLOC.invokeExact(oldSegment, byteCount);
      return segment.address();
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug("Failed to realloc {} bytes of memory", byteCount, throwable);
      throw new IllegalStateException("Could not realloc memory", throwable);
    }
  }

  /**
   * Frees the memory block associated with the given address.
   *
   * @param address the address of the memory block to free.
   * @throws IllegalStateException if the freeing failed for some reason.
   */
  public static void free(long address) {
    try {
      var segment = MemorySegment.ofAddress(address);
      FREE.invokeExact(segment);
    } catch (Throwable throwable) {
      UnsafeLogUtil.debug("Failed to free memory", throwable);
      throw new IllegalStateException("Could not free memory", throwable);
    }
  }
}
