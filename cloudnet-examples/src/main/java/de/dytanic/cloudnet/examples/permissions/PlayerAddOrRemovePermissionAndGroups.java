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
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public final class PlayerAddOrRemovePermissionAndGroups {

  public void addPermission(Player player) {
    CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
      permissionUser.addPermission("minecraft.command.gamemode", true); //adds a permission
      permissionUser.addPermission("CityBuild",
        "minecraft.command.difficulty"); //adds a permission which the effect works only on CityBuild group services
    });
  }

  public void removePermission(Player player) {
    CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
      permissionUser.removePermission("minecraft.command.gamemode"); //removes a permission
      permissionUser
        .removePermission("CityBuild", "minecraft.command.difficulty"); //removes a group specific permission
    });
  }

  public void addGroup(Player player) {
    CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
      permissionUser.addGroup("Admin"); //add Admin group
      permissionUser.addGroup("YouTuber", 5, TimeUnit.DAYS); //add YouTuber group for 5 days

      if (permissionUser.inGroup("Admin")) {
        player.sendMessage("Your in group Admin!");
      }
    });
  }

  public void removeGroup(Player player) {
    CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
      permissionUser.removeGroup("YouTuber"); //removes the YouTuber group
    });
  }
}
