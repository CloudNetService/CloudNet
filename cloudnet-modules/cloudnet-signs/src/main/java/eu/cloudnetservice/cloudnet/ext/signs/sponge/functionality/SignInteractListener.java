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

package eu.cloudnetservice.cloudnet.ext.signs.sponge.functionality;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.sponge.event.SpongeCloudSignInteractEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;

public class SignInteractListener {

  protected final Object plugin;
  protected final ServiceSignManagement<org.spongepowered.api.block.tileentity.Sign> signManagement;

  public SignInteractListener(Object plugin,
    ServiceSignManagement<org.spongepowered.api.block.tileentity.Sign> signManagement) {
    this.plugin = plugin;
    this.signManagement = signManagement;
  }

  @Listener
  public void handle(InteractBlockEvent event, @First Player player) {
    BlockType blockType = event.getTargetBlock().getState().getType();
    if (blockType.equals(BlockTypes.WALL_SIGN) || blockType.equals(BlockTypes.STANDING_SIGN)) {
      if (event.getTargetBlock().getLocation().isPresent()
        && event.getTargetBlock().getLocation().get().getTileEntity().isPresent()
        && event.getTargetBlock().getLocation().get().getTileEntity()
        .orElse(null) instanceof org.spongepowered.api.block.tileentity.Sign) {
        Sign sign = this.signManagement.getSignAt(
          (org.spongepowered.api.block.tileentity.Sign) event.getTargetBlock().getLocation().get().getTileEntity()
            .get());
        if (sign != null) {
          boolean canConnect = this.signManagement.canConnect(sign, player::hasPermission);

          SpongeCloudSignInteractEvent interactEvent = new SpongeCloudSignInteractEvent(
            Cause.of(EventContext.empty(), this.plugin),
            player, sign, !canConnect);
          Sponge.getEventManager().post(interactEvent);

          if (!interactEvent.isCancelled() && interactEvent.getTarget().isPresent()) {
            this.signManagement.getSignsConfiguration().sendMessage("server-connecting-message",
              m -> player.sendMessage(Text.of(m)),
              m -> m.replace("%server%", interactEvent.getTarget().get().getName()));
            this.getPlayerManager().getPlayerExecutor(player.getUniqueId())
              .connect(interactEvent.getTarget().get().getName());
          }
        }
      }
    }
  }

  protected IPlayerManager getPlayerManager() {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
  }
}
