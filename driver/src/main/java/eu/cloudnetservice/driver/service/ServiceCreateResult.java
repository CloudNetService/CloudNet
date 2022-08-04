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

public record ServiceCreateResult(
  @NonNull State state,
  @Nullable UUID creationId,
  @Nullable ServiceInfoSnapshot serviceInfo
) {

  public static final ServiceCreateResult FAILED = new ServiceCreateResult(State.FAILED, null, null);

  public ServiceCreateResult {
    // creation id must be present for deferred and created
    Preconditions.checkArgument(state == State.FAILED || creationId != null);
    // created services must have a service information present
    Preconditions.checkArgument(state != State.CREATED || serviceInfo != null);
  }

  public static @NonNull ServiceCreateResult deferred(@NonNull UUID creationId) {
    return new ServiceCreateResult(State.CREATED, creationId, null);
  }

  public static @NonNull ServiceCreateResult created(@NonNull ServiceInfoSnapshot serviceInfo) {
    return new ServiceCreateResult(State.CREATED, serviceInfo.serviceId().uniqueId(), serviceInfo);
  }

  @Override
  public @NonNull UUID creationId() {
    // we could check for state != State.FAILED as well, but then IJ gives a warning that creationId might be null
    Preconditions.checkNotNull(this.creationId, "Cannot retrieve creationId for State.FAILED");
    return this.creationId;
  }

  @Override
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    // we could check for state == State.CREATED as well, but then IJ gives a warning that serviceInfo might be null
    Preconditions.checkNotNull(this.serviceInfo, "Can only retrieve service info for State.CREATED");
    return this.serviceInfo;
  }

  public enum State {

    CREATED,
    DEFERRED,
    FAILED
  }
}
