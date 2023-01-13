/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import eu.cloudnetservice.modules.bridge.platform.fabric.FabricBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedClientConnection;
import java.net.InetSocketAddress;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ServerHandshakePacketListenerImpl.class)
public final class ServerHandshakePacketListenerMixin {

  private static final Gson GSON = new Gson();
  private static final Component IP_INFO_MISSING = Component.literal(
    "If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");

  @Final
  @Shadow
  private Connection connection;

  @Inject(at = @At("HEAD"), method = "handleIntention")
  public void onHandshake(@NonNull ClientIntentionPacket packet, @NonNull CallbackInfo info) {
    // do not try this for pings
    if (!FabricBridgeManagement.DISABLE_CLOUDNET_FORWARDING && packet.getIntention() == ConnectionProtocol.LOGIN) {
      var bridged = (BridgedClientConnection) this.connection;
      // decode the bungee handshake
      var split = packet.getHostName().split("\00");
      if (split.length == 3 || split.length == 4) {
        packet.hostName = split[0];
        // set bridged properties for later use
        bridged.forwardedUniqueId(UUIDTypeAdapter.fromString(split[2]));
        bridged.addr(
          new InetSocketAddress(split[1], ((InetSocketAddress) this.connection.getRemoteAddress()).getPort()));
        // check if properties were supplied
        if (split.length == 4) {
          bridged.forwardedProfile(GSON.fromJson(split[3], Property[].class));
        }
      } else {
        // disconnect will not send the packet - it will just close the channel and set the disconnect reason
        this.connection.send(new ClientboundLoginDisconnectPacket(IP_INFO_MISSING));
        this.connection.disconnect(IP_INFO_MISSING);
      }
    }
  }
}
