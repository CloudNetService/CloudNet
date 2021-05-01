package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import net.kyori.text.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * @deprecated Use {@link AdventureComponentMessenger} instead
 */
@Deprecated
@ApiStatus.ScheduledForRemoval
public class VelocityComponentMessenger {

    private VelocityComponentMessenger() {
        throw new UnsupportedOperationException();
    }

    public static void sendMessage(ICloudPlayer cloudPlayer, Component message) {
        Preconditions.checkNotNull(cloudPlayer);

        sendMessage(cloudPlayer.getUniqueId(), message);
    }

    public static void sendMessage(UUID uniqueId, Component message) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(message);

        AdventureComponentMessenger.sendMessage(uniqueId, LegacyText3ComponentSerializer.get().deserialize(message));
    }

    public static void broadcastMessage(Component message) {
        Preconditions.checkNotNull(message);

        broadcastMessage(message, null);
    }

    public static void broadcastMessage(Component message, String permission) {
        Preconditions.checkNotNull(message);

        AdventureComponentMessenger.broadcastMessage(LegacyText3ComponentSerializer.get().deserialize(message), permission);
    }
}
