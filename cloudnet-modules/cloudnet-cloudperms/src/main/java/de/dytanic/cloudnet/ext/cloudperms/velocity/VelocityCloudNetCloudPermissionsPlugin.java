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

package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.velocity.listener.VelocityCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Field;

@Plugin(id = "cloudnet_cloudperms_velocity")
public final class VelocityCloudNetCloudPermissionsPlugin {

  private final ProxyServer proxyServer;
  private PermissionProvider permissionProvider;

  @Inject
  public VelocityCloudNetCloudPermissionsPlugin(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    this.permissionProvider = new VelocityCloudNetCloudPermissionsPermissionProvider(
      CloudNetDriver.getInstance().getPermissionManagement());

    this.initPlayersPermissionFunction();
    this.proxyServer.getEventManager().register(this, new VelocityCloudNetCloudPermissionsPlayerListener(
      CloudNetDriver.getInstance().getPermissionManagement(),
      this.permissionProvider
    ));
  }

  @Subscribe
  public void handleShutdown(ProxyShutdownEvent event) {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }


  private void initPlayersPermissionFunction() {
    this.proxyServer.getAllPlayers().forEach(this::injectPermissionFunction);
  }

  public void injectPermissionFunction(Player player) {
    Preconditions.checkNotNull(player);

    try {
      Field field = player.getClass().getDeclaredField("permissionFunction");
      field.setAccessible(true);
      field.set(player, new VelocityCloudNetCloudPermissionsPermissionFunction(player.getUniqueId(),
        CloudNetDriver.getInstance().getPermissionManagement()));
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  public PermissionProvider getPermissionProvider() {
    return this.permissionProvider;
  }

}
