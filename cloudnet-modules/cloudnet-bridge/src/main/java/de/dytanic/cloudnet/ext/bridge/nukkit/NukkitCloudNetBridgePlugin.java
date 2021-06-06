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

package de.dytanic.cloudnet.ext.bridge.nukkit;

import cn.nukkit.Server;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.server.QueryRegenerateEvent;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.nukkit.listener.NukkitCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.nukkit.listener.NukkitPlayerListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class NukkitCloudNetBridgePlugin extends PluginBase {

  @Override
  public synchronized void onEnable() {
    NukkitCloudNetHelper.init();

    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());
    this.initListeners();

    Wrapper.getInstance().getTaskExecutor().execute(
      BridgeHelper::updateServiceInfo); //However, calling this method in the scheduler fixes a NullPointerException in NukkitCloudNetHelper.initProperties(ServiceInfoSnapshot)
    this.runFireServerListPingEvent();
  }

  @Override
  public synchronized void onDisable() {
    HandlerList.unregisterAll(this);
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void initListeners() {
    //NukkitAPI
    Server.getInstance().getPluginManager().registerEvents(new NukkitPlayerListener(), this);

    //CloudNet
    CloudNetDriver.getInstance().getEventManager().registerListener(new NukkitCloudNetListener());
    CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
  }

  private void runFireServerListPingEvent() {
    Server.getInstance().getScheduler().scheduleRepeatingTask(this, () -> {
      boolean hasToUpdate = false;
      boolean value = false;

      try {
        QueryRegenerateEvent event = new QueryRegenerateEvent(Server.getInstance());
        Server.getInstance().getPluginManager().callEvent(event);

        if (!event.getServerName().equalsIgnoreCase(BridgeServerHelper.getMotd())) {
          hasToUpdate = true;
          BridgeServerHelper.setMotd(event.getServerName());

          String lowerMotd = event.getServerName().toLowerCase();
          if (lowerMotd.contains("running") || lowerMotd.contains("ingame") || lowerMotd.contains("playing")) {
            value = true;
          }
        }

        if (event.getMaxPlayerCount() != BridgeServerHelper.getMaxPlayers()) {
          hasToUpdate = true;
          BridgeServerHelper.setMaxPlayers(event.getMaxPlayerCount());
        }

        if (value) {
          BridgeServerHelper.changeToIngame(true);
          return;
        }

        if (hasToUpdate) {
          BridgeHelper.updateServiceInfo();
        }
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }, 10, true);
  }
}
