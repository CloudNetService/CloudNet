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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.UUID;
import lombok.NonNull;

/**
 * An event which gets called when a deferred service creation gets a state update, either by successfully creating the
 * service or by failing if the maximum retry count is reached.
 * <p>
 * This event is only called on all channel message receivers which were defined previously.
 *
 * @since 4.0
 */
public final class CloudServiceDeferredStateEvent extends Event {

  private final UUID creationId;
  private final ServiceCreateResult createResult;

  /**
   * Constructs a new deferred state event instance.
   *
   * @param creationId   the id submitted in the initial result of the service creation.
   * @param createResult the result of the creation process.
   * @throws NullPointerException if the given creation id or result is null.
   */
  public CloudServiceDeferredStateEvent(@NonNull UUID creationId, @NonNull ServiceCreateResult createResult) {
    this.creationId = creationId;
    this.createResult = createResult;
  }

  /**
   * Get the id of the deferred result this event is associated with. The same id was given in the service create result
   * with the {@code DEFERRED} state.
   *
   * @return the creation id of the deferred service.
   */
  public @NonNull UUID deferredCreationId() {
    return this.creationId;
  }

  /**
   * Get the state of the service creation. Equivalent to {@code serviceCreateResult().state()}.
   *
   * @return the state of the service creation.
   */
  public @NonNull ServiceCreateResult.State state() {
    return this.createResult.state();
  }

  /**
   * Get the underlying service of the service result. This method throws an exception if the state of the creation is
   * not {@code CREATED}. Equivalent to {@code serviceCreateResult().createdService()}.
   *
   * @return the underlying service of the service result.
   * @throws IllegalStateException if the underlying result was not completed successfully.
   */
  public @NonNull ServiceInfoSnapshot createdService() {
    return this.createResult.serviceInfo();
  }

  /**
   * Get the raw underlying create result.
   *
   * @return the raw underlying create result.
   */
  public @NonNull ServiceCreateResult serviceCreateResult() {
    return this.createResult;
  }
}
