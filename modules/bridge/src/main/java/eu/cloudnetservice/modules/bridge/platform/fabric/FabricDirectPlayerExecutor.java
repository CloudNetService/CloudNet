/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.bridge.platform.fabric;

import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class FabricDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID uniqueId;
  private final Supplier<? extends Collection<ServerPlayerEntity>> players;

  public FabricDirectPlayerExecutor(
    @NonNull UUID uniqueId,
    @NonNull Supplier<? extends Collection<ServerPlayerEntity>> players
  ) {
    this.uniqueId = uniqueId;
    this.players = players;
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    // no-op
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToFallback() {
    // no-op
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void kick(@NonNull Component message) {
    var reason = new LiteralText(LegacyComponentSerializer.legacySection().serialize(message));
    this.players.get().forEach(player -> player.networkHandler.disconnect(reason));
  }

  @Override
  public void sendMessage(@NonNull Component message) {
    var text = new LiteralText(LegacyComponentSerializer.legacySection().serialize(message));
    this.players.get().forEach(player -> player.sendMessage(text, false));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    // we're unable to check the permission for the player
    this.sendMessage(message);
  }

  @Override
  public void sendPluginMessage(@NonNull String tag, byte[] data) {
    var identifier = new Identifier(tag);
    this.players.get().forEach(player -> player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
      identifier,
      new PacketByteBuf(Unpooled.wrappedBuffer(data)))));
  }

  @Override
  public void dispatchProxyCommand(@NonNull String command) {
    this.players.get().forEach(player -> player.networkHandler.executeCommand(command));
  }
}
