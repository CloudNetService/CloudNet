/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.platform.sponge.functionality;

import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.server.ServerLocation;

public class SignsCommand implements CommandExecutor {

  public static final Key<String> ACTION = Parameter.key("action", String.class);
  public static final Key<String> TARGET_GROUP = Parameter.key("target_group", String.class);
  public static final Key<String> TARGET_TEMPLATE = Parameter.key("target_template_path", String.class);

  protected final Supplier<PlatformSignManagement<?, ServerLocation, ?>> signManagement;

  public SignsCommand(@NonNull Supplier<PlatformSignManagement<?, ServerLocation, ?>> signManagement) {
    this.signManagement = signManagement;
  }

  @Override
  public CommandResult execute(@NonNull CommandContext context) {
    if (!(context.subject() instanceof ServerPlayer player)) {
      context.sendMessage(Identity.nil(), Component.text("Only players may execute this command"));
      return CommandResult.success();
    }

    var entry = this.signManagement.get().applicableSignConfigurationEntry();
    if (entry == null) {
      SignsConfiguration.sendMessage(
        "command-cloudsign-no-entry",
        message -> player.sendMessage(Component.text(message)));
      return CommandResult.success();
    }

    var type = context.one(ACTION).orElse(null);
    var targetGroup = context.one(TARGET_GROUP).orElse(null);
    var targetTemplatePath = context.one(TARGET_TEMPLATE).orElse(null);

    if (type != null) {
      if (type.equalsIgnoreCase("create") && targetGroup != null) {
        var hit = this.getTargetBlock(player);
        if (hit.isEmpty()) {
          return CommandResult.success();
        }

        var loc = this.signManagement.get().convertPosition(hit.get().serverLocation());
        var sign = this.signManagement.get().platformSignAt(loc);
        if (sign != null) {
          SignsConfiguration.sendMessage(
            "command-cloudsign-sign-already-exist",
            message -> player.sendMessage(Component.text(message)));
        } else {
          //noinspection ConstantConditions
          this.signManagement.get().createSign(new eu.cloudnetservice.modules.signs.Sign(
            targetGroup,
            targetTemplatePath,
            loc));
          SignsConfiguration.sendMessage(
            "command-cloudsign-create-success",
            m -> player.sendMessage(Component.text(m)),
            m -> m.replace("%group%", targetGroup));
        }

        return CommandResult.success();
      } else if (type.equalsIgnoreCase("cleanup")) {
        var removed = this.signManagement.get().removeMissingSigns();
        SignsConfiguration.sendMessage(
          "command-cloudsign-cleanup-success",
          m -> player.sendMessage(Component.text(m)),
          m -> m.replace("%amount%", Integer.toString(removed)));
        return CommandResult.success();
      } else if (type.equalsIgnoreCase("removeall")) {
        var removed = this.signManagement.get().deleteAllSigns();
        SignsConfiguration.sendMessage(
          "command-cloudsign-bulk-remove-success",
          m -> player.sendMessage(Component.text(m)),
          m -> m.replace("%amount%", Integer.toString(removed)));
        return CommandResult.success();
      } else if (type.equalsIgnoreCase("remove")) {
        var hit = this.getTargetBlock(player);
        if (hit.isEmpty()) {
          return CommandResult.success();
        }

        var loc = this.signManagement.get().convertPosition(hit.get().serverLocation());
        var sign = this.signManagement.get().platformSignAt(loc);
        if (sign != null && loc != null) {
          this.signManagement.get().deleteSign(loc);
          SignsConfiguration.sendMessage(
            "command-cloudsign-remove-success",
            m -> player.sendMessage(Component.text(m)));
        } else {
          SignsConfiguration.sendMessage(
            "command-cloudsign-remove-not-existing",
            m -> player.sendMessage(Component.text(m)));
        }

        return CommandResult.success();
      }
    }

    context.sendMessage(Identity.nil(), Component.text("§7/cloudsigns create <targetGroup> [templatePath]"));
    context.sendMessage(Identity.nil(), Component.text("§7/cloudsigns remove"));
    context.sendMessage(Identity.nil(), Component.text("§7/cloudsigns removeAll"));
    context.sendMessage(Identity.nil(), Component.text("§7/cloudsigns cleanup"));

    return CommandResult.success();
  }

  protected @NonNull Optional<Sign> getTargetBlock(@NonNull ServerPlayer player) {
    var result = RayTrace.block()
      .limit(15)
      .world(player.world())
      .sourceEyePosition(player)
      .direction(player.direction())
      .select(block -> block.blockState().type().key(RegistryTypes.BLOCK_TYPE).formatted().endsWith("_sign"))
      .execute()
      .flatMap(hit -> hit.selectedObject().location().blockEntity())
      .map(entity -> entity instanceof Sign sign ? sign : null);
    // check if the player is facing a sign
    if (result.isEmpty()) {
      SignsConfiguration.sendMessage(
        "command-cloudsign-not-looking-at-sign",
        message -> player.sendMessage(Component.text(message)));
      return Optional.empty();
    }
    return result;
  }
}
