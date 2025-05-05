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

package eu.cloudnetservice.modules.npc.impl.platform;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import lombok.NonNull;

final class CloudNetServiceListener {

  private final PlatformNPCManagement<?, ?, ?, ?, ?> management;

  public CloudNetServiceListener(@NonNull PlatformNPCManagement<?, ?, ?, ?, ?> management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NonNull CloudServiceLifecycleChangeEvent event) {
    switch (event.newLifeCycle()) {
      case RUNNING -> this.management.handleServiceUpdate(event.serviceInfo());
      case STOPPED, DELETED -> this.management.handleServiceRemove(event.serviceInfo());
      default -> {
      }
    }
  }

  @EventListener
  public void handle(@NonNull CloudServiceUpdateEvent event) {
    if (event.serviceInfo().lifeCycle() == ServiceLifeCycle.RUNNING) {
      this.management.handleServiceUpdate(event.serviceInfo());
    }
  }
}
