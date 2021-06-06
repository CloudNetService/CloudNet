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

package de.dytanic.cloudnet.examples.bridge;

import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * The Bridge Methods are only available when the bridge plugin is enabled on a service
 */
public final class BukkitSpongeNukkitBridgeExample {

  public void changeMotd() {
    BridgeServerHelper.setMotd("My new Motd"); //Change the motd string, which will be included with the next updates
    BridgeServerHelper.updateServiceInfo();
  }

  public void changeMaxPlayers() {
    BridgeServerHelper.setMaxPlayers(16); //Changed the displayed player limit
    BridgeServerHelper.updateServiceInfo();
  }

  public void changeState() {
    BridgeServerHelper.setState(
      "PREMIUM"); //Sets the server state for this instance. Text like "LOBBY", "FULL", "INGAME" which other services indicates, what state the service has
    BridgeServerHelper
      .setExtra("Waterfall"); //Extra is an custom string, which can displayed on signs or as communication string
    Wrapper.getInstance().publishServiceInfoUpdate(); //Original alternative to XXXCloudNetHelper.updateServiceInfo()
  }

  public void changeToIngame() {
    BridgeServerHelper.setMotd(
      "INGAME"); //if the motd, state or extra is "ingame", "running" or "playing", the default modules indicate as service which should be hide.
    BridgeServerHelper.setState("running");
    BridgeServerHelper.setExtra("playing");
    BridgeServerHelper.updateServiceInfo();
  }

  public void changeToIngameShortCut() {
    BridgeServerHelper.changeToIngame(
      true); //Set the state to "INGAME", send a ServiceInfoSnapshot update and try to start a new service of the task of this service
  }
}
