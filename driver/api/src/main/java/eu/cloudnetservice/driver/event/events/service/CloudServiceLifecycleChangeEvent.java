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

package eu.cloudnetservice.driver.event.events.service;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import lombok.NonNull;

/**
 * An event being fired when the lifecycle of a service changes. For example from running to stopped. Note that this
 * event is not fired for all service state changes. For example when auto delete on stop is enabled for a service, this
 * event will be fired for the change from running to deleted, but never from running to stopped nor from stopped to
 * deleted.
 * <p>
 * This event is fired after the change of the lifecycle, the new lifecycle is already set in the associated service
 * info.
 * <p>
 * This event is not called on the own node of the service.
 *
 * @since 4.0
 */
public final class CloudServiceLifecycleChangeEvent extends CloudServiceEvent {

  private final ServiceLifeCycle lastLifeCycle;

  /**
   * Constructs a new cloud service lifecycle event.
   *
   * @param lastLifeCycle the lifecycle of the service before changing to the new lifecycle.
   * @param info          the service info associated with this event.
   * @throws NullPointerException if either the last lifecycle or service info is null.
   */
  public CloudServiceLifecycleChangeEvent(@NonNull ServiceLifeCycle lastLifeCycle, @NonNull ServiceInfoSnapshot info) {
    super(info);
    this.lastLifeCycle = lastLifeCycle;
  }

  /**
   * Get the lifecycle the service was in before changing.
   *
   * @return the lifecycle the service was in before changing.
   */
  public @NonNull ServiceLifeCycle lastLifeCycle() {
    return this.lastLifeCycle;
  }

  /**
   * Get the lifecycle the service changed to. This lifecycle is already present in the service info associated with
   * this event.
   * <p>
   * This method call is equivalent to {@code serviceInfo().lifeCycle()}.
   *
   * @return the lifecycle the service changed to.
   */
  public @NonNull ServiceLifeCycle newLifeCycle() {
    return this.serviceInfo.lifeCycle();
  }
}
