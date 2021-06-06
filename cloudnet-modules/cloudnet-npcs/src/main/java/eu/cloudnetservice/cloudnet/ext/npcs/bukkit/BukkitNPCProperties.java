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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;

import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import java.util.Map;
import org.bukkit.inventory.Inventory;

public class BukkitNPCProperties {

  private final CloudNPC holder;

  private final int entityId;

  private final Inventory inventory;

  private final Map<Integer, String> serverSlots;

  public BukkitNPCProperties(CloudNPC holder, int entityId, Inventory inventory, Map<Integer, String> serverSlots) {
    this.holder = holder;
    this.entityId = entityId;
    this.inventory = inventory;
    this.serverSlots = serverSlots;
  }

  public CloudNPC getHolder() {
    return this.holder;
  }

  public int getEntityId() {
    return this.entityId;
  }

  public Inventory getInventory() {
    return this.inventory;
  }

  public Map<Integer, String> getServerSlots() {
    return this.serverSlots;
  }

}
