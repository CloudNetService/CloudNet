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

package eu.cloudnetservice.modules.npc.impl.node.listeners;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.impl.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.impl.platform.PlatformNPCManagement;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;

public final class NodeChannelMessageListener {

  private static final Type STRING_COLLECTION = TypeFactory.parameterizedClass(Collection.class, String.class);

  private final AbstractNPCManagement management;

  public NodeChannelMessageListener(@NonNull AbstractNPCManagement management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractNPCManagement.NPC_CHANNEL_NAME)) {
      switch (event.message()) {
        // deletes an existing npc
        case PlatformNPCManagement.NPC_DELETE -> this.management.deleteNPC(
          event.content().readObject(WorldPosition.class));

        // creates a new npc
        case PlatformNPCManagement.NPC_CREATE -> this.management.createNPC(event.content().readObject(NPC.class));

        // bulk deletes all npcs of a given group
        case PlatformNPCManagement.NPC_BULK_DELETE -> {
          var deleted = this.management.deleteAllNPCs(event.content().readString());
          event.binaryResponse(DataBuf.empty().writeInt(deleted));
        }
        // deletes all npcs
        case PlatformNPCManagement.NPC_ALL_DELETE -> event.binaryResponse(
          DataBuf.empty().writeInt(this.management.deleteAllNPCs()));

        // get all npcs of a specific group
        case PlatformNPCManagement.NPC_GET_NPCS_BY_GROUP -> {
          var npcs = this.management.npcs(event.content().readObject(STRING_COLLECTION));
          event.binaryResponse(DataBuf.empty().writeObject(npcs));
        }
        // request of a service for the npc config
        case PlatformNPCManagement.NPC_REQUEST_CONFIG -> event.binaryResponse(
          DataBuf.empty().writeObject(this.management.npcConfiguration()));

        // set the npc config
        case PlatformNPCManagement.NPC_SET_CONFIG -> this.management.npcConfiguration(
          event.content().readObject(NPCConfiguration.class));

        // not our business
        default -> {
        }
      }
    }
  }
}
