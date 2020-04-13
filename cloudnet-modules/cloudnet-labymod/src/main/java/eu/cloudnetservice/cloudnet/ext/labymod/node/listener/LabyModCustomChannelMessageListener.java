package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.node.CloudNetLabyModModule;

import java.util.UUID;

public class LabyModCustomChannelMessageListener {

    private final CloudNetLabyModModule module;

    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    public LabyModCustomChannelMessageListener(CloudNetLabyModModule module) {
        this.module = module;
    }

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(LabyModConstants.CLOUDNET_CHANNEL_NAME)) {
            return;
        }

        switch (event.getId()) {
            case LabyModConstants.GET_CONFIGURATION:
                event.setCallbackPacket(new JsonDocument().append("labyModConfig", this.module.getConfiguration()));
                break;

            case LabyModConstants.GET_PLAYER_JOIN_SECRET:
                event.setCallbackPacket(new JsonDocument().append("player", this.getPlayerByJoinSecret(event.getHeader().get("joinSecret", UUID.class))));
                break;

            case LabyModConstants.GET_PLAYER_SPECTATE_SECRET:
                event.setCallbackPacket(new JsonDocument().append("player", this.getPlayerBySpectateSecret(event.getHeader().get("spectateSecret", UUID.class))));
                break;
        }
    }

    private ICloudPlayer getPlayerByJoinSecret(UUID joinSecret) {
        return this.playerManager.getOnlinePlayers()
                .stream()
                .filter(o -> LabyModUtils.getLabyModOptions(o) != null)
                .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret() != null)
                .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret().equals(joinSecret))
                .findFirst()
                .orElse(null);
    }

    private ICloudPlayer getPlayerBySpectateSecret(UUID spectateSecret) {
        return this.playerManager.getOnlinePlayers()
                .stream()
                .filter(o -> LabyModUtils.getLabyModOptions(o) != null)
                .filter(o -> LabyModUtils.getLabyModOptions(o).getSpectateSecret() != null)
                .filter(o -> LabyModUtils.getLabyModOptions(o).getSpectateSecret().equals(spectateSecret))
                .findFirst()
                .orElse(null);
    }

}
