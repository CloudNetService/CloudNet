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

package eu.cloudnetservice.modules.signs.impl.platform.nukkit.functionality;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.platform.nukkit.NukkitSignManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class SignsCommand implements CommandExecutor {

  private final NukkitSignManagement signManagement;

  @Inject
  public SignsCommand(@NonNull NukkitSignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @Override
  @SuppressWarnings("DuplicatedCode") // bukkit is too similar
  public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
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
      var targetBlock = player.getTargetBlock(15);
      var blockEntity = targetBlock.getLevel().getBlockEntity(targetBlock.getLocation());
      if (blockEntity instanceof BlockEntitySign) {
        var pos = this.signManagement.convertPosition(targetBlock.getLocation());
        var sign = this.signManagement.platformSignAt(pos);
        if (sign != null) {
          SignsConfiguration.sendMessage(
            "command-cloudsign-sign-already-exist",
            player::sendMessage,
            m -> m.replace("%group%", sign.base().targetGroup()));
          return true;
        }

        //noinspection ConstantConditions
        this.signManagement.createSign(new Sign(args[1], args.length == 3 ? args[2] : null, pos));
        SignsConfiguration.sendMessage(
          "command-cloudsign-create-success",
          player::sendMessage,
          m -> m.replace("%group%", args[1]));
      } else {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanupall")) {
      var removed = this.signManagement.removeAllMissingSigns();
      SignsConfiguration.sendMessage("command-cloudsign-cleanup-success", player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("cleanup")) {
      var world = args.length == 2 ? args[1] : player.getLocation().getLevel().getName();
      var removed = this.signManagement.removeMissingSigns(world);
      SignsConfiguration.sendMessage("command-cloudsign-cleanup-success", player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("removeall")) {
      var removed = this.signManagement.deleteAllSigns();
      SignsConfiguration.sendMessage("command-cloudsign-bulk-remove-success", player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
      var targetBlock = player.getTargetBlock(15);
      var blockEntity = targetBlock.getLevel().getBlockEntity(targetBlock.getLocation());
      if (blockEntity instanceof BlockEntitySign) {
        var pos = this.signManagement.convertPosition(targetBlock.getLocation());
        var sign = this.signManagement.platformSignAt(pos);
        if (sign == null) {
          SignsConfiguration.sendMessage("command-cloudsign-remove-not-existing", player::sendMessage);
        } else {
          this.signManagement.deleteSign(sign.base());
          SignsConfiguration.sendMessage("command-cloudsign-remove-success", player::sendMessage);
        }
      } else {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    }

    sender.sendMessage("§7/cloudsigns create <targetGroup> [templatePath]");
    sender.sendMessage("§7/cloudsigns remove");
    sender.sendMessage("§7/cloudsigns removeAll");
    sender.sendMessage("§7/cloudsigns cleanup [world]");
    sender.sendMessage("§7/cloudsigns cleanupAll");

    return true;
  }
}
