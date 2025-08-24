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
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.impl.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.impl.platform.PlatformNPCManagement;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;

public final class NodeChannelMessageListener {

  private static final Type STRING_COLLECTION = TypeFactory.parameterizedClass(Collection.class, String.class);

  private final NPCManagement management;

  public NodeChannelMessageListener(@NonNull NPCManagement management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractNPCManagement.NPC_CHANNEL_NAME)) {
      switch (event.message()) {
        case PlatformNPCManagement.NPC_DELETE -> {
          var npcPosition = event.content().readObject(WorldPosition.class);
          this.management.deleteNPC(npcPosition);
        }
        case PlatformNPCManagement.NPC_CREATE -> {
          var npc = event.content().readObject(NPC.class);
          this.management.createNPC(npc);
        }
        case PlatformNPCManagement.NPC_BULK_DELETE -> {
          var group = event.content().readString();
          var deleted = this.management.deleteAllNPCs(group);
          event.binaryResponse(DataBuf.empty().writeInt(deleted));
        }
        case PlatformNPCManagement.NPC_ALL_DELETE -> {
          var deleted = this.management.deleteAllNPCs();
          event.binaryResponse(DataBuf.empty().writeInt(deleted));
        }
        case PlatformNPCManagement.NPC_GET_NPCS_BY_GROUP -> {
          Collection<String> groups = event.content().readObject(STRING_COLLECTION);
          var npcs = this.management.npcs(groups);
          event.binaryResponse(DataBuf.empty().writeObject(npcs));
        }
        case PlatformNPCManagement.NPC_REQUEST_CONFIG -> {
          var config = this.management.npcConfiguration();
          event.binaryResponse(DataBuf.empty().writeObject(config));
        }
        case PlatformNPCManagement.NPC_SET_CONFIG -> {
          var config = event.content().readObject(NPCConfiguration.class);
          this.management.npcConfiguration(config);
        }
        default -> {
        }
      }
    }
  }
}
