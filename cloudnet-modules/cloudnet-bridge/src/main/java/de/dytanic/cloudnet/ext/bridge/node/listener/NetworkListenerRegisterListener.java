package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;

import java.io.File;

public final class NetworkListenerRegisterListener {

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        /*event.getNode().sendCustomChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER,
                new JsonDocument("bridgeConfiguration", CloudNetBridgeModule.getInstance().getBridgeConfiguration())
        );*/
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || !event.isQuery()) {
            return;
        }

        if (BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION.equals(event.getMessage())) {
            BridgeConfiguration configuration = JsonDocument.newDocument(
                    new File(CloudNetDriver.getInstance().getModuleProvider().getModule("CloudNet-Bridge").getDataFolder(), "config.json")
            ).get("config", BridgeConfiguration.TYPE);

            event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                    .json(JsonDocument.newDocument("bridgeConfig", configuration))
                    .build());
        }
    }
}