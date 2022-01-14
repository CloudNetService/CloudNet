/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.service;

import eu.cloudnetservice.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import java.util.Arrays;
import java.util.UUID;
import lombok.NonNull;

/**
 * The current state of a service.
 */
public enum ServiceLifeCycle {

  /**
   * This is the state directly after DEFINED or after STOPPED (only if autoDeleteOnStop is disabled for the service).
   * It will be prepared until it is changed to RUNNING by {@link SpecificCloudServiceProvider#start()}.
   */
  PREPARED(1, 2, 3),
  /**
   * This is the state after PREPARED. It is invoked by {@link SpecificCloudServiceProvider#start()}. It will be running
   * until the process of the service has exited.
   */
  RUNNING(2, 3),
  /**
   * This is the state after RUNNING. It is invoked by exiting the process. This will only be for a very short time
   * after the process has exited. There are two possibilities for the next state: - If autoDeleteOnStop is enabled, the
   * state will be switched to DELETED. - If autoDeleteOnStop is disabled, the state will be switched to PREPARED.
   */
  STOPPED(0, 3),
  /**
   * This is the state after STOPPED. When this state is set, the service is no more registered in the cloud and methods
   * like {@link GeneralCloudServiceProvider#service(UUID)} won't return this service anymore.
   */
  DELETED;

  private final int[] possibleChangeTargetOrdinals;

  /**
   * Creates a new service lifecycle enum constant instance.
   *
   * @param possibleChangeTargetOrdinals all ordinal indexes of other lifecycles this lifecycle can be changed to.
   */
  ServiceLifeCycle(int... possibleChangeTargetOrdinals) {
    this.possibleChangeTargetOrdinals = possibleChangeTargetOrdinals;
    Arrays.sort(this.possibleChangeTargetOrdinals);
  }

  /**
   * Checks if a service can change from this state to the given target.
   *
   * @param target the target state the service want's to change to.
   * @return If the service can change from the current into the target state.
   */
  public boolean canChangeTo(@NonNull ServiceLifeCycle target) {
    return Arrays.binarySearch(this.possibleChangeTargetOrdinals, target.ordinal()) >= 0;
  }
}
