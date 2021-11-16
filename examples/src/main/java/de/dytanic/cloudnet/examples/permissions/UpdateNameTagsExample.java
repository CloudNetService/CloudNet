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

package de.dytanic.cloudnet.examples.permissions;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.simplenametags.CloudNetSimpleNameTagsPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNameTagsExample {

  private final Collection<UUID> nickedPlayers = new ArrayList<>();

  @EventHandler
  public void executeBukkitExampleOnPlayerJoinEvent(PlayerJoinEvent event) {
    CloudNetSimpleNameTagsPlugin.getInstance().updateNameTags(event.getPlayer());
    //Sets the nametags and don't overwrite the scoreboard rather the scoreboard will updated
  }

  @EventListener(priority = EventPriority.LOWEST)
  public void handle(PermissionUpdateUserEvent event) { //Live update of permission users
    Player player = Bukkit.getPlayer(event.getPermissionUser().getUniqueId());

    if (player != null) {
      CloudNetSimpleNameTagsPlugin.getInstance().updateNameTags(player);
    }
  }

  //For developers with a NickAPI or something like this
  public void nickExample(Player player) {
    CloudNetSimpleNameTagsPlugin.getInstance().updateNameTags(player, player1 -> {
      if (this.isNicked(player1)) {
        return CloudNetDriver.getInstance().getPermissionManagement().getGroup("Default");
      }

      IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement()
        .getUser(player1.getUniqueId());

      return permissionUser == null ? null
        : CloudNetDriver.getInstance().getPermissionManagement().getHighestPermissionGroup(permissionUser);
    });
  }

  public boolean isNicked(Player player) {
    return this.nickedPlayers.contains(player.getUniqueId());
  }
}
