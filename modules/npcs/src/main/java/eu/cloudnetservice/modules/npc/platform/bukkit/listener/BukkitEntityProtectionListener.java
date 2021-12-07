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

package eu.cloudnetservice.modules.npc.platform.bukkit.listener;

import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.jetbrains.annotations.NotNull;

public final class BukkitEntityProtectionListener implements Listener {

  private final BukkitPlatformNPCManagement management;

  public BukkitEntityProtectionListener(@NotNull BukkitPlatformNPCManagement management) {
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull EntityDamageEvent event) {
    this.management.getTrackedEntities().values().stream()
      .filter(PlatformSelectorEntity::isSpawned)
      .filter(npc -> {
        int eid = event.getEntity().getEntityId();
        return npc.getEntityId() == eid || npc.getInfoLineEntityIds().contains(eid);
      })
      .findFirst()
      .ifPresent($ -> {
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
      });
  }

  @EventHandler
  public void handle(@NotNull PlayerArmorStandManipulateEvent event) {
    this.management.getTrackedEntities().values().stream()
      .filter(PlatformSelectorEntity::isSpawned)
      .filter(npc -> npc.getInfoLineEntityIds().contains(event.getRightClicked().getEntityId()))
      .findFirst()
      .ifPresent($ -> event.setCancelled(true));
  }
}
