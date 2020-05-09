package de.dytanic.cloudnet.ext.bridge.player.executor;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.UUID;

public class DefaultPlayerExecutor implements PlayerExecutor {

    private final UUID uniqueId;

    public DefaultPlayerExecutor(@NotNull UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public @NotNull UUID getPlayerUniqueId() {
        return this.uniqueId;
    }

    @Override
    public void connect(@NotNull String serviceName) {
        Preconditions.checkNotNull(serviceName);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_on_proxy_player_to_server",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("serviceName", serviceName)
        );
    }

    @Override
    public void kick(@NotNull String message) {
        Preconditions.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "kick_on_proxy_player_from_network",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("kickMessage", message)
        );
    }

    @Override
    public void sendChatMessage(@NotNull String message) {
        Preconditions.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("message", message)
        );
    }

    @Override
    public void sendPluginMessage(@NotNull String tag, @NotNull byte[] data) {
        Preconditions.checkNotNull(tag);
        Preconditions.checkNotNull(data);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_plugin_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("tag", tag)
                        .append("data", Base64.getEncoder().encodeToString(data))
        );
    }

}
