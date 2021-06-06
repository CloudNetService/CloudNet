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
