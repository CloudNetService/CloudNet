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

import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(PlayerList.class)
public final class PlayerListMixin {

  @Final
  @Shadow
  private MinecraftServer server;

  @Inject(
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
    ),
    method = "placeNewPlayer"
  )
  public void onJoin(
    @NonNull Connection connection,
    @NonNull ServerPlayer serverPlayer,
    @NonNull CommonListenerCookie commonListenerCookie,
    @NonNull CallbackInfo callbackInfo
  ) {
    if (this.server instanceof BridgedServer bridgedServer) {
      bridgedServer.injectionHolder().serverPlatformHelper().sendChannelMessageLoginSuccess(
        serverPlayer.getUUID(),
        bridgedServer.management().createPlayerInformation(serverPlayer));
      // update the service info instantly as the player is registered now
      bridgedServer.injectionHolder().serviceInfoHolder().publishServiceInfoUpdate();
    }
  }

  @Inject(at = @At("TAIL"), method = "remove")
  public void onDisconnect(@NonNull ServerPlayer player, @NonNull CallbackInfo info) {
    if (this.server instanceof BridgedServer bridgedServer) {
      bridgedServer.injectionHolder().serverPlatformHelper().sendChannelMessageDisconnected(
        player.getUUID(),
        bridgedServer.management().ownNetworkServiceInfo());
      // update the service info instantly as the player is unregistered now
      bridgedServer.injectionHolder().serviceInfoHolder().publishServiceInfoUpdate();
    }
  }
}
