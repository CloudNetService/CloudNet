package eu.cloudnetservice.cloudnet.ext.signs.sponge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(
        id = "cloudnet_signs",
        name = "CloudNetSigns",
        version = "2.0",
        authors = "CloudNetService",
        url = "https://cloudnetservice.eu",
        dependencies = @Dependency(id = "cloudnet_bridge")
)
public class SpongeSignsPlugin {

    @Listener
    public void handleStart(GameStartedServerEvent event) {
        AbstractServiceSignManagement<Sign> signManagement = new SpongeSignManagement(this);
        signManagement.registerToServiceRegistry();

        CloudNetDriver.getInstance().getEventManager().registerListener(new GlobalChannelMessageListener(signManagement));
    }

    @Listener
    public void handleShutdown(GameStoppingServerEvent event) {
        SpongeSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
