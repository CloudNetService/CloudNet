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

package eu.cloudnetservice.modules.npc;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import java.util.Collection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NPCManagement {

  @Nullable NPC npcAt(@NotNull WorldPosition position);

  void createNPC(@NotNull NPC npc);

  void deleteNPC(@NotNull NPC npc);

  void deleteNPC(@NotNull WorldPosition position);

  int deleteAllNPCs(@NotNull String group);

  int deleteAllNPCs();

  @NotNull Collection<NPC> npcs();

  @NotNull Collection<NPC> npcs(@NotNull String[] groups);

  @NotNull NPCConfiguration npcConfiguration();

  void npcConfiguration(@NotNull NPCConfiguration configuration);

  // Internal methods

  @Internal
  void registerToServiceRegistry();

  @Internal
  void unregisterFromServiceRegistry();

  @Internal
  void handleInternalNPCCreate(@NotNull NPC npc);

  @Internal
  void handleInternalNPCRemove(@NotNull WorldPosition position);

  @Internal
  void handleInternalNPCConfigUpdate(@NotNull NPCConfiguration configuration);
}
