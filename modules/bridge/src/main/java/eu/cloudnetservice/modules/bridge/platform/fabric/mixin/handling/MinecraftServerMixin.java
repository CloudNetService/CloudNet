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

package eu.cloudnetservice.modules.bridge.platform.fabric.mixin.handling;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.FabricBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedClientConnection;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServerCommonPacketListener;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.FabricInjectionHolder;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements BridgedServer {

  @Unique
  private PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> management;
  @Unique
  private FabricInjectionHolder injectionHolder;

  @Shadow
  public abstract PlayerList getPlayerList();

  @Shadow
  public abstract String getMotd();

  @Inject(
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;"
    ),
    method = "runServer"
  )
  public void cloudnet_bridge$beforeTickLoopStart(CallbackInfo callbackInfo) {
    // the server now booted completely
    this.injectionHolder = InjectionLayer.ext().instance(FabricInjectionHolder.class);
    // we have to create the management ourselves as we can't inject the server
    this.management = new FabricBridgeManagement(
      this,
      this.injectionHolder.rpcFactory(),
      this.injectionHolder.eventManager(),
      this.injectionHolder.networkClient(),
      this.injectionHolder.taskProvider(),
      this.injectionHolder.serviceHelper(),
      this.injectionHolder.serviceInfoHolder(),
      this.injectionHolder.serviceProvider(),
      this.injectionHolder.wrapperConfiguration());
    this.management.registerServices(this.injectionHolder.serviceRegistry());
    this.management.postInit();
  }

  @Override
  public int cloudnet_bridge$maxPlayers() {
    return this.getPlayerList().getMaxPlayers();
  }

  @Override
  public int cloudnet_bridge$playerCount() {
    return this.cloudnet_bridge$players().size();
  }

  @Override
  public @NonNull String cloudnet_bridge$motd() {
    return this.getMotd();
  }

  @Override
  public @NonNull Collection<ServerPlayer> cloudnet_bridge$players() {
    return this.getPlayerList().getPlayers().stream()
      .map(player -> this.cloudnet_bridge$player(player.getUUID()))
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public @Nullable ServerPlayer cloudnet_bridge$player(@NonNull UUID uniqueId) {
    var player = this.getPlayerList().getPlayer(uniqueId);
    if (player == null) {
      return null;
    }

    var connection = (BridgedServerCommonPacketListener) player.connection;
    var bridgeConnection = (BridgedClientConnection) connection.cloudnet_bridge$connection();
    return bridgeConnection.cloudnet_bridge$intentionPacketSeen() ? player : null;
  }

  @Override
  public @NonNull PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> cloudnet_bridge$management() {
    return this.management;
  }

  @Override
  public @NonNull FabricInjectionHolder cloudnet_bridge$injectionHolder() {
    return this.injectionHolder;
  }
}
