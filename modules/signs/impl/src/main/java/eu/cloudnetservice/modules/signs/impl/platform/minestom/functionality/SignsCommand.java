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

package eu.cloudnetservice.modules.signs.impl.platform.minestom.functionality;

import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomBridgeManagement;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.platform.minestom.MinestomSignManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
public class SignsCommand extends Command {

  private static final ArgumentLiteral REMOVE_LITERAL = new ArgumentLiteral("remove");
  private static final ArgumentLiteral REMOVE_ALL_LITERAL = new ArgumentLiteral("removeAll");
  private static final ArgumentLiteral CLEANUP_LITERAL = new ArgumentLiteral("cleanup");
  private static final ArgumentLiteral CLEANUP_ALL_LITERAL = new ArgumentLiteral("cleanupAll");

  private final Argument<String> world;
  private final Argument<String> template;
  private final Argument<String> targetGroup;
  private final MinestomSignManagement signManagement;

  @Inject
  public SignsCommand(
    @NonNull @Service I18n i18n,
    @NonNull MinestomSignManagement signManagement,
    @NonNull GroupConfigurationProvider groupProvider,
    @NonNull MinestomBridgeManagement bridgeManagement
  ) {
    super("cloudsign", "cs", "signs", "cloudsigns");

    this.world = new ArgumentString("world");
    this.template = this.createTemplatePathArgument(i18n);
    this.targetGroup = this.createTargetGroupArgument(i18n, groupProvider);
    this.signManagement = signManagement;

    var createLiteral = new ArgumentLiteral("create");

    CommandCondition defaultCondition = (sender, _) -> sender instanceof Player player
      && bridgeManagement.permissionFunction().apply(player, "cloudnet.command.cloudsign");

    this.addConditionalSyntax(defaultCondition, this::handleCreate, createLiteral, this.targetGroup, this.template);
    this.addConditionalSyntax(defaultCondition, this::handleRemove, REMOVE_LITERAL);
    this.addConditionalSyntax(defaultCondition, this::handleRemoveAll, REMOVE_ALL_LITERAL);
    this.addConditionalSyntax(defaultCondition, this::handleCleanup, CLEANUP_LITERAL, this.world);
    this.addConditionalSyntax(defaultCondition, this::handleCleanupAll, CLEANUP_ALL_LITERAL);
  }

  private @NonNull Argument<String> createTemplatePathArgument(@NonNull @Service I18n i18n) {
    return new ArgumentString("templatePath") {
      @Override
      public @NonNull String parse(@NonNull CommandSender sender, @NonNull String input) throws ArgumentSyntaxException {
        var template = ServiceTemplate.parse(input);
        if (template == null) {
          throw new ArgumentSyntaxException(i18n.translate("command-template-not-valid"), input, -1);
        }

        return template.toString();
      }
    }.setDefaultValue(() -> null);
  }

  private @NonNull ArgumentString createTargetGroupArgument(
    @NonNull @Service I18n i18n,
    @NonNull GroupConfigurationProvider groupProvider
  ) {
    return new ArgumentString("targetGroup") {
      @Override
      public @NonNull String parse(
        @NonNull CommandSender sender,
        @NonNull String input
      ) throws ArgumentSyntaxException {
        return groupProvider.groupConfigurations()
          .stream()
          .filter(group -> group.name().equalsIgnoreCase(input))
          .findFirst()
          .orElseThrow(
            () -> new ArgumentSyntaxException(i18n.translate("command-general-group-does-not-exist"), input, -1))
          .name();
      }
    };
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
      // the target block might be null if there is no block in reach
      var targetPoint = player.getTargetBlockPosition(15);
      if (targetPoint == null) {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
        return;
      }

      // check if the player faces a sign
      var targetBlock = instance.getBlock(targetPoint);
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
            context.get(this.targetGroup),
            context.get(this.template),
            worldPosition
          ));
          SignsConfiguration.sendMessage(
            "command-cloudsign-create-success",
            player::sendMessage, m -> m.replace("%group%", context.get(this.targetGroup)));
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
      // the target block might be null if there is no block in reach
      var targetPoint = player.getTargetBlockPosition(15);
      if (targetPoint == null) {
        SignsConfiguration.sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
        return;
      }

      // check if the player is facing a sign
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
    var world = context.getOrDefault(this.world, ((Player) sender).getInstance().getUniqueId().toString());
    // removes all signs on which location is not a sign anymore
    var removed = this.signManagement.removeMissingSigns(world);
    SignsConfiguration.sendMessage(
      "command-cloudsign-cleanup-success",
      sender::sendMessage,
      m -> m.replace("%amount%", Integer.toString(removed)));
  }

  private void handleCleanupAll(@NonNull CommandSender sender, @NonNull CommandContext context) {
    // removes all signs on which location is not a sign anymore
    var removed = this.signManagement.removeAllMissingSigns();
    SignsConfiguration.sendMessage(
      "command-cloudsign-cleanup-success",
      sender::sendMessage,
      m -> m.replace("%amount%", Integer.toString(removed)));
  }
}
