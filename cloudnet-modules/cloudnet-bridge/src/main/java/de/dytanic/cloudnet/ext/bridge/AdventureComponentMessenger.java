package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.UUID;

public final class AdventureComponentMessenger {

    private AdventureComponentMessenger() {
        throw new UnsupportedOperationException();
    }

    public static void sendMessage(ICloudPlayer cloudPlayer, Component message) {
        Preconditions.checkNotNull(cloudPlayer);

        sendMessage(cloudPlayer.getUniqueId(), message);
    }

    public static void sendMessage(UUID uniqueId, Component message) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(message);

        DefaultPlayerExecutor.builder()
                .message("send_message_component")
                .buffer(ProtocolBuffer.create()
                        .writeUUID(uniqueId)
                        .writeString(GsonComponentSerializer.gson().serialize(message))
                )
                .build().send();
    }

    public static void broadcastMessage(Component message) {
        Preconditions.checkNotNull(message);

        broadcastMessage(message, null);
    }

    public static void broadcastMessage(Component message, String permission) {
        Preconditions.checkNotNull(message);

        DefaultPlayerExecutor.builder()
                .message("broadcast_message_component")
                .buffer(ProtocolBuffer.create()
                        .writeString(GsonComponentSerializer.gson().serialize(message))
                        .writeOptionalString(permission)
                )
                .build().send();
    }
}
