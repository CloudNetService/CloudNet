/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric;

import eu.cloudnetservice.modules.bridge.impl.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.impl.platform.fabric.access.CustomPayloadAccessor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
    var reason = this.vanillaFromAdventure(message);
    this.forEach(player -> player.connection.disconnect(reason));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    // we're unable to check the permission for the player
    var text = this.vanillaFromAdventure(message);
    this.forEach(player -> player.sendSystemMessage(text, false));
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    var payload = new DiscardedPayload(ResourceLocation.parse(key));
    ((CustomPayloadAccessor) (Object) payload).cloudnet_bridge$setData(Unpooled.wrappedBuffer(data));
    this.forEach(player -> player.connection.send(new ClientboundCustomPayloadPacket(payload)));
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> {
      var stack = player.createCommandSourceStack();
      player.server.getCommands().performPrefixedCommand(stack, command);
    });
  }

  private @NonNull net.minecraft.network.chat.Component vanillaFromAdventure(@NonNull Component adventure) {
    var adventureAsJson = GsonComponentSerializer.gson().serializeToTree(adventure);
    var vanilla = net.minecraft.network.chat.Component.Serializer.fromJson(adventureAsJson, RegistryAccess.EMPTY);
    return vanilla == null ? net.minecraft.network.chat.Component.empty() : vanilla;
  }
}
