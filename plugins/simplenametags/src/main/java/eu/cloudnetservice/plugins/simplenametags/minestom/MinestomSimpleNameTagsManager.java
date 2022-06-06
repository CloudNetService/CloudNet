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

package eu.cloudnetservice.plugins.simplenametags.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import eu.cloudnetservice.ext.adventure.AdventureTextFormatLookup;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import org.jetbrains.annotations.Nullable;

final class MinestomSimpleNameTagsManager extends SimpleNameTagsManager<Player> {

  public MinestomSimpleNameTagsManager() {
    super(MoreExecutors.directExecutor());
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
  public void displayName(@NonNull Player player, @NonNull String displayName) {
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
    var team = MinecraftServer.getTeamManager().getTeams()
      .stream()
      .filter(registeredTeam -> registeredTeam.getTeamName().equals(name))
      .findFirst()
      .orElseGet(() -> MinecraftServer.getTeamManager().createBuilder(name).build());
    // set the default team attributes
    team.setPrefix(AdventureSerializerUtil.serialize(group.prefix()));
    team.setSuffix(AdventureSerializerUtil.serialize(group.suffix()));
    // set the team color if possible
    var teamColor = AdventureTextFormatLookup.findColor(this.getColorChar(group));
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
    return MinecraftServer.getConnectionManager().getOnlinePlayers()
      .stream()
      .filter(player -> !(player instanceof FakePlayer))
      .toList();
  }

  @Override
  public @Nullable Player onlinePlayer(@NonNull UUID uniqueId) {
    var player = MinecraftServer.getConnectionManager().getPlayer(uniqueId);
    // only provide real players
    return player instanceof FakePlayer ? null : player;
  }
}
