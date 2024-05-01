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

package eu.cloudnetservice.plugins.simplenametags.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.scoreboard.TeamManager;
import org.jetbrains.annotations.Nullable;

final class MinestomSimpleNameTagsManager extends SimpleNameTagsManager<Player> {

  private final TeamManager teamManager;
  private final ConnectionManager connectionManager;

  public MinestomSimpleNameTagsManager(
    @NonNull TeamManager teamManager,
    @NonNull ConnectionManager connectionManager,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    super(MoreExecutors.directExecutor(), eventManager, permissionManagement);
    this.teamManager = teamManager;
    this.connectionManager = connectionManager;
  }

  @Override
  public void updateNameTagsFor(@NonNull Player player) {
    // ignore fake players
    if (player instanceof FakePlayer) {
      return;
    }

    this.updateNameTagsFor(player, player.getUuid(), player.getUsername());
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull Player player) {
    return player.getUuid();
  }

  @Override
  public void displayName(@NonNull Player player, @NonNull Component displayName) {
  }

  @Override
  public void resetScoreboard(@NonNull Player player) {
    player.setTeam(null);
  }

  @Override
  public void registerPlayerToTeam(
    @NonNull Player player,
    @NonNull Player scoreboardHolder,
    @NonNull String name,
    @NonNull PermissionGroup group
  ) {
    // get the already registered team or a new one if needed
    var team = this.teamManager.getTeams()
      .stream()
      .filter(registeredTeam -> registeredTeam.getTeamName().equals(name))
      .findFirst()
      .orElseGet(() -> this.teamManager.createBuilder(name).build());
    // set the default team attributes
    team.setPrefix(ComponentFormats.USER_INPUT.toAdventure(group.prefix()));
    team.setSuffix(ComponentFormats.USER_INPUT.toAdventure(group.suffix()));
    // set the team color if possible
    var teamColor = AdventureTextFormatLookup.findColor(group.color());
    if (teamColor != null) {
      team.setTeamColor(teamColor);
    }
    // register the player to the team
    team.sendUpdatePacket();
    team.addMember(player.getUsername());
  }

  @Override
  public @NonNull Collection<? extends Player> onlinePlayers() {
    // remove all fake players in the collection
    return this.connectionManager.getOnlinePlayers()
      .stream()
      .filter(player -> !(player instanceof FakePlayer))
      .toList();
  }

  @Override
  public @Nullable Player onlinePlayer(@NonNull UUID uniqueId) {
    // only provide real players
    var player = this.connectionManager.getPlayer(uniqueId);
    return player instanceof FakePlayer ? null : player;
  }
}
