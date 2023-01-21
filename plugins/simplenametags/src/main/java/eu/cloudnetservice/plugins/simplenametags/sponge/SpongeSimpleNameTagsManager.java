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

package eu.cloudnetservice.plugins.simplenametags.sponge;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;

final class SpongeSimpleNameTagsManager extends SimpleNameTagsManager<ServerPlayer> {

  private static final Scoreboard.Builder BUILDER = Scoreboard.builder();

  private final Server server;

  public SpongeSimpleNameTagsManager(
    @NonNull Server server,
    @NonNull Executor syncTaskExecutor,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    super(syncTaskExecutor, eventManager, permissionManagement);
    this.server = server;
  }

  @Override
  public void updateNameTagsFor(@NonNull ServerPlayer player) {
    this.updateNameTagsFor(player, player.uniqueId(), player.name());
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull ServerPlayer player) {
    return player.uniqueId();
  }

  @Override
  public void displayName(@NonNull ServerPlayer player, @NonNull String displayName) {
    player.displayName().set(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(displayName));
  }

  @Override
  public void resetScoreboard(@NonNull ServerPlayer player) {
    if (this.server.serverScoreboard().map(player.scoreboard()::equals).orElse(false)) {
      player.setScoreboard(BUILDER.build());
    }
  }

  @Override
  public void registerPlayerToTeam(
    @NonNull ServerPlayer player,
    @NonNull ServerPlayer scoreboardHolder,
    @NonNull String name,
    @NonNull PermissionGroup group
  ) {
    var team = scoreboardHolder.scoreboard().team(name).orElseGet(() -> {
      // create and register a new team
      var newTeam = Team.builder().name(name).build();
      scoreboardHolder.scoreboard().registerTeam(newTeam);
      return newTeam;
    });
    // set the default team attributes
    team.setPrefix(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(group.prefix()));
    team.setSuffix(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(group.suffix()));
    // set the team color if possible
    var teamColor = AdventureTextFormatLookup.findColor(this.getColorChar(group));
    if (teamColor != null) {
      team.setColor(teamColor);
    }
    // add the player to the team
    team.addMember(player.teamRepresentation());
  }

  @Override
  public @NonNull Collection<? extends ServerPlayer> onlinePlayers() {
    return this.server.onlinePlayers();
  }

  @Override
  public @Nullable ServerPlayer onlinePlayer(@NonNull UUID uniqueId) {
    return this.server.player(uniqueId).orElse(null);
  }
}
