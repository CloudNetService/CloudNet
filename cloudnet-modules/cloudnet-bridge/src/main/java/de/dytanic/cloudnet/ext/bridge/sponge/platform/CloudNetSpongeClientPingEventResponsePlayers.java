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

package de.dytanic.cloudnet.ext.bridge.sponge.platform;

import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;

class CloudNetSpongeClientPingEventResponsePlayers implements ClientPingServerEvent.Response.Players {

  private final List<GameProfile> gameProfiles = new ArrayList<>(
    Sponge.getServer().getGameProfileManager().getCache().getProfiles());

  private int online;
  private int max;

  private CloudNetSpongeClientPingEventResponsePlayers(int online, int max) {
    this.online = online;
    this.max = max;
  }

  static @NotNull ClientPingServerEvent.Response.Players fromCloudNet() {
    return new CloudNetSpongeClientPingEventResponsePlayers(
      Sponge.getServer().getOnlinePlayers().size(),
      BridgeServerHelper.getMaxPlayers()
    );
  }

  @Override
  public int getOnline() {
    return this.online;
  }

  @Override
  public void setOnline(int online) {
    this.online = online;
  }

  @Override
  public int getMax() {
    return this.max;
  }

  @Override
  public void setMax(int max) {
    this.max = max;
  }

  @Override
  public @NotNull List<GameProfile> getProfiles() {
    return this.gameProfiles;
  }
}
