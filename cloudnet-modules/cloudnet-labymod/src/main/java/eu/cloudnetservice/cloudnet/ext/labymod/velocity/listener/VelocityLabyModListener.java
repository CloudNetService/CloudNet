package eu.cloudnetservice.cloudnet.ext.labymod.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;

public class VelocityLabyModListener {

    private final AbstractLabyModManagement labyModManagement;

    public VelocityLabyModListener(AbstractLabyModManagement labyModManagement) {
        this.labyModManagement = labyModManagement;
    }

    @Subscribe
    public void handleServerConnected(ServerConnectedEvent event) {
        this.labyModManagement.sendServerUpdate(event.getPlayer().getUniqueId(), event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void handlePluginMessage(PluginMessageEvent event) {
        LabyModConfiguration configuration = LabyModUtils.getConfiguration();
        if (configuration == null || !configuration.isEnabled() || !event.getIdentifier().getId().equals(LabyModConstants.LMC_CHANNEL_NAME)) {
            return;
        }

        if (!(event.getSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getSource();

        this.labyModManagement.handleChannelMessage(player.getUniqueId(), event.getData());
    }

}
