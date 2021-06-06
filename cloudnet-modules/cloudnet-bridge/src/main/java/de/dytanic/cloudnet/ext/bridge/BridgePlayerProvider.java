package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class BridgePlayerProvider implements PlayerProvider {

  private final BridgePlayerManager playerManager;
  private final String messageKey;
  private final ProtocolBuffer buffer;

  public BridgePlayerProvider(BridgePlayerManager playerManager, String messageKey, ProtocolBuffer buffer) {
    this.playerManager = playerManager;
    this.messageKey = messageKey;
    this.buffer = buffer;
  }

  @Override
  public @NotNull Collection<? extends ICloudPlayer> asPlayers() {
    return this.asPlayersAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
  }

  @Override
  public @NotNull Collection<UUID> asUUIDs() {
    return this.asUUIDsAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
  }

  @Override
  public @NotNull Collection<String> asNames() {
    return this.asNamesAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
  }

  @Override
  public int count() {
    return this.countAsync().get(5, TimeUnit.SECONDS, -1);
  }

  @Override
  public @NotNull ITask<Collection<? extends ICloudPlayer>> asPlayersAsync() {
    return this.sendQuery("_player").map(message -> message.getBuffer().readObjectCollection(CloudPlayer.class));
  }

  @Override
  public @NotNull ITask<Collection<UUID>> asUUIDsAsync() {
    return this.sendQuery("_uuid").map(message -> message.getBuffer().readUUIDCollection());
  }

  @Override
  public @NotNull ITask<Collection<String>> asNamesAsync() {
    return this.sendQuery("_name").map(message -> message.getBuffer().readStringCollection());
  }

  @Override
  public @NotNull ITask<Integer> countAsync() {
    return this.sendQuery("_count").map(message -> message.getBuffer().readVarInt());
  }

  private ITask<ChannelMessage> sendQuery(String keySuffix) {
    return this.playerManager.messageBuilder()
      .message(this.messageKey + keySuffix)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .buffer(this.buffer)
      .build()
      .sendSingleQueryAsync();
  }

}
