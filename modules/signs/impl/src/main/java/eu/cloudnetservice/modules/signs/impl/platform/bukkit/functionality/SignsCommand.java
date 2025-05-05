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

package eu.cloudnetservice.modules.signs.impl.platform.bukkit.functionality;

import com.google.common.collect.ImmutableList;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.ext.bukkitcommands.BaseTabExecutor;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSignManagement;
import eu.cloudnetservice.modules.signs.impl.platform.bukkit.BukkitSignManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class SignsCommand extends BaseTabExecutor {

  private final GroupConfigurationProvider groupProvider;
  private final PlatformSignManagement<?, Location, ?> signManagement;

  @Inject
  public SignsCommand(@NonNull GroupConfigurationProvider groupProvider, @NonNull BukkitSignManagement signManagement) {
    this.groupProvider = groupProvider;
    this.signManagement = signManagement;
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // nukkit is too similar
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players may execute this command");
      return true;
    }

    var entry = this.signManagement.applicableSignConfigurationEntry();
    if (entry == null) {
      SignsConfiguration.sendMessage("command-cloudsign-no-entry", sender::sendMessage);
      return true;
    }

    if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("create")) {
      var targetBlock = player.getTargetBlock((Set<Material>) null, 15);
      // check if the block the player is facing is a sign
      if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
        // validate that the sign isn't existing already
        var loc = this.signManagement.convertPosition(targetBlock.getLocation());
        var sign = this.signManagement.platformSignAt(loc);
        if (sign != null) {
          SignsConfiguration.sendMessage(
            "command-cloudsign-sign-already-exist",
            player::sendMessage, m -> m.replace("%group%", sign.base().targetGroup()));
          return true;
        }

        // create the sign
        //noinspection ConstantConditions
        this.signManagement.createSign(new Sign(
          args[1],
          args.length == 3 ? args[2] : null,
          loc));
        SignsConfiguration.sendMessage(
          "command-cloudsign-create-success",
          player::sendMessage, m -> m.replace("%group%", args[1]));
      } else {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanupall")) {
      // removes all signs on which location is not a sign anymore
      var removed = this.signManagement.removeAllMissingSigns();
      SignsConfiguration.sendMessage(
        "command-cloudsign-cleanup-success",
        player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("cleanup")) {
      var world = args.length == 2 ? args[1] : player.getWorld().getName();
      // removes all signs on which location is not a sign anymore
      var removed = this.signManagement.removeMissingSigns(world);
      SignsConfiguration.sendMessage(
        "command-cloudsign-cleanup-success",
        player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("removeall")) {
      // deletes all signs
      var removed = this.signManagement.deleteAllSigns();
      SignsConfiguration.sendMessage(
        "command-cloudsign-bulk-remove-success",
        player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
      // check if the player is facing a sign
      var targetBlock = player.getTargetBlock((Set<Material>) null, 15);
      if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
        // check if the sign exists
        var loc = this.signManagement.convertPosition(targetBlock.getLocation());
        var sign = this.signManagement.platformSignAt(loc);
        if (sign == null) {
          SignsConfiguration.sendMessage(
            "command-cloudsign-remove-not-existing",
            player::sendMessage);
        } else {
          // remove the sign
          this.signManagement.deleteSign(sign.base());
          SignsConfiguration.sendMessage(
            "command-cloudsign-remove-success",
            player::sendMessage);
        }
      } else {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    }

    // unknown command
    sender.sendMessage("§7/cloudsigns create <targetGroup> [templatePath]");
    sender.sendMessage("§7/cloudsigns remove");
    sender.sendMessage("§7/cloudsigns removeAll");
    sender.sendMessage("§7/cloudsigns cleanup [world]");
    sender.sendMessage("§7/cloudsigns cleanupAll");
    return true;
  }

  @Override
  public @NonNull Collection<String> tabComplete(@NonNull CommandSender sender, String @NonNull [] args) {
    if (args.length == 1) {
      // filter for all strings that partially match the input of the player
      return Arrays.asList("create", "remove", "removeall", "cleanup");
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
      return this.groupProvider.groupConfigurations().stream()
        .map(GroupConfiguration::name)
        .toList();
    }
    // unable to tab complete
    return ImmutableList.of();
  }
}
