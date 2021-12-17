/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.ext.signs.platform.sponge.functionality;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.signs.platform.PlatformSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.platform.sponge.event.SpongeCloudSignInteractEvent;
import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.plugin.PluginContainer;

public class SignInteractListener {

  protected final PluginContainer plugin;
  protected final PlatformSignManagement<org.spongepowered.api.block.entity.Sign> signManagement;

  public SignInteractListener(
    @NotNull PluginContainer plugin,
    @NotNull PlatformSignManagement<org.spongepowered.api.block.entity.Sign> signManagement
  ) {
    this.plugin = plugin;
    this.signManagement = signManagement;
  }

  @Listener
  public void handle(@NotNull InteractBlockEvent.Secondary event, @First ServerPlayer player) {
    // easy hack to allow all sign types like acacia_sign, birch_wall_sign etc.
    var key = event.block().state().type().findKey(RegistryTypes.BLOCK_TYPE).orElse(null);
    if (key != null && key.formatted().endsWith("_sign")) {
      if (event.block().location().isPresent()
        && event.block().location().get().blockEntity().isPresent()
        && event.block().location().get().blockEntity().get() instanceof org.spongepowered.api.block.entity.Sign) {
        // get the cloud sign at the position
        var sign = this.signManagement.getSignAt(
          (org.spongepowered.api.block.entity.Sign) event.block().location().get().blockEntity().get());
        if (sign != null) {
          var canConnect = this.signManagement.canConnect(sign, player::hasPermission);

          var interactEvent = new SpongeCloudSignInteractEvent(
            Cause.of(EventContext.builder().from(event.context()).build(), this.plugin),
            player, sign, !canConnect);
          Sponge.eventManager().post(interactEvent);

          if (!interactEvent.isCancelled() && interactEvent.getTarget().isPresent()) {
            this.signManagement.getSignsConfiguration().sendMessage(
              "server-connecting-message",
              m -> player.sendMessage(AdventureSerializerUtil.serialize(m)),
              m -> m.replace("%server%", interactEvent.getTarget().get().name()));
            this.getPlayerManager().getPlayerExecutor(player.uniqueId())
              .connect(interactEvent.getTarget().get().name());
          }
        }
      }
    }
  }

  protected @NotNull IPlayerManager getPlayerManager() {
    return CloudNetDriver.instance().servicesRegistry().firstService(IPlayerManager.class);
  }
}
