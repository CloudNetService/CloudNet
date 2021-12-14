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

package de.dytanic.cloudnet.ext.bridge.node.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.players")
@Description("Management for online and offline cloud players")
public class CommandPlayers {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  private final NodePlayerManager playerManager;

  public CommandPlayers(@NotNull NodePlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @Parser(suggestions = "onlinePlayers")
  public CloudPlayer defaultCloudPlayerParser(CommandContext<?> $, Queue<String> input) {
    var identifier = input.remove();
    CloudPlayer player;

    try {
      // first check if we can find a player using the uuid
      var uniqueId = UUID.fromString(identifier);
      player = this.playerManager.getOnlinePlayer(uniqueId);
    } catch (IllegalArgumentException exception) {
      // check if a player has the given name
      player = this.playerManager.getFirstOnlinePlayer(identifier);
    }

    if (player == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-players-player-not-online"));
    }
    return player;
  }

  @Suggestions("onlinePlayers")
  public List<String> suggestOnlinePlayers(CommandContext<?> $, String input) {
    return this.playerManager.getOnlinePlayers().values().stream()
      .map(INameable::name)
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "playerService")
  public ServiceInfoSnapshot playerServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceProvider()
      .getCloudServiceByName(name);
    if (serviceInfoSnapshot == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }
    return serviceInfoSnapshot;
  }

  @Suggestions("playerService")
  public List<String> suggestPlayerService(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getCloudServiceProvider().getCloudServices()
      .stream()
      .filter(snapshot -> ServiceEnvironmentType.isMinecraftServer(snapshot.getServiceId().getEnvironment()))
      .map(INameable::name)
      .collect(Collectors.toList());
  }

  @Parser(name = "offlinePlayer")
  public CloudOfflinePlayer defaultCloudOfflinePlayerParser(CommandContext<?> $, Queue<String> input) {
    var identifier = input.remove();
    CloudOfflinePlayer player;

    try {
      // first check if we can find a player using the uuid
      var uniqueId = UUID.fromString(identifier);

      // try to get an online player
      player = this.playerManager.getOnlinePlayer(uniqueId);
      // check if we found an online player
      if (player == null) {
        // use an offline player as we could not find an online one
        player = this.playerManager.getOfflinePlayer(uniqueId);
      }

    } catch (IllegalArgumentException exception) {
      // check if we can find a player using his name
      // try to get an online player
      player = this.playerManager.getFirstOnlinePlayer(identifier);
      // check if we found an online player
      if (player == null) {
        // use an offline player as we could not find an online one
        player = this.playerManager.getFirstOfflinePlayer(identifier);
      }
    }

    if (player == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-players-player-not-registered"));
    }
    return player;
  }

  @CommandMethod("players online")
  public void displayOnlinePlayers(@NotNull CommandSource source) {
    for (var player : this.playerManager.getOnlinePlayers().values()) {
      source.sendMessage(
        "Name: " + player.name() +
          " | UUID: " + player.getUniqueId() +
          " | Proxy: " + player.getLoginService().getServerName() +
          " | Service: " + player.getConnectedService().getServerName());
    }
    source.sendMessage("=> Online players " + this.playerManager.getOnlineCount());
  }

  @CommandMethod("players registered")
  public void displayRegisteredCount(@NotNull CommandSource source) {
    source.sendMessage("=> Registered players " + this.playerManager.getRegisteredCount());
  }

  @CommandMethod("players player <player>")
  public void displayPlayerInformation(
    @NotNull CommandSource source,
    @NotNull @Argument(value = "player", parserName = "offlinePlayer") CloudOfflinePlayer offlinePlayer
  ) {
    source.sendMessage("CloudPlayer: " + offlinePlayer.name() + " | " + offlinePlayer.getUniqueId());
    source.sendMessage("First login: " + DATE_FORMAT.format(offlinePlayer.getFirstLoginTimeMillis()));
    source.sendMessage("Last login: " + DATE_FORMAT.format(offlinePlayer.getLastLoginTimeMillis()));
    // check if we have more information about the player
    if (offlinePlayer instanceof CloudPlayer onlinePlayer) {
      source.sendMessage("Proxy: " + onlinePlayer.getLoginService().getServerName());
      source.sendMessage("Service: " + onlinePlayer.getConnectedService().getServerName());
      source.sendMessage("Online Properties: ");
      // print the online properties of the player per line
      for (var line : onlinePlayer.getOnlineProperties().toPrettyJson().split("\n")) {
        source.sendMessage(line);
      }
    }
    // print the offline properties of the player per line
    for (var line : offlinePlayer.getProperties().toPrettyJson().split("\n")) {
      source.sendMessage(line);
    }
  }

  @CommandMethod("players player <player> delete")
  public void deletePlayer(
    @NotNull CommandSource source,
    @NotNull @Argument(value = "player", parserName = "offlinePlayer") CloudOfflinePlayer player
  ) {
    this.playerManager.deleteCloudOfflinePlayer(player);
    source.sendMessage(I18n.trans("module-bridge-command-players-delete-player")
      .replace("%name%", player.name())
      .replace("%uniqueId%", player.getUniqueId().toString()));
  }

  @CommandMethod("players player <player> kick [reason]")
  public void kickPlayer(
    @NotNull CommandSource source,
    @NotNull @Argument("player") CloudPlayer player,
    @Nullable @Greedy @Argument("reason") String reason,
    @Flag("force") boolean force
  ) {
    var reasonComponent = reason == null ? Component.empty() : AdventureSerializerUtil.serialize(reason);
    player.getPlayerExecutor().kick(reasonComponent);

    source.sendMessage(I18n.trans("module-bridge-command-players-kick-player")
      .replace("%name%", player.name())
      .replace("%uniqueId%", player.getUniqueId().toString())
      .replace("%reason%", reason == null ? "No reason given" : reason));

    if (force) {
      // force the logout of the player and remove the player from the cache
      this.playerManager.logoutPlayer(player);
      source.sendMessage(I18n.trans("module-bridge-command-players-kick-player-force"));
    }
  }

  @CommandMethod("players player <player> message <message>")
  public void messagePlayer(
    @NotNull CommandSource source,
    @NotNull @Argument("player") CloudPlayer player,
    @NotNull @Greedy @Argument("message") String message
  ) {
    player.getPlayerExecutor().sendMessage(AdventureSerializerUtil.serialize(message));
    source.sendMessage(I18n.trans("module-bridge-command-players-send-player-message")
      .replace("%name%", player.name())
      .replace("%uniqueId%", player.getUniqueId().toString()));
  }

  @CommandMethod("players player <player> connect <server>")
  public void connectPlayer(
    @NotNull CommandSource source,
    @NotNull @Argument("player") CloudPlayer player,
    @NotNull @Argument("server") ServiceInfoSnapshot server
  ) {
    if (BridgeServiceProperties.IS_ONLINE.get(server).orElse(false)) {
      player.getPlayerExecutor().connect(server.name());

      source.sendMessage(I18n.trans("module-bridge-command-players-send-player-server")
        .replace("%name%", player.name())
        .replace("%uniqueId%", player.getUniqueId().toString()));
    } else {
      source.sendMessage(I18n.trans("module-bridge-command-players-send-player-server-not-found")
        .replace("%name%", player.name())
        .replace("%uniqueId%", player.getUniqueId().toString()));
    }
  }
}
