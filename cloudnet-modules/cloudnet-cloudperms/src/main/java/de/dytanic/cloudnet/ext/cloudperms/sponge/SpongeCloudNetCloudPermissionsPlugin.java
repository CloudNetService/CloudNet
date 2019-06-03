package de.dytanic.cloudnet.ext.cloudperms.sponge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(
    id = "cloudnet_cloudperms",
    name = "CloudNet-CloudPerms",
    version = "1.0",
    description = "Sponge extension which implement the permission management system from CloudNet into Sponge for players",
    url = "https://cloudnetservice.eu"
)
public final class SpongeCloudNetCloudPermissionsPlugin {

  @Listener
  public void onEnable(GameStartedServerEvent event) {

  }

  @Listener
  public void onDisable(GameStoppingServerEvent event) {
    CloudNetDriver.getInstance().getEventManager()
        .unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
        this.getClass().getClassLoader());
  }
}