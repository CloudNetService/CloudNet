package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
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
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(LabyModConstants.CLOUDNET_CHANNEL_NAME) || event.getMessage() == null || !event.isQuery()) {
            return;
        }

        switch (event.getMessage()) {
            case LabyModConstants.GET_CONFIGURATION:
                event.setJsonResponse(JsonDocument.newDocument("labyModConfig", this.module.getConfiguration()));
                break;

            case LabyModConstants.GET_PLAYER_JOIN_SECRET:
                UUID joinSecret = event.getBuffer().readUUID();
                event.setBinaryResponse(ProtocolBuffer.create().writeOptionalObject(this.getPlayerByJoinSecret(joinSecret)));
                break;

            case LabyModConstants.GET_PLAYER_SPECTATE_SECRET:
                UUID spectateSecret = event.getBuffer().readUUID();
                event.setBinaryResponse(ProtocolBuffer.create().writeOptionalObject(this.getPlayerBySpectateSecret(spectateSecret)));
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
