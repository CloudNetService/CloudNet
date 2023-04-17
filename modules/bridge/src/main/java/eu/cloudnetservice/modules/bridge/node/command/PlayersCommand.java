/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.node.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.node.player.NodePlayerManager;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandAlias({"pl", "player"})
@CommandPermission("cloudnet.command.players")
@Description("module-bridge-player-command-description")
public class PlayersCommand {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final NodePlayerManager playerManager;
  private final CloudServiceProvider serviceProvider;

  @Inject
  public PlayersCommand(@NonNull NodePlayerManager playerManager, @NonNull CloudServiceProvider serviceProvider) {
    this.playerManager = playerManager;
    this.serviceProvider = serviceProvider;
  }

  @Parser(suggestions = "onlinePlayers")
  public @NonNull CloudPlayer defaultCloudPlayerParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var identifier = input.remove();
    CloudPlayer player;

    try {
      // first check if we can find a player using the uuid
      var uniqueId = UUID.fromString(identifier);
      player = this.playerManager.onlinePlayer(uniqueId);
    } catch (IllegalArgumentException exception) {
      // check if a player has the given name
      player = this.playerManager.firstOnlinePlayer(identifier);
    }

    if (player == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-players-player-not-online"));
    }
    return player;
  }

  @Suggestions("onlinePlayers")
  public @NonNull List<String> suggestOnlinePlayers(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.playerManager.players().values().stream()
      .map(Named::name)
      .toList();
  }

  @Parser(suggestions = "playerService")
  public @NonNull ServiceInfoSnapshot playerServiceParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var serviceInfoSnapshot = this.serviceProvider.serviceByName(name);
    if (serviceInfoSnapshot == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }
    return serviceInfoSnapshot;
  }

  @Suggestions("playerService")
  public @NonNull List<String> suggestPlayerService(@NonNull CommandContext<CommandSource> $, @NonNull String input) {
    return this.serviceProvider.services()
      .stream()
      .filter(snapshot -> ServiceEnvironmentType.minecraftServer(snapshot.serviceId().environment()))
      .map(Named::name)
      .toList();
  }

  @Parser(name = "offlinePlayer")
  public @NonNull CloudOfflinePlayer defaultCloudOfflinePlayerParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var identifier = input.remove();
    CloudOfflinePlayer player;

    try {
      // first check if we can find a player using the uuid
      var uniqueId = UUID.fromString(identifier);

      // try to get an online player
      player = this.playerManager.onlinePlayer(uniqueId);
      // check if we found an online player
      if (player == null) {
        // use an offline player as we could not find an online one
        player = this.playerManager.offlinePlayer(uniqueId);
      }

    } catch (IllegalArgumentException exception) {
      // check if we can find a player using his name
      // try to get an online player
      player = this.playerManager.firstOnlinePlayer(identifier);
      // check if we found an online player
      if (player == null) {
        // use an offline player as we could not find an online one
        player = this.playerManager.firstOfflinePlayer(identifier);
      }
    }

    if (player == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-players-player-not-registered"));
    }
    return player;
  }

  @CommandMethod("players|player|pl online")
  public void displayOnlinePlayers(@NonNull CommandSource source) {
    for (var player : this.playerManager.players().values()) {
      source.sendMessage(
        "Name: " + player.name() +
          " | UUID: " + player.uniqueId() +
          " | Proxy: " + player.loginService().serverName() +
          " | Service: " + player.connectedService().serverName());
    }
    source.sendMessage("=> Online players " + this.playerManager.onlineCount());
  }

  @CommandMethod("players|player|pl registered")
  public void displayRegisteredCount(@NonNull CommandSource source) {
    source.sendMessage("=> Registered players " + this.playerManager.registeredCount());
  }

  @CommandMethod("players|player|pl player <player>")
  public void displayPlayerInformation(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "player", parserName = "offlinePlayer") CloudOfflinePlayer offlinePlayer
  ) {
    var firstLoginTime = Instant.ofEpochMilli(offlinePlayer.firstLoginTimeMillis()).atZone(ZoneId.systemDefault());
    var lastLoginTime = Instant.ofEpochMilli(offlinePlayer.lastLoginTimeMillis()).atZone(ZoneId.systemDefault());

    source.sendMessage("CloudPlayer: " + offlinePlayer.name() + " | " + offlinePlayer.uniqueId());
    source.sendMessage("First login: " + DATE_TIME_FORMATTER.format(firstLoginTime));
    source.sendMessage("Last login: " + DATE_TIME_FORMATTER.format(lastLoginTime));

    // check if we have more information about the player
    if (offlinePlayer instanceof CloudPlayer onlinePlayer) {
      source.sendMessage("Proxy: " + onlinePlayer.loginService().serverName());
      source.sendMessage("Service: " + onlinePlayer.connectedService().serverName());
      source.sendMessage("Online Properties: ");

      // print the online properties of the player per line
      for (var line : onlinePlayer.onlineProperties().serializeToString().split("\n")) {
        source.sendMessage(line);
      }
    }

    // print the offline properties of the player per line
    for (var line : offlinePlayer.propertyHolder().serializeToString().split("\n")) {
      source.sendMessage(line);
    }
  }

  @CommandMethod("players|player|pl player <player> delete")
  public void deletePlayer(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "player", parserName = "offlinePlayer") CloudOfflinePlayer player
  ) {
    this.playerManager.deleteCloudOfflinePlayer(player);
    source.sendMessage(I18n.trans("module-bridge-command-players-delete-player", player.name(), player.uniqueId()));
  }

  @CommandMethod("players|player|pl online <player> kick [reason]")
  public void kickPlayer(
    @NonNull CommandSource source,
    @NonNull @Argument("player") CloudPlayer player,
    @Nullable @Greedy @Argument("reason") String reason,
    @Flag("force") boolean force
  ) {
    var reasonComponent = reason == null
      ? Component.empty()
      : ComponentFormats.BUNGEE_TO_ADVENTURE.convert(reason);
    player.playerExecutor().kick(reasonComponent);

    source.sendMessage(I18n.trans("module-bridge-command-players-kick-player",
      player.name(),
      player.uniqueId(),
      reason == null ? "No reason given" : reason));

    if (force) {
      // force the logout of the player and remove the player from the cache
      this.playerManager.logoutPlayer(player);
      source.sendMessage(I18n.trans("module-bridge-command-players-kick-player-force"));
    }
  }

  @CommandMethod("players|player|pl online <player> message <message>")
  public void messagePlayer(
    @NonNull CommandSource source,
    @NonNull @Argument("player") CloudPlayer player,
    @NonNull @Greedy @Argument("message") String message
  ) {
    player.playerExecutor().sendChatMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(message));
    source.sendMessage(
      I18n.trans("module-bridge-command-players-send-player-message", player.name(), player.uniqueId()));
  }

  @CommandMethod("players|player|pl online <player> connect <server>")
  public void connectPlayer(
    @NonNull CommandSource source,
    @NonNull @Argument("player") CloudPlayer player,
    @NonNull @Argument("server") ServiceInfoSnapshot server
  ) {
    if (server.readProperty(BridgeDocProperties.IS_ONLINE)) {
      player.playerExecutor().connect(server.name());

      source.sendMessage(
        I18n.trans("module-bridge-command-players-send-player-server", player.name(), player.uniqueId()));
    } else {
      source.sendMessage(
        I18n.trans("module-bridge-command-players-send-player-server-not-found", player.name(), player.uniqueId()));
    }
  }
}
