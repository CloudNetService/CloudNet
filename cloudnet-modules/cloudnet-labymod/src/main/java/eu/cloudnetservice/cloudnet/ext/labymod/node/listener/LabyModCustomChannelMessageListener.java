package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.node.CloudNetLabyModModule;

public class LabyModCustomChannelMessageListener {

    private CloudNetLabyModModule module;

    public LabyModCustomChannelMessageListener(CloudNetLabyModModule module) {
        this.module = module;
    }

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(LabyModConstants.GET_CONFIGURATION_CHANNEL_NAME)) {
            return;
        }

        if (LabyModConstants.GET_CONFIGURATION.equals(event.getId())) {
            event.setCallbackPacket(new JsonDocument().append("labyModConfig", this.module.getConfiguration()));
        }
    }

}
