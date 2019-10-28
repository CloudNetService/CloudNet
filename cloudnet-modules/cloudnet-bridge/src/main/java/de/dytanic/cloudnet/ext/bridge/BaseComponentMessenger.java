package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import net.md_5.bungee.api.chat.BaseComponent;

public class BaseComponentMessenger {
    private BaseComponentMessenger() {
        throw new UnsupportedOperationException();
    }

    public static void sendMessage(ICloudPlayer cloudPlayer, BaseComponent[] messages) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("messages", messages)
        );
    }

    public static void broadcastMessage(BaseComponent[] messages) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("messages", messages)
        );
    }

    public static void broadcastMessage(BaseComponent[] messages, String permission) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("messages", messages)
                        .append("permission", permission)
        );
    }
}
