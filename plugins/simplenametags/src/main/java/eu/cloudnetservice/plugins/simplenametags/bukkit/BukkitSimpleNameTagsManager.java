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

package eu.cloudnetservice.plugins.simplenametags.bukkit;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

final class BukkitSimpleNameTagsManager extends SimpleNameTagsManager<Player> {

  public BukkitSimpleNameTagsManager(
    @NonNull Executor syncTaskExecutor,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    super(syncTaskExecutor, eventManager, permissionManagement);
  }

  @Override
  public void updateNameTagsFor(@NonNull Player player) {
    this.updateNameTagsFor(player, player.getUniqueId(), player.getName());
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull Player player) {
    return player.getUniqueId();
  }

  @Override
  public void displayName(@NonNull Player player, @NonNull String displayName) {
    player.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
  }

  @Override
  public void resetScoreboard(@NonNull Player player) {
    // just to make IntelliJ happy - the manager should not be null when a player connected successfully
    var manager = player.getServer().getScoreboardManager();
    if (manager != null && player.getScoreboard().equals(manager.getMainScoreboard())) {
      player.setScoreboard(manager.getNewScoreboard());
    }
  }

  @Override
  public void registerPlayerToTeam(
    @NonNull Player player,
    @NonNull Player scoreboardHolder,
    @NonNull String name,
    @NonNull PermissionGroup group
  ) {
    // check if the team is already registered
    var team = scoreboardHolder.getScoreboard().getTeam(name);
    if (team == null) {
      team = scoreboardHolder.getScoreboard().registerNewTeam(name);
    }
    // set the default team attributes
    team.setPrefix(ChatColor.translateAlternateColorCodes('&', group.prefix()));
    team.setSuffix(ChatColor.translateAlternateColorCodes('&', group.suffix()));
    // set the team color if possible
    var teamColor = ChatColor.getByChar(this.getColorChar(group));
    if (teamColor != null) {
      BukkitCompatibility.teamColor(team, teamColor);
    }
    // register the player to the team
    team.addEntry(player.getName());
  }

  @Override
  public @NonNull Collection<? extends Player> onlinePlayers() {
    return Bukkit.getOnlinePlayers();
  }

  @Override
  public @Nullable Player onlinePlayer(@NonNull UUID uniqueId) {
    return Bukkit.getPlayer(uniqueId);
  }
}
