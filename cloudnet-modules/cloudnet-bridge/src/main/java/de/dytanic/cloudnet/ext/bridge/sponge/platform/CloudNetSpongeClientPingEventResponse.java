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
