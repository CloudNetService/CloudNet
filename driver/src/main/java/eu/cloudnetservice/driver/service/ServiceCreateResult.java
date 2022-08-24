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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a service creation. A result can have three states:
 * <ol>
 *   <li>CREATED: the service was created successfully, the service info is available via {@link #serviceInfo()}.
 *   <li>DEFERRED: this state can only happen when a retry configuration was specified during the creation of the
 *   service. In that case, the creation id of the result can be used to listen to
 *   the {@link eu.cloudnetservice.driver.event.events.service.CloudServiceDeferredStateEvent} to catch updates
 *   of the retries.
 *   <li>FAILED: indicates that the service couldn't be created.
 * </ol>
 *
 * @param state       the creation state of the service.
 * @param creationId  the creation id of this result, if the state is not {@code FAILED}.
 * @param serviceInfo the underlying service info, if the service was created successfully.
 * @since 4.0
 */
public record ServiceCreateResult(
  @NonNull State state,
  @Nullable UUID creationId,
  @Nullable ServiceInfoSnapshot serviceInfo
) {

  /**
   * A static create result indicating that the service creation failed.
   */
  public static final ServiceCreateResult FAILED = new ServiceCreateResult(State.FAILED, null, null);

  /**
   * Creates a new result instance.
   *
   * @param state       the creation state of the service.
   * @param creationId  the creation id of this result, if the state is not {@code FAILED}.
   * @param serviceInfo the underlying service info, if the service was created successfully.
   * @throws NullPointerException     if the given state is null.
   * @throws IllegalArgumentException if the creation id is missing or the service info depending on the given state.
   */
  public ServiceCreateResult {
    // creation id must be present for deferred and created
    Preconditions.checkArgument(state == State.FAILED || creationId != null);
    // created services must have a service information present
    Preconditions.checkArgument(state != State.CREATED || serviceInfo != null);
  }

  /**
   * Creates a new result with the state set to deferred and using the given creation id.
   *
   * @param creationId the creation id for deferred service event updates.
   * @return a new result instance.
   * @throws NullPointerException if the given creation id is null.
   */
  public static @NonNull ServiceCreateResult deferred(@NonNull UUID creationId) {
    return new ServiceCreateResult(State.DEFERRED, creationId, null);
  }

  /**
   * Creates a new result with the state set to created and using the given service info.
   *
   * @param serviceInfo the service info of the created service.
   * @return a new result instance.
   * @throws NullPointerException if the given service info is null.
   */
  public static @NonNull ServiceCreateResult created(@NonNull ServiceInfoSnapshot serviceInfo) {
    return new ServiceCreateResult(State.CREATED, serviceInfo.serviceId().uniqueId(), serviceInfo);
  }

  /**
   * Get the creation id of this create result. If the service was created successfully, this id is set to the unique id
   * of the service. If the creation was deferred the id is set to a unique notification id which can be used to
   * identify the final creation result.
   *
   * @return the creation id of this service.
   * @throws IllegalStateException if called when the state of this result is {@code FAILED}.
   */
  @Override
  public @NonNull UUID creationId() {
    // we could check for state != State.FAILED as well, but then IJ gives a warning that creationId might be null
    Preconditions.checkState(this.creationId != null, "Cannot retrieve creationId for State.FAILED");
    return this.creationId;
  }

  /**
   * Get the service info of the service which was created. This is only present if the state of this result is
   * {@code CREATED}.
   *
   * @return the service info of the created service.
   * @throws IllegalStateException if called when the state of this result is not {@code CREATED}.
   */
  @Override
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    // we could check for state == State.CREATED as well, but then IJ gives a warning that serviceInfo might be null
    Preconditions.checkState(this.serviceInfo != null, "Can only retrieve service info for State.CREATED");
    return this.serviceInfo;
  }

  /**
   * Represents the state of a service creation.
   *
   * @since 4.0
   */
  public enum State {

    /**
     * The service was created successfully and is fully available for further actions.
     */
    CREATED,
    /**
     * the service creation was deferred and retries to create the service will be made based on the supplied retry
     * configuration. Updates of the creation will be sent to all subscribed channel message targets using the present
     * creation id as an identifier for the service create.
     */
    DEFERRED,
    /**
     * The service couldn't be created. Either no retry configuration was present and there was no successful creation
     * possible, or the maximum retry count was reached without a successful creation.
     */
    FAILED
  }
}
