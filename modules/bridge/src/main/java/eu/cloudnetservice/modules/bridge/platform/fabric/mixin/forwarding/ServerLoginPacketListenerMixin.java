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

package eu.cloudnetservice.modules.bridge.platform.fabric.mixin.forwarding;

import com.mojang.authlib.GameProfile;
import eu.cloudnetservice.modules.bridge.platform.fabric.FabricBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedClientConnection;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerMixin {

  @Final
  @Shadow
  Connection connection;

  @Shadow
  private GameProfile authenticatedProfile;

  @Inject(at = @At("TAIL"), method = "startClientVerification")
  private void cloudnet_bridge$onAcceptedLogin(@NonNull CallbackInfo callbackInfo) {
    if (!FabricBridgeManagement.DISABLE_CLOUDNET_FORWARDING) {
      var bridged = (BridgedClientConnection) this.connection;
      this.authenticatedProfile = new GameProfile(
        bridged.cloudnet_bridge$forwardedUniqueId(),
        this.authenticatedProfile.getName());
      for (var property : bridged.cloudnet_bridge$forwardedProfile()) {
        this.authenticatedProfile.getProperties().put(property.name(), property);
      }
    }
  }
}
