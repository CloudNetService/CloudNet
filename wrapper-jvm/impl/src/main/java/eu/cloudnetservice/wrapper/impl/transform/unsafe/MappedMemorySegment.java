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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A memory segment that was allocated for usage with the unsafe replacement. Provides the actual allocated segment for
 * the user alongside the associated arena to free the memory.
 *
 * @since 4.0
 */
final class MappedMemorySegment {

  final MemorySegment segment;
  private final Arena arena;

  // used to prevent multiple close calls to an arena which is unsupported
  private final AtomicBoolean closed;

  /**
   * Creates a new mapped memory segment with the given size.
   *
   * @param size the size (in bytes) to allocate for the memory segment.
   * @throws IllegalArgumentException if the given size is negative.
   */
  public MappedMemorySegment(long size) {
    this.arena = Arena.ofShared();
    this.segment = this.arena.allocate(size);
    this.closed = new AtomicBoolean(false);
  }

  /**
   * Disposes the wrapped memory segment. This method does nothing if the segment has already been disposed.
   */
  public void dispose() {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.arena.close();
      } catch (Exception exception) {
        UnsafeLogUtil.warn("Failed to close arena of mapped memory segment: {}", exception.getMessage());
      }
    }
  }
}
