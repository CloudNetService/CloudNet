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

package eu.cloudnetservice.driver.impl.network.object.data;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A holder for a statistic about the amount of bytes being allocated during another call. This can be used to reduce
 * the amount of buffer resizes being necessary to write data.
 *
 * @since 4.0
 */
final class AllocationStatistic {

  private static final VarHandle STATISTIC_HOLDER_UPDATER;

  static {
    try {
      var lookup = MethodHandles.lookup();
      STATISTIC_HOLDER_UPDATER = lookup.findVarHandle(AllocationStatistic.class, "statisticHolder", long[].class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  @SuppressWarnings("FieldMayBeFinal") // it can't be final, setting it via VarHandle
  private long[] statisticHolder = new long[2];

  /**
   * Adds the given amount of bytes from an allocation run into the statistic. If the given allocated bytes are negative
   * or a long overflow would occur adding it, the call is silently ignored.
   *
   * @param allocatedBytes the bytes that were allocated during a serialization.
   */
  public void add(int allocatedBytes) {
    // ignore negative additions
    if (allocatedBytes < 0) {
      return;
    }

    // get the current values from the current holder
    var currentRef = this.statisticHolder;
    var prevAllocatedBytes = currentRef[0];
    var prevAllocationCount = currentRef[1];

    // add the amount of bytes that were allocated in the run, do nothing in case the allocated byte
    // count overflows while trying to add the value - at that point we can assume that our average is
    // close enough that we can ignore the new count
    var newAllocatedBytes = prevAllocatedBytes + allocatedBytes;
    if (newAllocatedBytes >= prevAllocatedBytes) {
      var newHolder = new long[]{newAllocatedBytes, ++prevAllocationCount};
      STATISTIC_HOLDER_UPDATER.compareAndSet(this, currentRef, newHolder);
    }
  }

  /**
   * Get the average amount of bytes being allocated in the past allocation runs.
   *
   * @return the average amount of bytes being allocated in the past allocation runs.
   * @throws ArithmeticException if the allocation byte average overflows an int.
   */
  public int average() {
    var currentRef = this.statisticHolder;
    var allocatedBytes = currentRef[0];
    var allocationCount = currentRef[1];
    if (allocationCount > 0) {
      // this also ensures that the long division result does not overflow an int
      return Math.toIntExact(allocatedBytes / allocationCount);
    } else {
      // no allocations recorded yet
      return 0;
    }
  }
}
