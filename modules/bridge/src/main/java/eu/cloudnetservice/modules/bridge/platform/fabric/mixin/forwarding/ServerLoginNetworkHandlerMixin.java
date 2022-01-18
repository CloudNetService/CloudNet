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

package eu.cloudnetservice.modules.bridge.platform.fabric.mixin.forwarding;

import com.mojang.authlib.GameProfile;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedClientConnection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ServerLoginNetworkHandler.class)
public final class ServerLoginNetworkHandlerMixin {

  @Final
  @Shadow
  public ClientConnection connection;

  @Shadow
  GameProfile profile;

  @Inject(
    at = @At(
      ordinal = 0,
      value = "FIELD",
      shift = Shift.AFTER,
      opcode = Opcodes.PUTFIELD,
      target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;profile:Lcom/mojang/authlib/GameProfile;"
    ),
    method = "onHello"
  )
  private void onHello(LoginHelloC2SPacket packet, CallbackInfo info) {
    var bridged = (BridgedClientConnection) this.connection;
    // update the profile according to the forwarded data
    this.profile = new GameProfile(bridged.forwardedUniqueId(), this.profile.getName());
    for (var property : bridged.forwardedProfile()) {
      this.profile.getProperties().put(property.getName(), property);
    }
  }
}
