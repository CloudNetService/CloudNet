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

package de.dytanic.cloudnet.examples.npc;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class ExampleNPCs {

  // getting the NPCManagement via CloudNet's ServiceRegistry
  private final AbstractNPCManagement npcManagement = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(AbstractNPCManagement.class);

  public void editNPC() {
    this.npcManagement.getCloudNPCS().stream().findFirst().ifPresent(cloudNPC -> {
      cloudNPC.setItemInHand("IRON_SWORD");
      cloudNPC.setDisplayName("§bSurvival Games");

      // updating the NPC on this wrapper instance
      this.npcManagement.addNPC(cloudNPC);
      // or saving the NPC to the database and updating it on the whole cluster
      this.npcManagement.sendNPCAddUpdate(cloudNPC);
    });
  }

  public void addRemoveNPC() {
    // removing the NPC on this wrapper instance
    this.npcManagement.getCloudNPCS().stream().findFirst().ifPresent(this.npcManagement::removeNPC);
    // or removing the NPC on the whole cluster
    this.npcManagement.getCloudNPCS().stream().findFirst().ifPresent(this.npcManagement::sendNPCRemoveUpdate);

    CloudNPC cloudNPC = new CloudNPC(
      UUID.randomUUID(),
      "§bSurvival Games",
      "§8• §7%online_players% of %max_players% players online §8•",
      // adding profile properties to set a skin for the NPC
      Collections.singleton(new CloudNPC.NPCProfileProperty(
        "textures",
        "value",
        "signature"
      )),
      new WorldPosition(
        0D,
        0D,
        0D,
        0f,
        0f,
        "world",
        "Lobby"
      ),
      "SurvivalGames",
      "IRON_SWORD",
      true,
      false
    );

    // adding the NPC on this wrapper instance
    this.npcManagement.addNPC(cloudNPC);
    // or saving the NPC to the database and adding it on the whole cluster
    this.npcManagement.sendNPCAddUpdate(cloudNPC);
  }

  public void editNPCConfiguration() {
    NPCConfigurationEntry npcConfigurationEntry = this.npcManagement.getOwnNPCConfigurationEntry();

    // updating the local NPCConfiguration entry
    npcConfigurationEntry.setShowFullServices(false);
    npcConfigurationEntry.setOnlineItem(new NPCConfigurationEntry.ItemLayout(
      "EMERALD",
      "§a%name%",
      Arrays.asList(
        "§7This service is online! :peepoHappy:",
        "§7%online_players%/%max_players%"
      )
    ));

    // updating the configuration in the whole cluster
    NPCConfiguration.sendNPCConfigurationUpdate(this.npcManagement.getNPCConfiguration());
  }

}
