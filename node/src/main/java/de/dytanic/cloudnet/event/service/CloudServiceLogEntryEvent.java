/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called whenever a new console line is read from a service on this node, it won't be called in the
 * cluster.
 */
public final class CloudServiceLogEntryEvent extends CloudServiceEvent {

  private final String message;
  private final boolean errorMessage;

  public CloudServiceLogEntryEvent(
    @NotNull ICloudService service,
    @NotNull String message,
    boolean errorMessage
  ) {
    super(service);

    this.message = message;
    this.errorMessage = errorMessage;
  }

  public @NotNull String getMessage() {
    return this.message;
  }

  public boolean isErrorMessage() {
    return this.errorMessage;
  }
}
