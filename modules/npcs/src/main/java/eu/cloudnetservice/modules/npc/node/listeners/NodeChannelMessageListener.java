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

package eu.cloudnetservice.modules.npc.node.listeners;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.platform.PlatformNPCManagement;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class NodeChannelMessageListener {

  private final AbstractNPCManagement management;

  public NodeChannelMessageListener(@NotNull AbstractNPCManagement management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(AbstractNPCManagement.NPC_CHANNEL_NAME) && event.getMessage() != null) {
      switch (event.getMessage()) {
        // deletes an existing npc
        case PlatformNPCManagement.NPC_DELETE -> this.management.deleteNPC(
            event.getContent().readObject(WorldPosition.class));

        // creates a new npc
        case PlatformNPCManagement.NPC_CREATE -> this.management.createNPC(event.getContent().readObject(NPC.class));

        // bulk deletes all npcs of a given group
        case PlatformNPCManagement.NPC_BULK_DELETE -> {
          var deleted = this.management.deleteAllNPCs(event.getContent().readString());
          event.setBinaryResponse(DataBuf.empty().writeInt(deleted));
        }
        // deletes all npcs
        case PlatformNPCManagement.NPC_ALL_DELETE -> event.setBinaryResponse(
            DataBuf.empty().writeInt(this.management.deleteAllNPCs()));

        // get all npcs of a specific group
        case PlatformNPCManagement.NPC_GET_NPCS_BY_GROUP -> {
          var npcs = this.management.getNPCs(event.getContent().readObject(String[].class));
          event.setBinaryResponse(DataBuf.empty().writeObject(npcs));
        }
        // request of a service for the npc config
        case PlatformNPCManagement.NPC_REQUEST_CONFIG -> event.setBinaryResponse(
            DataBuf.empty().writeObject(this.management.getNPCConfiguration()));

        // set the npc config
        case PlatformNPCManagement.NPC_SET_CONFIG -> this.management.setNPCConfiguration(
            event.getContent().readObject(NPCConfiguration.class));

        // not our business
        default -> {
        }
      }
    }
  }
}
