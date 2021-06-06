package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultPlayerManager implements IPlayerManager {

  private static final PlayerExecutor GLOBAL_PLAYER_EXECUTOR = new DefaultPlayerExecutor(
    DefaultPlayerExecutor.GLOBAL_ID);

  @Override
  public @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId) {
    return new DefaultPlayerExecutor(uniqueId);
  }

  @Override
  public PlayerExecutor getGlobalPlayerExecutor() {
    return GLOBAL_PLAYER_EXECUTOR;
  }

  @Override
  public void broadcastMessage(@NotNull String message) {
    this.getGlobalPlayerExecutor().sendChatMessage(message);
  }

  @Override
  public void broadcastMessage(@NotNull String message, @Nullable String permission) {
    this.getGlobalPlayerExecutor().sendChatMessage(message, permission);
  }

  public ChannelMessage.Builder messageBuilder() {
    return ChannelMessage.builder().channel(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL);
  }

}
