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

package de.dytanic.cloudnet.ext.simplenametags.bukkit;

import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.ext.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BukkitSimpleNameTagsManager extends SimpleNameTagsManager<Player> {

  public BukkitSimpleNameTagsManager(@NotNull Executor syncTaskExecutor) {
    super(syncTaskExecutor);
  }

  @Override
  public void updateNameTagsFor(@NotNull Player player) {
    this.updateNameTagsFor(player, player.getUniqueId(), player.getName());
  }

  @Override
  public @NotNull UUID getPlayerUniqueId(@NotNull Player player) {
    return player.getUniqueId();
  }

  @Override
  public void setDisplayName(@NotNull Player player, @NotNull String displayName) {
    player.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
  }

  @Override
  public void resetScoreboard(@NotNull Player player) {
    // just to make IntelliJ happy - the manager should not be null when a player connected successfully
    var manager = player.getServer().getScoreboardManager();
    if (manager != null && player.getScoreboard().equals(manager.getMainScoreboard())) {
      player.setScoreboard(manager.getNewScoreboard());
    }
  }

  @Override
  public void registerPlayerToTeam(
    @NotNull Player player,
    @NotNull Player scoreboardHolder,
    @NotNull String name,
    @NotNull PermissionGroup group
  ) {
    // check if the team is already registered
    var team = scoreboardHolder.getScoreboard().getTeam(name);
    if (team == null) {
      team = scoreboardHolder.getScoreboard().registerNewTeam(name);
    }
    // set the default team attributes
    team.setPrefix(ChatColor.translateAlternateColorCodes('&', group.getPrefix()));
    team.setSuffix(ChatColor.translateAlternateColorCodes('&', group.getSuffix()));
    // set the team color if possible
    var teamColor = ChatColor.getByChar(this.getColorChar(group));
    if (teamColor != null) {
      BukkitCompatibility.setTeamColor(team, teamColor);
    }
    // register the player to the team
    team.addEntry(player.getName());
  }

  @Override
  public @NotNull Collection<? extends Player> getOnlinePlayers() {
    return Bukkit.getOnlinePlayers();
  }

  @Override
  public @Nullable Player getOnlinePlayer(@NotNull UUID uniqueId) {
    return Bukkit.getPlayer(uniqueId);
  }
}
