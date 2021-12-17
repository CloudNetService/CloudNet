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

package eu.cloudnetservice.modules.npc.platform;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.npc.NPC;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface PlatformSelectorEntity<L, P, M, I> {

  void spawn();

  void remove();

  void update();

  void trackService(@NotNull ServiceInfoSnapshot service);

  void stopTrackingService(@NotNull ServiceInfoSnapshot service);

  void handleRightClickAction(@NotNull P player);

  void handleLeftClickAction(@NotNull P player);

  @NotNull I selectorInventory();

  void handleInventoryInteract(@NotNull I inv, @NotNull P player, @NotNull M clickedItem);

  @NotNull NPC npc();

  @NotNull L location();

  int entityId();

  @NotNull Set<Integer> infoLineEntityIds();

  @NotNull String scoreboardRepresentation();

  boolean removeWhenWorldSaving();

  boolean spawned();

  boolean canSpawn();
}
