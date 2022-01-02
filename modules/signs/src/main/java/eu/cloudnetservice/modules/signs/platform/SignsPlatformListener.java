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

package eu.cloudnetservice.modules.signs.platform;

import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import lombok.NonNull;

public class SignsPlatformListener {

  protected final PlatformSignManagement<?> signManagement;

  public SignsPlatformListener(PlatformSignManagement<?> signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handle(@NonNull CloudServiceUpdateEvent event) {
    this.signManagement.handleServiceUpdate(event.serviceInfo());
  }

  @EventListener
  public void handle(@NonNull CloudServiceLifecycleChangeEvent event) {
    switch (event.newLifeCycle()) {
      case STOPPED, DELETED -> this.signManagement.handleServiceRemove(event.serviceInfo());
      case RUNNING -> this.signManagement.handleServiceAdd(event.serviceInfo());
      default -> this.signManagement.handleServiceUpdate(event.serviceInfo());
    }
  }
}
