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

package eu.cloudnetservice.cloudnet.ext.signs.sponge.functionality;

import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;

public class CommandSigns implements CommandExecutor {

  protected final ServiceSignManagement<org.spongepowered.api.block.tileentity.Sign> signManagement;

  public CommandSigns(ServiceSignManagement<org.spongepowered.api.block.tileentity.Sign> signManagement) {
    this.signManagement = signManagement;
  }

  @Override
  public @NotNull CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) {
    if (!(src instanceof Player)) {
      src.sendMessage(Text.of("Only players may execute this command"));
      return CommandResult.success();
    }

    Player player = (Player) src;
    String type = args.<String>getOne("Type").orElse(null);
    String targetGroup = args.<String>getOne("Target Group").orElse(null);
    String targetTemplatePath = args.<String>getOne("Template Path").orElse(null);

    if (type != null) {
      if (type.equalsIgnoreCase("create") && targetGroup != null) {
        Optional<BlockRayHit<World>> hit = this.getTargetBlock(player);
        if (!hit.isPresent()) {
          return CommandResult.success();
        }

        Sign sign = this.signManagement
          .getSignAt((org.spongepowered.api.block.tileentity.Sign) hit.get().getLocation().getTileEntity().get());
        if (sign != null) {
          this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-sign-already-exist",
            message -> player.sendMessage(Text.of(message)));
        } else {
          Sign createdSign = this.signManagement
            .createSign((org.spongepowered.api.block.tileentity.Sign) hit.get().getLocation().getTileEntity().get(),
              targetGroup, targetTemplatePath);
          if (createdSign != null) {
            this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-create-success",
              m -> player.sendMessage(Text.of(m)), m -> m.replace("%group%", createdSign.getTargetGroup()));
          }
        }

        return CommandResult.success();
      } else if (type.equalsIgnoreCase("cleanup")) {
        int removed = this.signManagement.removeMissingSigns();
        this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-cleanup-success",
          m -> player.sendMessage(Text.of(m)), m -> m.replace("%amount%", Integer.toString(removed)));
        return CommandResult.success();
      } else if (type.equalsIgnoreCase("removeall")) {
        int removed = this.signManagement.deleteAllSigns();
        this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-bulk-remove-success",
          m -> player.sendMessage(Text.of(m)), m -> m.replace("%amount%", Integer.toString(removed)));
        return CommandResult.success();
      } else if (type.equalsIgnoreCase("remove")) {
        Optional<BlockRayHit<World>> hit = this.getTargetBlock(player);
        if (!hit.isPresent()) {
          return CommandResult.success();
        }

        Sign sign = this.signManagement
          .getSignAt((org.spongepowered.api.block.tileentity.Sign) hit.get().getLocation().getTileEntity().get());
        if (sign != null) {
          this.signManagement.deleteSign(sign);
          this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-remove-success",
            m -> player.sendMessage(Text.of(m)));
        } else {
          this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-remove-not-existing",
            m -> player.sendMessage(Text.of(m)));
        }

        return CommandResult.success();
      }
    }

    src.sendMessage(Text.of("§7/cloudsigns create <targetGroup> [templatePath]"));
    src.sendMessage(Text.of("§7/cloudsigns remove"));
    src.sendMessage(Text.of("§7/cloudsigns removeAll"));
    src.sendMessage(Text.of("§7/cloudsigns cleanup"));

    return CommandResult.success();
  }

  protected @NotNull Optional<BlockRayHit<World>> getTargetBlock(@NotNull Player player) {
    Optional<BlockRayHit<World>> hit = BlockRay.from(player)
      .distanceLimit(15)
      .narrowPhase(true)
      .select(lastHit -> {
        BlockType blockType = lastHit.getExtent()
          .getBlockType(lastHit.getBlockX(), lastHit.getBlockY(), lastHit.getBlockZ());
        return blockType.equals(BlockTypes.STANDING_SIGN) || blockType.equals(BlockTypes.WALL_SIGN);
      })
      .end();
    if (!hit.isPresent() || !(hit.get().getLocation().getTileEntity()
      .orElse(null) instanceof org.spongepowered.api.block.tileentity.Sign)) {
      this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-not-looking-at-sign",
        message -> player.sendMessage(Text.of(message)));
      return Optional.empty();
    }
    return hit;
  }
}
