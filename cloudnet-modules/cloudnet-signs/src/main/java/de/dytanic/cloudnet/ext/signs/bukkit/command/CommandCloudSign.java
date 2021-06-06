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

package de.dytanic.cloudnet.ext.signs.bukkit.command;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Arrays;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CommandCloudSign implements CommandExecutor {

  private final BukkitSignManagement bukkitSignManagement;

  public CommandCloudSign(BukkitSignManagement bukkitSignManagement) {
    this.bukkitSignManagement = bukkitSignManagement;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
    @NotNull String[] args) {
    if (!(sender instanceof Player)) {
      return false;
    }

    SignConfigurationEntry entry = this.bukkitSignManagement.getOwnSignConfigurationEntry();

    if (entry == null) {
      return false;
    }

    if (args.length == 0) {
      sender.sendMessage("§7/cloudsign create <targetGroup> [templatePath]");
      sender.sendMessage("§7/cloudsign remove");
      sender.sendMessage("§7/cloudsign cleanup");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
      Block block = player.getTargetBlock((Set<Material>) null, 15);

      if (block.getState() instanceof org.bukkit.block.Sign) {
        for (Sign sign : this.bukkitSignManagement.getSigns()) {
          if (!Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
            .contains(sign.getProvidedGroup())) {
            continue;
          }

          Location location = this.bukkitSignManagement.toLocation(sign.getWorldPosition());

          if (location != null && location.equals(block.getLocation())) {
            this.bukkitSignManagement.sendSignRemoveUpdate(sign);

            org.bukkit.block.Sign blockSign = (org.bukkit.block.Sign) block.getState();
            blockSign.setLine(0, "");
            blockSign.setLine(1, "");
            blockSign.setLine(2, "");
            blockSign.setLine(3, "");
            blockSign.update();

            sender.sendMessage(
              ChatColor.translateAlternateColorCodes('&',
                SignConfigurationProvider.load().getMessages().get("command-cloudsign-remove-success")
              )
            );
            return true;
          }
        }
      }
    }

    if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
      Block block = player.getTargetBlock((Set<Material>) null, 15);

      if (block.getState() instanceof org.bukkit.block.Sign) {
        for (Sign sign : this.bukkitSignManagement.getSigns()) {
          if (!Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
            .contains(sign.getProvidedGroup())) {
            continue;
          }

          Location location = this.bukkitSignManagement.toLocation(sign.getWorldPosition());

          if (location != null && location.equals(block.getLocation())) {
            sender.sendMessage(
              ChatColor.translateAlternateColorCodes('&',
                SignConfigurationProvider.load().getMessages().getOrDefault("command-cloudsign-sign-already-exist",
                  "&7The sign is already set. If you want to remove that, use the /cloudsign remove command")
                  .replace("%group%", sign.getTargetGroup())
              )
            );
            return true;
          }
        }

        Sign sign = new Sign(
          entry.getTargetGroup(),
          args[1],
          new WorldPosition(block.getX(), block.getY(), block.getZ(), 0, 0, block.getWorld().getName()),
          args.length == 3 ? args[2] : null
        );

        this.bukkitSignManagement.sendSignAddUpdate(sign);
        sender.sendMessage(
          ChatColor.translateAlternateColorCodes('&',
            SignConfigurationProvider.load().getMessages().get("command-cloudsign-create-success")
              .replace("%group%", sign.getTargetGroup())
          )
        );
      }
    } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
      this.bukkitSignManagement.cleanup();

      sender.sendMessage(
        ChatColor.translateAlternateColorCodes('&',
          SignConfigurationProvider.load().getMessages().get("command-cloudsign-cleanup-success"))
      );
    }

    return true;
  }
}
