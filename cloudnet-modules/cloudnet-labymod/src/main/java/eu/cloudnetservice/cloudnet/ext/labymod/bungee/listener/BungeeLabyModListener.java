package eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener;

import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeLabyModListener implements Listener {

    private final AbstractLabyModManagement labyModManagement;

    public BungeeLabyModListener(AbstractLabyModManagement labyModManagement) {
        this.labyModManagement = labyModManagement;
    }

    @EventHandler
    public void handle(ServerConnectedEvent event) {
        this.labyModManagement.sendServerUpdate(event.getPlayer().getUniqueId(), event.getServer().getInfo().getName());
    }

    @EventHandler
    public void handle(PluginMessageEvent event) {
        LabyModConfiguration configuration = LabyModUtils.getConfiguration();
        if (configuration == null || !configuration.isEnabled() || !event.getTag().equals(LabyModConstants.LMC_CHANNEL_NAME)) {
            return;
        }

        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        this.labyModManagement.handleChannelMessage(player.getUniqueId(), event.getData());
    }

}
