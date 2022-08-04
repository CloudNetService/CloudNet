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

package eu.cloudnetservice.driver.event.events.service;

import eu.cloudnetservice.driver.event.events.DriverEvent;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.UUID;
import lombok.NonNull;

public final class CloudServiceDeferredStateEvent extends DriverEvent {

  private final UUID creationId;
  private final ServiceCreateResult createResult;

  public CloudServiceDeferredStateEvent(@NonNull UUID creationId, @NonNull ServiceCreateResult createResult) {
    this.creationId = creationId;
    this.createResult = createResult;
  }

  public @NonNull UUID deferredCreationId() {
    return this.creationId;
  }

  public @NonNull ServiceCreateResult.State state() {
    return this.createResult.state();
  }

  public @NonNull ServiceInfoSnapshot createdService() {
    return this.createResult.serviceInfo();
  }

  public @NonNull ServiceCreateResult serviceCreateResult() {
    return this.createResult;
  }
}
