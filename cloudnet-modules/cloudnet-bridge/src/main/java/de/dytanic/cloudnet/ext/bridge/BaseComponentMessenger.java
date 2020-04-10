package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.UUID;

public class BaseComponentMessenger {

    private BaseComponentMessenger() {
        throw new UnsupportedOperationException();
    }

    public static void sendMessage(ICloudPlayer cloudPlayer, BaseComponent[] messages) {
        Preconditions.checkNotNull(cloudPlayer);

        sendMessage(cloudPlayer.getUniqueId(), messages);
    }

    public static void sendMessage(UUID uniqueId, BaseComponent[] messages) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(messages);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("messages", messages)
        );
    }

    public static void broadcastMessage(BaseComponent[] messages) {
        Preconditions.checkNotNull(messages);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("messages", messages)
        );
    }

    public static void broadcastMessage(BaseComponent[] messages, String permission) {
        Preconditions.checkNotNull(messages);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("messages", messages)
                        .append("permission", permission)
        );
    }
}
