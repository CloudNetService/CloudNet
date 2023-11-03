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

package eu.cloudnetservice.plugins.simplenametags;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.plugins.simplenametags.event.PrePlayerPrefixSetEvent;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleNameTagsManager<P> {

  protected static final String TEAM_NAME_FORMAT = "%s%s";

  protected final EventManager eventManager;
  protected final PermissionManagement permissionManagement;

  public SimpleNameTagsManager(
    @NonNull Executor syncTaskExecutor,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.eventManager = eventManager;
    this.permissionManagement = permissionManagement;

    eventManager.registerListener(new CloudSimpleNameTagsListener<>(syncTaskExecutor, this, permissionManagement));
  }

  public void updateNameTagsFor(@NonNull P player, @NonNull UUID playerUniqueId, @NonNull String playerName) {
    // get the permission group of the player
    var group = this.getPermissionGroup(playerUniqueId, player);
    if (group != null) {
      // find the highest sort id length of any known group on this instance
      var maxSortIdLength = this.permissionManagement.groups().stream()
        .map(PermissionGroup::sortId)
        .mapToInt(i -> (int) Math.log10(i) + 1)
        .max()
        .orElse(0);
      var groupTeamName = this.selectTeamName(group, maxSortIdLength);
      // reset the scoreboard of the current player
      this.resetScoreboard(player);
      // set the team entries for each player connected to the server
      for (P onlinePlayer : this.onlinePlayers()) {
        // reset the scoreboard for the player
        this.resetScoreboard(onlinePlayer);
        // add the player to the score board for this player
        this.registerPlayerToTeam(player, onlinePlayer, groupTeamName, group);

        // get the permission group for the player
        var onlinePlayerGroup = this.getPermissionGroup(this.playerUniqueId(onlinePlayer), onlinePlayer);
        if (onlinePlayerGroup != null) {
          // get the team name of the group
          var onlinePlayerGroupTeamName = this.selectTeamName(onlinePlayerGroup, maxSortIdLength);
          // register the team to the target updated player score board
          this.registerPlayerToTeam(onlinePlayer, player, onlinePlayerGroupTeamName, onlinePlayerGroup);
        }
      }
      // set the players display name
      this.displayName(player, group.display() + playerName);
    }
  }

  public abstract void updateNameTagsFor(@NonNull P player);

  public abstract @NonNull UUID playerUniqueId(@NonNull P player);

  public abstract void displayName(@NonNull P player, @NonNull String displayName);

  public abstract void resetScoreboard(@NonNull P player);

  public abstract void registerPlayerToTeam(
    @NonNull P player,
    @NonNull P scoreboardHolder,
    @NonNull String name,
    @NonNull PermissionGroup group);

  public abstract @NonNull Collection<? extends P> onlinePlayers();

  public abstract @Nullable P onlinePlayer(@NonNull UUID uniqueId);

  protected char getColorChar(@NonNull PermissionGroup group) {
    // check if the color of the group is given and valid
    if (group.color().length() == 2) {
      // check if the first char is a color indicator
      var indicatorChar = group.color().charAt(0);
      if (indicatorChar == '&' || indicatorChar == '§') {
        // the next char should be the color char then
        return group.color().charAt(1);
      }
    }
    // search for the last color char in the prefix of the group
    var length = group.prefix().length();
    for (var index = length - 2; index >= 0; index--) {
      // check if the current char is a color indicator char
      var atPosition = group.prefix().charAt(index);
      if (atPosition == '&' || atPosition == '§') {
        return group.prefix().charAt(index + 1);
      }
    }
    // no color char found
    return ' ';
  }

  protected @Nullable PermissionGroup getPermissionGroup(@NonNull UUID playerUniqueId, @NonNull P platformPlayer) {
    // select the best permission group for the player
    var user = this.permissionManagement.user(playerUniqueId);
    // no user -> no group
    if (user == null) {
      return null;
    }
    // get the highest group of the player
    var group = this.permissionManagement.highestPermissionGroup(user);
    // no group -> try the default group
    if (group == null) {
      group = this.permissionManagement.defaultPermissionGroup();
      // no default group -> skip
      if (group == null) {
        return null;
      }
    }
    // post the choose event to let the user modify the permission group of the player (for example to nick a player)
    return this.eventManager.callEvent(new PrePlayerPrefixSetEvent<>(platformPlayer, group)).group();
  }

  protected @NonNull String selectTeamName(@NonNull PermissionGroup group, int highestSortIdLength) {
    // get the length of the group's sort id
    var sortIdLength = (int) Math.log10(group.sortId()) + 1;
    var teamName = String.format(
      TEAM_NAME_FORMAT,
      highestSortIdLength == sortIdLength
        ? group.sortId()
        : String.format("%0" + highestSortIdLength + "d", group.sortId()),
      group.name());
    // shorten the name if needed
    return teamName.length() > 16 ? teamName.substring(0, 16) : teamName;
  }
}
