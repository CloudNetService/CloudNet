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

package eu.cloudnetservice.cloudnet.ext.signs.nukkit.functionality;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;

public class CommandSigns implements CommandExecutor {

  private final ServiceSignManagement<BlockEntitySign> signManagement;

  public CommandSigns(ServiceSignManagement<BlockEntitySign> signManagement) {
    this.signManagement = signManagement;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only players may execute this command");
      return true;
    }

    Player player = (Player) sender;
    SignConfigurationEntry entry = this.signManagement.getApplicableSignConfigurationEntry();
    if (entry == null) {
      this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-no-entry", sender::sendMessage);
      return true;
    }

    if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("create")) {
      Block targetBlock = player.getTargetBlock(15);
      BlockEntity blockEntity = targetBlock.getLevel().getBlockEntity(targetBlock.getLocation());
      if (blockEntity instanceof BlockEntitySign) {
        Sign sign = this.signManagement.getSignAt((BlockEntitySign) blockEntity);
        if (sign != null) {
          this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-sign-already-exist",
            player::sendMessage, m -> m.replace("%group%", sign.getTargetGroup()));
          return true;
        }

        Sign createdSign = this.signManagement.createSign(
          (BlockEntitySign) blockEntity,
          args[1], args.length == 3 ? args[2] : null
        );
        if (createdSign != null) {
          this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-create-success",
            player::sendMessage, m -> m.replace("%group%", createdSign.getTargetGroup()));
        }
      } else {
        this.signManagement.getSignsConfiguration()
          .sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
      int removed = this.signManagement.removeMissingSigns();
      this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-cleanup-success", player::sendMessage,
        m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("removeall")) {
      int removed = this.signManagement.deleteAllSigns();
      this.signManagement.getSignsConfiguration()
        .sendMessage("command-cloudsign-bulk-remove-success", player::sendMessage,
          m -> m.replace("%amount%", Integer.toString(removed)));
      return true;
    } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
      Block targetBlock = player.getTargetBlock(15);
      BlockEntity blockEntity = targetBlock.getLevel().getBlockEntity(targetBlock.getLocation());
      if (blockEntity instanceof BlockEntitySign) {
        Sign sign = this.signManagement.getSignAt((BlockEntitySign) blockEntity);
        if (sign == null) {
          this.signManagement.getSignsConfiguration()
            .sendMessage("command-cloudsign-remove-not-existing", player::sendMessage);
        } else {
          this.signManagement.deleteSign(sign);
          this.signManagement.getSignsConfiguration()
            .sendMessage("command-cloudsign-remove-success", player::sendMessage);
        }
      } else {
        this.signManagement.getSignsConfiguration()
          .sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }

      return true;
    }

    sender.sendMessage("/signs create <targetGroup> [templatePath]");
    sender.sendMessage("/signs remove");
    sender.sendMessage("/signs removeAll");
    sender.sendMessage("/signs cleanup");

    return true;
  }
}
