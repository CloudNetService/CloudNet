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

package eu.cloudnetservice.modules.signs.impl.platform;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class SignsPlatformListener {

  @EventListener
  public void handle(@NonNull CloudServiceUpdateEvent event, @NonNull PlatformSignManagement signManagement) {
    signManagement.handleServiceUpdate(event.serviceInfo());
  }

  @EventListener
  public void handle(@NonNull CloudServiceLifecycleChangeEvent event, @NonNull PlatformSignManagement signManagement) {
    switch (event.newLifeCycle()) {
      case STOPPED, DELETED -> signManagement.handleServiceRemove(event.serviceInfo());
      case RUNNING -> signManagement.handleServiceAdd(event.serviceInfo());
      default -> signManagement.handleServiceUpdate(event.serviceInfo());
    }
  }
}
