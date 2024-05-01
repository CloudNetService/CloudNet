/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class VelocityCloudPermissionsPlayerListener {

  private final ProxyServer proxyServer;
  private final PermissionProvider permissionProvider;
  private final PermissionManagement permissionsManagement;

  @Inject
  public VelocityCloudPermissionsPlayerListener(
    @NonNull ProxyServer proxyServer,
    @NonNull PermissionProvider permissionProvider,
    @NonNull PermissionManagement permissionsManagement
  ) {
    this.proxyServer = proxyServer;
    this.permissionProvider = permissionProvider;
    this.permissionsManagement = permissionsManagement;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void handle(@NonNull LoginEvent event) {
    CloudPermissionsHelper.initPermissionUser(
      this.permissionsManagement,
      event.getPlayer().getUniqueId(),
      event.getPlayer().getUsername(),
      message -> {
        event.setResult(ResultedEvent.ComponentResult.denied(message));
      },
      this.proxyServer.getConfiguration().isOnlineMode());
  }

  @Subscribe
  public void handle(@NonNull PermissionsSetupEvent event) {
    if (event.getSubject() instanceof Player) {
      event.setProvider(this.permissionProvider);
    }
  }

  @Subscribe
  public void handle(@NonNull DisconnectEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
