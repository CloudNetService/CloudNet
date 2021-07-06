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
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import java.util.UUID;

public final class ServiceInfoSnapshotUtilExample {

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    int onlineCount = event.getServiceInfo().getProperty(BridgeServiceProperty.ONLINE_COUNT)
      .orElse(0); //The online players count

    int maxPlayers = event.getServiceInfo().getProperty(BridgeServiceProperty.MAX_PLAYERS)
      .orElse(0); //The API set PlayerLimit

    String version = event.getServiceInfo().getProperty(BridgeServiceProperty.VERSION)
      .orElse(null); //The version of the service

    String motd = event.getServiceInfo().getProperty(BridgeServiceProperty.MOTD).orElse(null); //State motd or null

    String extra = event.getServiceInfo().getProperty(BridgeServiceProperty.EXTRA).orElse(null); //Extra string or null

    String state = event.getServiceInfo().getProperty(BridgeServiceProperty.STATE).orElse(null); //State string or null

    event.getServiceInfo().getProperty(BridgeServiceProperty.PLUGINS)
      .ifPresent(pluginInfos -> { //The pluginInfo items with the important information about the plugin
        for (PluginInfo pluginInfo : pluginInfos) {
          String pluginInfoName = pluginInfo.getName();
          String pluginInfoVersion = pluginInfo.getVersion();
          JsonDocument subProperties = pluginInfo.getProperties();
        }
      });

    event.getServiceInfo().getProperty(BridgeServiceProperty.PLAYERS).ifPresent(players -> {
      for (ServicePlayer player : players) {
        UUID uniqueId = player.getUniqueId();
        String name = player.getName();
      }
    });

  }
}
