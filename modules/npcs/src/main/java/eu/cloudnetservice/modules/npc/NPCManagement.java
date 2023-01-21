/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface NPCManagement {

  @Nullable NPC npcAt(@NonNull WorldPosition position);

  void createNPC(@NonNull NPC npc);

  void deleteNPC(@NonNull NPC npc);

  void deleteNPC(@NonNull WorldPosition position);

  int deleteAllNPCs(@NonNull String group);

  int deleteAllNPCs();

  @NonNull Collection<NPC> npcs();

  @NonNull Collection<NPC> npcs(@NonNull Collection<String> groups);

  @NonNull NPCConfiguration npcConfiguration();

  void npcConfiguration(@NonNull NPCConfiguration configuration);

  // Internal methods

  @ApiStatus.Internal
  void registerToServiceRegistry(@NonNull ServiceRegistry serviceRegistry);

  @ApiStatus.Internal
  void unregisterFromServiceRegistry(@NonNull ServiceRegistry serviceRegistry);

  @ApiStatus.Internal
  void handleInternalNPCCreate(@NonNull NPC npc);

  @ApiStatus.Internal
  void handleInternalNPCRemove(@NonNull WorldPosition position);

  @ApiStatus.Internal
  void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration);
}
