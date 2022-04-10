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

package eu.cloudnetservice.modules.signs.platform.nukkit.functionality;

import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import eu.cloudnetservice.modules.signs.platform.nukkit.event.NukkitCloudSignInteractEvent;
import lombok.NonNull;

public class SignInteractListener implements Listener {

  protected final PlatformSignManagement<BlockEntitySign> signManagement;

  public SignInteractListener(PlatformSignManagement<BlockEntitySign> signManagement) {
    this.signManagement = signManagement;
  }

  @EventHandler
  public void handle(PlayerInteractEvent event) {
    var entry = this.signManagement.applicableSignConfigurationEntry();
    if (entry != null
      && event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
      && event.getBlock() != null) {
      var blockEntity = event.getBlock().getLevel().getBlockEntity(event.getBlock().getLocation());
      if (blockEntity instanceof BlockEntitySign entitySign) {
        var sign = this.signManagement.signAt(entitySign, entry.targetGroup());
        if (sign != null) {
          var canConnect = this.signManagement.canConnect(sign, event.getPlayer()::hasPermission);

          var interactEvent = new NukkitCloudSignInteractEvent(event.getPlayer(), sign, !canConnect);
          Server.getInstance().getPluginManager().callEvent(interactEvent);

          if (!interactEvent.isCancelled()) {
            interactEvent.target().ifPresent(service -> {
              this.playerManager().playerExecutor(event.getPlayer().getUniqueId()).connect(service.name());
            });
          }
        }
      }
    }
  }

  protected @NonNull PlayerManager playerManager() {
    return ServiceRegistry.first(PlayerManager.class);
  }
}
