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
        case AbstractNPCManagement.NPC_CREATED -> {
          var npc = event.content().readObject(NPC.class);
          this.npcManagement.handleInternalNPCCreate(npc);
        }
        case AbstractNPCManagement.NPC_DELETED -> {
          var position = event.content().readObject(WorldPosition.class);
          this.npcManagement.handleInternalNPCRemove(position);
        }
        case AbstractNPCManagement.NPC_BULK_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          positions.forEach(this.npcManagement::handleInternalNPCRemove);
        }
        case AbstractNPCManagement.NPC_CONFIGURATION_UPDATE -> {
          var config = event.content().readObject(NPCConfiguration.class);
          this.npcManagement.handleInternalNPCConfigUpdate(config);
        }
        default -> {
        }
      }
    }
  }
}
