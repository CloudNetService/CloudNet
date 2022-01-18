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
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.FabricBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import java.util.Collection;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
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
  private PlatformBridgeManagement<ServerPlayerEntity, NetworkPlayerServerInfo> management;

  @Shadow
  public abstract PlayerManager getPlayerManager();

  @Shadow
  public abstract String getServerMotd();

  @Inject(
    at = @At(
      ordinal = 0,
      value = "INVOKE",
      target = "Lnet/minecraft/server/MinecraftServer;setFavicon(Lnet/minecraft/server/ServerMetadata;)V"
    ),
    method = "runServer"
  )
  public void beforeTickLoopStart(CallbackInfo callbackInfo) {
    // the server now booted completely
    this.management = new FabricBridgeManagement(this);
    management.registerServices(Wrapper.instance().servicesRegistry());
    management.postInit();
  }

  @Override
  public int maxPlayers() {
    return this.getPlayerManager().getMaxPlayerCount();
  }

  @Override
  public int playerCount() {
    return this.getPlayerManager().getCurrentPlayerCount();
  }

  @Override
  public String motd() {
    return this.getServerMotd();
  }

  @Override
  public Collection<ServerPlayerEntity> players() {
    return this.getPlayerManager().getPlayerList();
  }

  @Override
  public ServerPlayerEntity player(UUID uniqueId) {
    return this.getPlayerManager().getPlayer(uniqueId);
  }

  @Override
  public PlatformBridgeManagement<ServerPlayerEntity, NetworkPlayerServerInfo> management() {
    return this.management;
  }
}
