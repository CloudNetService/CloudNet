package de.dytanic.cloudnet.ext.bridge.player;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class DefaultPlayerManager implements IPlayerManager {

    @Override
    public @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId) {
        return new DefaultPlayerExecutor(uniqueId);
    }

    @Override
    public void broadcastMessage(@NotNull String message) {
        Preconditions.checkNotNull(message);

        this.broadcastMessage(message, null);
    }

    @Override
    public void broadcastMessage(@NotNull String message, @Nullable String permission) {
        Preconditions.checkNotNull(message);

        DefaultPlayerExecutor.builder()
                .message("broadcast_message")
                .buffer(ProtocolBuffer.create().writeString(message).writeOptionalString(permission))
                .build().send();
    }

    public ChannelMessage.Builder messageBuilder() {
        return ChannelMessage.builder().channel(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL);
    }

}
