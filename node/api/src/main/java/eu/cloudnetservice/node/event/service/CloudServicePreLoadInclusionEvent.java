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

package eu.cloudnetservice.node.event.service;

import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;

public final class CloudServicePreLoadInclusionEvent extends CloudServiceEvent implements Cancelable {

  private boolean cancelled;
  private ServiceRemoteInclusion serviceRemoteInclusion;

  public CloudServicePreLoadInclusionEvent(
    @NonNull CloudService cloudService,
    @NonNull ServiceRemoteInclusion serviceRemoteInclusion
  ) {
    super(cloudService);

    this.serviceRemoteInclusion = serviceRemoteInclusion;
  }

  public @NonNull ServiceRemoteInclusion inclusion() {
    return this.serviceRemoteInclusion;
  }

  public void inclusion(@NonNull ServiceRemoteInclusion inclusion) {
    this.serviceRemoteInclusion = inclusion;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
