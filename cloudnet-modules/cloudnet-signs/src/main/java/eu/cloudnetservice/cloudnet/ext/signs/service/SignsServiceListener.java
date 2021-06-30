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

package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;

public class SignsServiceListener {

  protected final ServiceSignManagement<?> signManagement;

  public SignsServiceListener(ServiceSignManagement<?> signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    this.signManagement.handleServiceAdd(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    this.signManagement.handleServiceRemove(event.getServiceInfo());
  }
}
