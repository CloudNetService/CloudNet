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
import java.util.Optional;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.text.Text;

public class CloudNetSpongeClientPingEventResponse implements ClientPingServerEvent.Response {

  private final Players players = CloudNetSpongeClientPingEventResponsePlayers.fromCloudNet();
  private Text description = Text.of(BridgeServerHelper.getMotd());

  @Override
  public @NotNull Text getDescription() {
    return this.description;
  }

  @Override
  public void setDescription(@NotNull Text description) {
    this.description = description;
  }

  @Override
  public @NotNull Optional<Players> getPlayers() {
    return Optional.of(this.players);
  }

  @Override
  public @NotNull MinecraftVersion getVersion() {
    return Sponge.getPlatform().getMinecraftVersion();
  }

  @Override
  public @NotNull Optional<Favicon> getFavicon() {
    return Optional.empty();
  }

  @Override
  public void setFavicon(@Nullable Favicon favicon) {
  }

  @Override
  public void setHidePlayers(boolean hide) {
  }
}
