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

package eu.cloudnetservice.driver.service;

import java.util.Arrays;
import lombok.NonNull;

/**
 * Represents the state in which a service can be.
 *
 * @since 4.0
 */
public enum ServiceLifeCycle {

  /**
   * Represents the prepared state. If a service is in this state it can be started, stopped and deleted. It is also the
   * state into which a service changes if it was stopped and auto delete on stop is disabled.
   */
  PREPARED(1, 2, 3),
  /**
   * The service is running. This does not mean that the service is connected or ready to accept connections from
   * players, it does only mean that the service process was started.
   */
  RUNNING(2, 3),
  /**
   * The service was stopped. This is more of a marker state as the service will never actually be in that state, it is
   * only used to either the state of the service (which will then publish an update which has the stopped state as the
   * target) or the service will change into the prepared state (if auto delete on stop is disabled) or change in the
   * deleted state (if auto delete on stop is enabled).
   */
  STOPPED(0, 3),
  /**
   * The service was removed from the system and is no longer accessible.
   */
  DELETED;

  private final int[] possibleChangeTargetOrdinals;

  /**
   * Creates a new service lifecycle instance.
   *
   * @param possibleChangeTargetOrdinals all ordinal indexes of other lifecycles the lifecycle can change to.
   */
  ServiceLifeCycle(int... possibleChangeTargetOrdinals) {
    this.possibleChangeTargetOrdinals = possibleChangeTargetOrdinals;
    Arrays.sort(this.possibleChangeTargetOrdinals);
  }

  /**
   * Checks if this lifecycle can be changed to the given target lifecycle.
   *
   * @param target the target lifecycle to check.
   * @return true if the change from this lifecycle to the given one is acceptable, false otherwise.
   * @throws NullPointerException if the given target lifecycle is null.
   */
  public boolean canChangeTo(@NonNull ServiceLifeCycle target) {
    return Arrays.binarySearch(this.possibleChangeTargetOrdinals, target.ordinal()) >= 0;
  }
}
