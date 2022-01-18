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

package eu.cloudnetservice.modules.bridge.platform.fabric.mixin.handling;

import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.platform.helper.ServerPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(PlayerManager.class)
public final class PlayerManagerMixin {

  @Final
  @Shadow
  private MinecraftServer server;

  @Inject(
    at = @At(
      by = 2,
      ordinal = 0,
      value = "FIELD",
      shift = Shift.BY,
      opcode = Opcodes.GETFIELD,
      target = "Lnet/minecraft/server/PlayerManager;players:Ljava/util/List;"
    ),
    method = "onPlayerConnect"
  )
  public void onJoin(ClientConnection con, ServerPlayerEntity player, CallbackInfo info) {
    ServerPlatformHelper.sendChannelMessageLoginSuccess(
      player.getUuid(),
      ((BridgedServer) this.server).management().ownNetworkServiceInfo());
    // update the service info instantly as the player is registered now
    Wrapper.instance().publishServiceInfoUpdate();
  }

  @Inject(at = @At("TAIL"), method = "remove")
  public void onDisconnect(ServerPlayerEntity player, CallbackInfo info) {
    ServerPlatformHelper.sendChannelMessageDisconnected(
      player.getUuid(),
      ((BridgedServer) this.server).management().ownNetworkServiceInfo());
    // update the service info instantly as the player is unregistered now
    Wrapper.instance().publishServiceInfoUpdate();
  }
}
