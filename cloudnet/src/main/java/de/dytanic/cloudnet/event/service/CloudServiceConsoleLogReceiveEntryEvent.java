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

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event is called whenever a new console line is read from a service on this node, it won't be called in the
 * cluster.
 */
public final class CloudServiceConsoleLogReceiveEntryEvent extends DriverEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  private final String message;

  private final boolean errorMessage;

  public CloudServiceConsoleLogReceiveEntryEvent(ServiceInfoSnapshot serviceInfoSnapshot, String message,
    boolean errorMessage) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
    this.message = message;
    this.errorMessage = errorMessage;
  }

  @Override
  public boolean isShowDebug() {
    return false;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }

  public String getMessage() {
    return this.message;
  }

  public boolean isErrorMessage() {
    return this.errorMessage;
  }

}
