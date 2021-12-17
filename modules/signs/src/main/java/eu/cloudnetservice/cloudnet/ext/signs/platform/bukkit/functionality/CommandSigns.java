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

package eu.cloudnetservice.cloudnet.ext.signs.platform.bukkit.functionality;

import com.google.common.collect.ImmutableList;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.bukkitcommands.BaseTabExecutor;
import eu.cloudnetservice.cloudnet.ext.signs.platform.PlatformSignManagement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandSigns extends BaseTabExecutor {

  protected final PlatformSignManagement<org.bukkit.block.Sign> signManagement;

  public CommandSigns(PlatformSignManagement<org.bukkit.block.Sign> signManagement) {
    this.signManagement = signManagement;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players may execute this command");
      return true;
    }

    var entry = this.signManagement.getApplicableSignConfigurationEntry();
    if (entry == null) {
      this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-no-entry", sender::sendMessage);
      return true;
    }

    if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("create")) {
      var targetBlock = player.getTargetBlock((Set<Material>) null, 15);
      // check if the block the player is facing is a sign
      if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
        // validate that the sign isn't existing already
        var sign = this.signManagement.getSignAt((org.bukkit.block.Sign) targetBlock.getState());
        if (sign != null) {
          this.signManagement.getSignsConfiguration().sendMessage(
            "command-cloudsign-sign-already-exist",
            player::sendMessage, m -> m.replace("%group%", sign.getTargetGroup()));
          return true;
        }

        // create the sign
        var createdSign = this.signManagement.createSign(
          (org.bukkit.block.Sign) targetBlock.getState(),
          args[1],
          args.length == 3 ? args[2] : null);
        if (createdSign != null) {
          // success
          this.signManagement.getSignsConfiguration().sendMessage(
            "command-cloudsign-create-success",
            player::sendMessage, m -> m.replace("%group%", createdSign.getTargetGroup()));
        }
      } else {
        this.signManagement.getSignsConfiguration()
          .sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
      // removes all signs on which location is not a sign anymore
      var removed = this.signManagement.removeMissingSigns();
      this.signManagement.getSignsConfiguration().sendMessage(
        "command-cloudsign-cleanup-success",
        player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("removeall")) {
      // deletes all signs
      var removed = this.signManagement.deleteAllSigns();
      this.signManagement.getSignsConfiguration().sendMessage(
        "command-cloudsign-bulk-remove-success",
        player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
      // check if the player is facing a sign
      var targetBlock = player.getTargetBlock((Set<Material>) null, 15);
      if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
        // check if the sign exists
        var sign = this.signManagement.getSignAt((org.bukkit.block.Sign) targetBlock.getState());
        if (sign == null) {
          this.signManagement.getSignsConfiguration().sendMessage(
            "command-cloudsign-remove-not-existing",
            player::sendMessage);
        } else {
          // remove the sign
          this.signManagement.deleteSign(sign);
          this.signManagement.getSignsConfiguration().sendMessage(
            "command-cloudsign-remove-success",
            player::sendMessage);
        }
      } else {
        this.signManagement.getSignsConfiguration()
          .sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    }
    // unknown command
    sender.sendMessage("§7/cloudsigns create <targetGroup> [templatePath]");
    sender.sendMessage("§7/cloudsigns remove");
    sender.sendMessage("§7/cloudsigns removeAll");
    sender.sendMessage("§7/cloudsigns cleanup");
    return true;
  }

  @Override
  public @NotNull Collection<String> tabComplete(@NotNull CommandSender sender, String @NotNull [] args) {
    if (args.length == 1) {
      // filter for all strings that partially match the input of the player
      return Arrays.asList("create", "remove", "removeall", "cleanup");
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
      return CloudNetDriver.instance().groupConfigurationProvider().groupConfigurations().stream()
        .map(GroupConfiguration::name)
        .toList();
    }
    // unable to tab complete
    return ImmutableList.of();
  }
}
