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

package eu.cloudnetservice.modules.signs.platform.minestom.functionality;

import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.platform.minestom.MinestomSignManagement;
import eu.cloudnetservice.wrapper.Wrapper;
import lombok.NonNull;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import org.jetbrains.annotations.NotNull;

public class SignsCommand extends Command {

  private static final ArgumentLiteral CREATE_LITERAL = new ArgumentLiteral("create");
  private static final Argument<String> TARGET_GROUP = new ArgumentString("targetGroup") {

    @Override
    public @NotNull String parse(@NotNull String input) throws ArgumentSyntaxException {
      return Wrapper.instance().groupConfigurationProvider().groupConfigurations()
        .stream()
        .filter(group -> group.name().equalsIgnoreCase(input))
        .findFirst()
        .orElseThrow(() -> new ArgumentSyntaxException("Help", input, -1))
        .name();
    }
  };

  private static final Argument<String> TEMPLATE = new ArgumentString("templatePath") {
    @Override
    public @NotNull String parse(@NotNull String input) throws ArgumentSyntaxException {
      var template = ServiceTemplate.parse(input);
      if (template == null) {
        throw new ArgumentSyntaxException("Help", input, -1);
      }

      return template.toString();
    }
  }.setDefaultValue(() -> null);

  private static final ArgumentLiteral REMOVE_LITERAL = new ArgumentLiteral("remove");
  private static final ArgumentLiteral REMOVE_ALL_LITERAL = new ArgumentLiteral("removeAll");
  private static final ArgumentLiteral CLEANUP_LITERAL = new ArgumentLiteral("cleanup");

  private static final CommandCondition DEFAULT_CONDITION = (sender, $) ->
    sender instanceof Player && !(sender instanceof FakePlayer) && sender.hasPermission("cloudnet.command.cloudsign");

  private final MinestomSignManagement signManagement;

  public SignsCommand(
    @NonNull MinestomSignManagement signManagement
  ) {
    super("cloudsign", "cs", "signs", "cloudsigns");
    this.signManagement = signManagement;

    this.addConditionalSyntax(DEFAULT_CONDITION, this::handleCreate, CREATE_LITERAL, TARGET_GROUP, TEMPLATE);
    this.addConditionalSyntax(DEFAULT_CONDITION, this::handleRemove, REMOVE_LITERAL);
    this.addConditionalSyntax(DEFAULT_CONDITION, this::handleRemoveAll, REMOVE_ALL_LITERAL);
    this.addConditionalSyntax(DEFAULT_CONDITION, this::handleCleanup, CLEANUP_LITERAL);
  }

  private void handleCreate(@NonNull CommandSender sender, @NonNull CommandContext context) {
    var entry = this.signManagement.applicableSignConfigurationEntry();
    if (entry == null) {
      SignsConfiguration.sendMessage("command-cloudsign-no-entry", sender::sendMessage);
      return;
    }

    var player = (Player) sender;
    var instance = player.getInstance();
    if (instance != null) {
      var targetPoint = player.getTargetBlockPosition(15);
      var targetBlock = instance.getBlock(targetPoint);

      // check if the player faces a sign
      if (targetBlock.name().contains("sign")) {
        var worldPosition = this.signManagement.convertPosition(targetPoint, instance);
        var sign = this.signManagement.platformSignAt(worldPosition);
        if (sign != null) {
          SignsConfiguration.sendMessage(
            "command-cloudsign-sign-already-exist",
            player::sendMessage, m -> m.replace("%group%", sign.base().targetGroup()));
          return;
        }
        // should never be null here - just for intellij
        if (worldPosition != null) {
          this.signManagement.createSign(new Sign(
            context.get(TARGET_GROUP),
            context.get(TEMPLATE),
            worldPosition
          ));
          SignsConfiguration.sendMessage(
            "command-cloudsign-create-success",
            player::sendMessage, m -> m.replace("%group%", context.get(TARGET_GROUP)));
        }
      } else {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
      }
    }
  }

  private void handleRemove(@NonNull CommandSender sender, @NonNull CommandContext context) {
    var entry = this.signManagement.applicableSignConfigurationEntry();
    if (entry == null) {
      SignsConfiguration.sendMessage("command-cloudsign-no-entry", sender::sendMessage);
      return;
    }

    var player = (Player) sender;
    var instance = player.getInstance();
    if (instance != null) {
      // check if the player is facing a sign
      var targetPoint = player.getTargetBlockPosition(15);
      var targetBlock = instance.getBlock(targetPoint);
      if (targetBlock.name().contains("sign")) {
        // check if the sign exists
        var worldPosition = this.signManagement.convertPosition(targetPoint, instance);
        var sign = this.signManagement.platformSignAt(worldPosition);
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
    }
  }

  private void handleRemoveAll(@NonNull CommandSender sender, @NonNull CommandContext context) {
    // deletes all signs
    var removed = this.signManagement.deleteAllSigns();
    SignsConfiguration.sendMessage(
      "command-cloudsign-bulk-remove-success",
      sender::sendMessage,
      m -> m.replace("%amount%", Integer.toString(removed)));
  }

  private void handleCleanup(@NonNull CommandSender sender, @NonNull CommandContext context) {
    // removes all signs on which location is not a sign anymore
    var removed = this.signManagement.removeMissingSigns();
    SignsConfiguration.sendMessage(
      "command-cloudsign-cleanup-success",
      sender::sendMessage,
      m -> m.replace("%amount%", Integer.toString(removed)));
  }
}
