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

package eu.cloudnetservice.cloudnet.ext.npcs.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;
import java.util.Set;

public class CloudNetNPCMessageListener {

  private final CloudNetNPCModule npcModule;

  public CloudNetNPCMessageListener(CloudNetNPCModule npcModule) {
    this.npcModule = npcModule;
  }

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
    event.getNode().sendCustomChannelMessage(
      NPCConstants.NPC_CHANNEL_NAME,
      NPCConstants.NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE,
      new JsonDocument()
        .append("npcConfiguration", this.npcModule.getNPCConfiguration())
        .append("npcs", this.npcModule.getCachedNPCs())
    );
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (event.getChannel().equalsIgnoreCase(NPCConstants.NPC_CHANNEL_NAME) && event.getMessage() != null) {

      switch (event.getMessage().toLowerCase()) {
        case NPCConstants.NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE: {
          NPCConfiguration npcConfiguration = event.getData().get("npcConfiguration", NPCConfiguration.class);

          this.npcModule.setNPCConfiguration(npcConfiguration);
          this.npcModule.saveNPCConfiguration();

          Set<CloudNPC> npcs = event.getData().get("npcs", NPCConstants.NPC_COLLECTION_TYPE);
          if (npcs != null) {
            this.npcModule.setCachedNPCs(npcs);
            this.npcModule.saveNPCs(npcs);
          }
        }
        break;
        case NPCConstants.NPC_CHANNEL_ADD_NPC_MESSAGE: {
          CloudNPC npc = event.getData().get("npc", CloudNPC.class);

          if (npc != null) {
            this.npcModule.addNPC(npc);
          }
        }
        break;
        case NPCConstants.NPC_CHANNEL_REMOVE_NPC_MESSAGE: {
          CloudNPC npc = event.getData().get("npc", CloudNPC.class);

          if (npc != null) {
            this.npcModule.removeNPC(npc);
          }
        }
        break;
        case NPCConstants.NPC_CHANNEL_GET_CONFIGURATION_MESSAGE: {
          event.setJsonResponse(JsonDocument.newDocument("npcConfiguration", this.npcModule.getNPCConfiguration()));
        }
        break;
        case NPCConstants.NPC_CHANNEL_GET_NPCS_MESSAGE: {
          event.setJsonResponse(JsonDocument.newDocument("npcs", this.npcModule.getCachedNPCs()));
        }
        break;
        default:
          break;
      }

    }

  }


}
