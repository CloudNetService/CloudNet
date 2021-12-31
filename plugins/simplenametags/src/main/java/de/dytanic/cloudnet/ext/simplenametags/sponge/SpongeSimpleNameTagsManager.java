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

package de.dytanic.cloudnet.ext.simplenametags.sponge;

import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.ext.simplenametags.SimpleNameTagsManager;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;

final class SpongeSimpleNameTagsManager extends SimpleNameTagsManager<ServerPlayer> {

  private static final Scoreboard.Builder BUILDER = Scoreboard.builder();

  public SpongeSimpleNameTagsManager(@NonNull Executor syncTaskExecutor) {
    super(syncTaskExecutor);
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
    player.displayName().set(AdventureSerializerUtil.serialize(displayName));
  }

  @Override
  public void resetScoreboard(@NonNull ServerPlayer player) {
    if (Sponge.server().serverScoreboard().map(player.scoreboard()::equals).orElse(false)) {
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
    team.setPrefix(AdventureSerializerUtil.serialize(group.prefix()));
    team.setSuffix(AdventureSerializerUtil.serialize(group.suffix()));
    // set the team color if possible
    var teamColor = NamedTextColor.ofExact(this.getColorChar(group));
    if (teamColor != null) {
      team.setColor(teamColor);
    }
    // add the player to the team
    team.addMember(player.teamRepresentation());
  }

  @Override
  public @NonNull Collection<? extends ServerPlayer> onlinePlayers() {
    return Sponge.server().onlinePlayers();
  }

  @Override
  public @Nullable ServerPlayer onlinePlayer(@NonNull UUID uniqueId) {
    return Sponge.server().player(uniqueId).orElse(null);
  }
}
