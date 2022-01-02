/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.cloudperms.velocity.listener.VelocityCloudPermissionsPlayerListener;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(
  id = "cloudnet_cloudperms",
  name = "CloudNet-CloudPerms",
  version = "{project.build.version}",
  description = "Velocity extension which implement the permission management system from CloudNet into Velocity",
  url = "https://cloudnetservice.eu",
  authors = "CloudNetService"
)
public final class VelocityCloudNetCloudPermissionsPlugin {

  private final ProxyServer proxyServer;
  private final Logger logger;

  @Inject
  public VelocityCloudNetCloudPermissionsPlugin(ProxyServer proxyServer, Logger logger) {
    this.proxyServer = proxyServer;
    this.logger = logger;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    this.initPlayersPermissionFunction();
    this.proxyServer.getEventManager().register(this, new VelocityCloudPermissionsPlayerListener(
      this.proxyServer,
      new VelocityCloudPermissionProvider(CloudNetDriver.instance().permissionManagement()),
      CloudNetDriver.instance().permissionManagement()));
  }

  @Subscribe
  public void handleShutdown(ProxyShutdownEvent event) {
    CloudNetDriver.instance().eventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.instance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void initPlayersPermissionFunction() {
    this.proxyServer.getAllPlayers().forEach(this::injectPermissionFunction);
  }

  private void injectPermissionFunction(Player player) {
    try {
      var field = player.getClass().getDeclaredField("permissionFunction");
      field.setAccessible(true);
      field.set(
        player,
        new VelocityCloudPermissionFunction(
          player.getUniqueId(),
          CloudNetDriver.instance().permissionManagement()));
    } catch (Exception exception) {
      this.logger.log(Level.SEVERE, "Exception while injecting permissions", exception);
    }
  }
}
