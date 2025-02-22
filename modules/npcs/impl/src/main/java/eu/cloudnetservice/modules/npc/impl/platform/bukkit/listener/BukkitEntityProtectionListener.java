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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.listener;

import eu.cloudnetservice.modules.npc.impl.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

@Singleton
public final class BukkitEntityProtectionListener implements Listener {

  private final BukkitPlatformNPCManagement management;

  @Inject
  public BukkitEntityProtectionListener(@NonNull BukkitPlatformNPCManagement management) {
    this.management = management;
  }

  @EventHandler
  public void handle(@NonNull EntityDamageEvent event) {
    this.management.trackedEntities().values().stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(npc -> {
        var eid = event.getEntity().getEntityId();
        return npc.entityId() == eid || npc.infoLineEntityIds().contains(eid);
      })
      .findFirst()
      .ifPresent($ -> {
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
      });
  }

  @EventHandler
  public void handle(@NonNull PlayerArmorStandManipulateEvent event) {
    this.management.trackedEntities().values().stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(npc -> npc.infoLineEntityIds().contains(event.getRightClicked().getEntityId()))
      .findFirst()
      .ifPresent($ -> event.setCancelled(true));
  }
}
