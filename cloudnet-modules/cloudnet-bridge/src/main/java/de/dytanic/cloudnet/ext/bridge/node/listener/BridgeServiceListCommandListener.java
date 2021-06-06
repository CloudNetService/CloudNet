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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.ServiceListCommandEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;

public class BridgeServiceListCommandListener {

  @EventListener
  public void handleCommand(ServiceListCommandEvent event) {
    event.addParameter(
      serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false) ?
        "Players: " + serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0) + "/"
          + serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0) :
        null);
    event.addParameter(
      serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false) ? "Ingame"
        : null);

    long onlineServices = event.getTargetServiceInfoSnapshots().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false))
      .count();
    long inGameServices = event.getTargetServiceInfoSnapshots().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false))
      .count();

    event.addSummaryParameter("Online: " + onlineServices);
    event.addSummaryParameter("Ingame: " + inGameServices);
  }

}
