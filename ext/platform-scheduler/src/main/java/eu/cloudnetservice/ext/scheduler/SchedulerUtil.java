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

package eu.cloudnetservice.ext.scheduler;

import java.util.concurrent.TimeUnit;
import lombok.NonNull;

/**
 * Utility methods for scheduler implementations.
 *
 * @since 4.0
 */
final class SchedulerUtil {

  private static final long ONE_TICK_MS = 50L;
  private static final long HALF_TICK_MS = ONE_TICK_MS / 2;

  private SchedulerUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts the given duration in the given timeunit to the closest tick value, using round-half-up. For example, a
   * time of 24 milliseconds would be rounded down to 0 ticks, a time of 25 milliseconds gets rounded up to one tick.
   * Equally, 55 milliseconds equals one tick, 75 milliseconds 2 ticks and so on.
   *
   * @param timeUnit the unit of the given duration.
   * @param duration the duration to convert to ticks.
   * @return the given duration converted to ticks.
   * @throws NullPointerException if the given time unit is null.
   */
  public static long convertToTicks(@NonNull TimeUnit timeUnit, long duration) {
    var durationMs = timeUnit.toMillis(duration);
    return (durationMs + HALF_TICK_MS) / ONE_TICK_MS;
  }

  /**
   * Safely checks if a class with the given name is present without initializing it.
   *
   * @param className the name of the class to check for.
   * @return true if a class with the given name is present, false otherwise.
   * @throws NullPointerException if the given class name is null.
   */
  public static boolean isClassPresent(@NonNull String className) {
    try {
      Class.forName(className, false, SchedulerUtil.class.getClassLoader());
      return true;
    } catch (Throwable _) {
      return false;
    }
  }
}
