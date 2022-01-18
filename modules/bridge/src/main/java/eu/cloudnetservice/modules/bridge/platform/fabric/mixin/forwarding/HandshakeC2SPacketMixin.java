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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.SERVER)
@Mixin(HandshakeC2SPacket.class)
public final class HandshakeC2SPacketMixin {

  @Redirect(
    at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;readString(I)Ljava/lang/String;"),
    method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V"
  )
  public String read(PacketByteBuf buf, int amount) {
    return buf.readString(Short.MAX_VALUE);
  }
}
