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

package de.dytanic.cloudnet.ext.bridge.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.bukkit.listener.BukkitCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.bukkit.listener.BukkitPlayerListener;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class BukkitCloudNetBridgePlugin extends JavaPlugin {

  private Supplier<ServerListPingEvent> serverListPingEventConstructor;

  @Override
  public synchronized void onEnable() {
    BukkitCloudNetHelper.init();

    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());

    this.initListeners();

    Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "cloudnet:main");
    Wrapper.getInstance().getTaskExecutor().execute(BridgeHelper::updateServiceInfo);

    this.runFireServerListPingEvent();
  }

  @Override
  public synchronized void onDisable() {
    HandlerList.unregisterAll(this);
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void initListeners() {
    //BukkitAPI
    Bukkit.getServer().getPluginManager().registerEvents(new BukkitPlayerListener(this), this);

    //CloudNet
    CloudNetDriver.getInstance().getEventManager().registerListener(new BukkitCloudNetListener());
    CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
  }

  private void runFireServerListPingEvent() {
    Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
      boolean hasToUpdate = false;
      boolean value = false;

      try {
        ServerListPingEvent serverListPingEvent = this.constructServerListPingEvent();
        if (serverListPingEvent == null) {
          return;
        }

        Bukkit.getPluginManager().callEvent(serverListPingEvent);
        if (!serverListPingEvent.getMotd().equalsIgnoreCase(BridgeServerHelper.getMotd())) {
          hasToUpdate = true;
          BridgeServerHelper.setMotd(serverListPingEvent.getMotd());

          String lowerMotd = serverListPingEvent.getMotd().toLowerCase();
          if (lowerMotd.contains("running") || lowerMotd.contains("ingame") || lowerMotd.contains("playing")) {
            value = true;
          }
        }

        if (serverListPingEvent.getMaxPlayers() != BridgeServerHelper.getMaxPlayers()) {
          hasToUpdate = true;
          BridgeServerHelper.setMaxPlayers(serverListPingEvent.getMaxPlayers());
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
    }, 0, 10);
  }

  private @Nullable ServerListPingEvent constructServerListPingEvent() {
    if (this.serverListPingEventConstructor == null) {
      try {
        // new method in 1.19 and above
        MethodHandle constructor = MethodHandles.publicLookup().findConstructor(
          ServerListPingEvent.class,
          MethodType.methodType(void.class, InetAddress.class, String.class, boolean.class, int.class, int.class));
        this.serverListPingEventConstructor = () -> {
          try {
            return (ServerListPingEvent) constructor.invokeExact(
              new InetSocketAddress("127.0.0.1", 53345).getAddress(),
              BridgeServerHelper.getMotd(),
              false,
              Bukkit.getOnlinePlayers().size(),
              BridgeServerHelper.getMaxPlayers());
          } catch (Throwable throwable) {
            // wait what
            this.getLogger().log(
              Level.SEVERE,
              "Unable to construct ServerListPingEvent using modern constructor; disabling event calling",
              throwable);
            this.serverListPingEventConstructor = () -> null;
            return null;
          }
        };
      } catch (IllegalAccessException exception) {
        // wait what
        this.serverListPingEventConstructor = () -> null;
        this.getLogger().log(
          Level.SEVERE,
          "Unable to access modern constructor of ServerListPingEvent; disabling event calling",
          exception);
      } catch (NoSuchMethodException exception) {
        // old method before 1.18
        this.serverListPingEventConstructor = () -> new ServerListPingEvent(
          new InetSocketAddress("127.0.0.1", 53345).getAddress(),
          BridgeServerHelper.getMotd(),
          Bukkit.getOnlinePlayers().size(),
          BridgeServerHelper.getMaxPlayers());
      }
    }
    return this.serverListPingEventConstructor.get();
  }
}
