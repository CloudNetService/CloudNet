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

package eu.cloudnetservice.modules.npc.impl;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import java.util.Collection;
import lombok.NonNull;

public final class SharedChannelMessageListener {

  private final InternalNPCManagement npcManagement;

  public SharedChannelMessageListener(@NonNull InternalNPCManagement npcManagement) {
    this.npcManagement = npcManagement;
  }

  @EventListener
  public void handle(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractNPCManagement.NPC_CHANNEL_NAME)) {
      switch (event.message()) {
        // a new npc was created
        case AbstractNPCManagement.NPC_CREATED -> this.npcManagement.handleInternalNPCCreate(
          event.content().readObject(NPC.class));

        // a npc was deleted
        case AbstractNPCManagement.NPC_DELETED -> this.npcManagement.handleInternalNPCRemove(
          event.content().readObject(WorldPosition.class));

        // multiple npcs were deleted - remove then one by one
        case AbstractNPCManagement.NPC_BULK_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          positions.forEach(this.npcManagement::handleInternalNPCRemove);
        }
        // the npc configuration was updated
        case AbstractNPCManagement.NPC_CONFIGURATION_UPDATE -> this.npcManagement.handleInternalNPCConfigUpdate(
          event.content().readObject(NPCConfiguration.class));

        // not our business
        default -> {
        }
      }
    }
  }
}
