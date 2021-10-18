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

package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.ChatColor;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.EventPriority;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import io.gomint.server.GoMintServer;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

  private final IPermissionManagement permissionsManagement;

  public GoMintCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void handle(PlayerLoginEvent event) {
    if (!event.cancelled()) {
      EntityPlayer player = event.player();

      CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, player.uuid(), player.name(), message -> {
        event.cancelled(true);
        event.kickMessage(ChatColor.translateAlternateColorCodes('&', message));
      }, ((GoMintServer) GoMint.instance()).encryptionKeyFactory().isKeyGiven());

      GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(player);
    }
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.player().uuid());
  }
}
