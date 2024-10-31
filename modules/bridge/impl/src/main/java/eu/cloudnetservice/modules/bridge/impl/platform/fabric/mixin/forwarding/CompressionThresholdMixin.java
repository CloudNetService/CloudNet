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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric.mixin.forwarding;

import eu.cloudnetservice.modules.bridge.impl.platform.fabric.FabricBridgeManagement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.SERVER)
@Mixin(MinecraftServer.class)
public abstract class CompressionThresholdMixin {

  @Inject(at = @At("RETURN"), method = "getCompressionThreshold", cancellable = true)
  public void cloudnet_bridge$getNetworkCompressionThreshold(CallbackInfoReturnable<Integer> returnable) {
    if (!FabricBridgeManagement.DISABLE_CLOUDNET_FORWARDING) {
      // disable the network compression when the server runs behind a proxy
      returnable.setReturnValue(-1);
    }
  }
}
