package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

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
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION_CHANNEL_NAME)) {
            return;
        }

        if (BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION.equals(event.getId())) {
            event.setCallbackPacket(
                    new JsonDocument().append("bridgeConfig", (BridgeConfiguration) JsonDocument.newDocument(
                            new File(CloudNetDriver.getInstance().getModuleProvider().getModule("CloudNet-Bridge").getDataFolder(), "config.json")
                    ).get("config", BridgeConfiguration.TYPE))
            );
        }
    }
}