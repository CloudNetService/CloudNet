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

package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

  private final IPermissionManagement permissionsManagement;
  private final PermissionProvider permissionProvider;

  public VelocityCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement,
    PermissionProvider permissionProvider) {
    this.permissionsManagement = permissionsManagement;
    this.permissionProvider = permissionProvider;
  }

  @Subscribe(order = PostOrder.LAST)
  public void handle(LoginEvent event) {
    if (event.getResult().isAllowed()) {
      Player player = event.getPlayer();
      CloudPermissionsHelper
        .initPermissionUser(this.permissionsManagement, player.getUniqueId(), player.getUsername(), message -> {
          Component reasonComponent = LegacyComponentSerializer.legacySection().deserialize(message.replace("&", "§"));
          event.setResult(ResultedEvent.ComponentResult.denied(reasonComponent));
        });
    }
  }

  @Subscribe
  public void handle(PermissionsSetupEvent event) {
    if (event.getSubject() instanceof Player) {
      event.setProvider(this.permissionProvider);
    }
  }

  @Subscribe
  public void handle(DisconnectEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
