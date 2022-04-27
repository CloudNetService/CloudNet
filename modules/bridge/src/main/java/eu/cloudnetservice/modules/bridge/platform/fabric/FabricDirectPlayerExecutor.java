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
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FabricDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<ServerPlayer> {

  public FabricDirectPlayerExecutor(
    @NonNull UUID uniqueId,
    @NonNull Supplier<? extends Collection<ServerPlayer>> players
  ) {
    super(uniqueId, players);
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
    var reason = new TextComponent(LegacyComponentSerializer.legacySection().serialize(message));
    this.forEach(player -> player.connection.disconnect(reason));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    // we're unable to check the permission for the player
    var text = new TextComponent(LegacyComponentSerializer.legacySection().serialize(message));
    this.forEach(player -> player.sendMessage(text, Util.NIL_UUID));
  }

  @Override
  public void sendPluginMessage(@NonNull String channel, byte @NotNull [] data) {
    var identifier = new ResourceLocation(channel);
    this.forEach(player -> player.connection.send(new ClientboundCustomPayloadPacket(
      identifier,
      new FriendlyByteBuf(Unpooled.wrappedBuffer(data)))));
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> player.connection.handleCommand(command));
  }
}
