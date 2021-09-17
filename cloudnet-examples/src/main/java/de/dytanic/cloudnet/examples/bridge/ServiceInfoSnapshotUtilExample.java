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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import java.util.UUID;

public final class ServiceInfoSnapshotUtilExample {

  public void serviceInfoProperties() {
    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider()
      .getCloudServiceByName("Lobby-187");

    if (serviceInfoSnapshot == null) {
      // there is no service with the name "Lobby-187"
      return;
    }

    int onlineCount = serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT)
      .orElse(0); //The online players count

    int maxPlayers = serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS)
      .orElse(0); //The API set PlayerLimit

    String version = serviceInfoSnapshot.getProperty(BridgeServiceProperty.VERSION)
      .orElse(null); //The version of the service

    String motd = serviceInfoSnapshot.getProperty(BridgeServiceProperty.MOTD).orElse(null); //State motd or null

    String extra = serviceInfoSnapshot.getProperty(BridgeServiceProperty.EXTRA).orElse(null); //Extra string or null

    String state = serviceInfoSnapshot.getProperty(BridgeServiceProperty.STATE).orElse(null); //State string or null

    boolean isIngame = serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME)
      .orElse(false); //true if ingame, false otherwise

    serviceInfoSnapshot.getProperty(BridgeServiceProperty.PLUGINS)
      .ifPresent(pluginInfos -> { //The pluginInfo items with the important information about the plugin
        for (PluginInfo pluginInfo : pluginInfos) {
          String pluginInfoName = pluginInfo.getName();
          String pluginInfoVersion = pluginInfo.getVersion();
          JsonDocument subProperties = pluginInfo.getProperties();
        }
      });

    serviceInfoSnapshot.getProperty(BridgeServiceProperty.PLAYERS).ifPresent(players -> {
      for (ServicePlayer player : players) {
        UUID uniqueId = player.getUniqueId();
        String name = player.getName();
      }
    });
  }
}
