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

package de.dytanic.cloudnet.ext.bridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.velocity.command.CommandHub;
import de.dytanic.cloudnet.ext.bridge.velocity.listener.VelocityCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.velocity.listener.VelocityPlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.velocity.listener.VelocityPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Plugin(id = "cloudnet_bridge_velocity")
public final class VelocityCloudNetBridgePlugin {

  private static VelocityCloudNetBridgePlugin instance;

  private final ProxyServer proxyServer;

  @Inject
  public VelocityCloudNetBridgePlugin(ProxyServer proxyServer) {
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());
    instance = this;

    this.proxyServer = proxyServer;
    VelocityCloudNetHelper.setProxyServer(proxyServer);
    VelocityCloudNetHelper.init();
  }

  public static VelocityCloudNetBridgePlugin getInstance() {
    return VelocityCloudNetBridgePlugin.instance;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    this.initListeners();
    this.registerCommands();
    this.initServers();
    this.runPlayerDisconnectTask();
  }

  @Subscribe
  public void handleShutdown(ProxyShutdownEvent event) {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void runPlayerDisconnectTask() {
    this.proxyServer.getScheduler().buildTask(this, () -> {
      if (VelocityCloudNetHelper.getLastOnlineCount() != -1
        && this.proxyServer.getPlayerCount() != VelocityCloudNetHelper.getLastOnlineCount()) {
        Wrapper.getInstance().configureServiceInfoSnapshot().getProperty(BridgeServiceProperty.PLAYERS)
          .ifPresent(players -> {
            boolean needsUpdate = false;
            for (ServicePlayer player : players) {
              if (!this.proxyServer.getPlayer(player.getUniqueId()).isPresent()) {
                needsUpdate = true;

                BridgeHelper.sendChannelMessageMissingDisconnect(player);
                BridgeProxyHelper.clearFallbackProfile(player.getUniqueId());
              }
            }

            if (needsUpdate) {
              BridgeHelper.updateServiceInfo();
            }
          });
      }
    }).repeat(5, TimeUnit.SECONDS).schedule();
  }

  private void initListeners() {
    //Velocity API
    this.proxyServer.getEventManager().register(this, new VelocityPlayerListener(this));

    //CloudNet
    CloudNetDriver.getInstance().getEventManager().registerListener(new VelocityCloudNetListener());
    CloudNetDriver.getInstance().getEventManager()
      .registerListener(new VelocityPlayerExecutorListener(this.proxyServer));
    CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
  }

  private void registerCommands() {
    this.proxyServer.getCommandManager().register("cloudnet", new CommandCloudNet(), "cloud", "cl");

    Collection<String> hubCommandNames = BridgeConfigurationProvider.load().getHubCommandNames();
    if (!hubCommandNames.isEmpty()) {
      String[] aliases = hubCommandNames.toArray(new String[0]);
      if (aliases.length > 1) {
        this.proxyServer.getCommandManager().register(aliases[0], new CommandHub(),
          Arrays.copyOfRange(aliases, 1, aliases.length));
      } else {
        this.proxyServer.getCommandManager().register(aliases[0], new CommandHub());
      }
    }
  }

  private void initServers() {
    for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider()
      .getCloudServices()) {
      if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer()) {
        if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties()
          .getBoolean("Online-Mode")) ||
          serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING) {
          continue;
        }

        String name = serviceInfoSnapshot.getServiceId().getName();
        this.proxyServer.registerServer(new ServerInfo(name, new InetSocketAddress(
          serviceInfoSnapshot.getConnectAddress().getHost(),
          serviceInfoSnapshot.getConnectAddress().getPort()
        )));

        BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
        VelocityCloudNetHelper.addServerToVelocityPrioritySystemConfiguration(serviceInfoSnapshot, name);
      }
    }
  }

  public ProxyServer getProxyServer() {
    return this.proxyServer;
  }

}
