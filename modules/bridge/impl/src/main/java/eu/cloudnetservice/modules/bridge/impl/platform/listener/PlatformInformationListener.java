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

package eu.cloudnetservice.modules.bridge.impl.platform.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.event.events.task.ServiceTaskAddEvent;
import eu.cloudnetservice.driver.event.events.task.ServiceTaskRemoveEvent;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import lombok.NonNull;

public final class PlatformInformationListener {

  private final PlatformBridgeManagement<?, ?> management;

  public PlatformInformationListener(@NonNull PlatformBridgeManagement<?, ?> management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NonNull ServiceInfoPropertiesConfigureEvent event) {
    this.management.appendServiceInformation(event);
  }

  @EventListener
  public void handleLifecycleChange(@NonNull CloudServiceLifecycleChangeEvent event) {
    this.management.handleServiceUpdate(event.serviceInfo());
  }

  @EventListener
  public void handleLifecycleChange(@NonNull CloudServiceUpdateEvent event) {
    this.management.handleServiceUpdate(event.serviceInfo());
  }

  @EventListener
  public void handleServiceTaskAdd(@NonNull ServiceTaskAddEvent event) {
    this.management.handleTaskUpdate(event.task().name(), event.task());
  }

  @EventListener
  public void handleServiceTaskRemove(@NonNull ServiceTaskRemoveEvent event) {
    this.management.handleTaskUpdate(event.task().name(), null);
  }
}
