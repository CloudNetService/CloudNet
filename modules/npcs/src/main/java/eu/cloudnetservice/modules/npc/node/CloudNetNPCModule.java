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

package eu.cloudnetservice.modules.npc.node;

import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPC.ClickAction;
import eu.cloudnetservice.modules.npc.NPC.ProfileProperty;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration.ItemLayoutHolder;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.configuration.LabyModEmoteConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCPoolOptions;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.NonNull;

public class CloudNetNPCModule extends DriverModule {

  protected static final String DATABASE_NAME = "cloudnet_npcs";

  @ModuleTask(order = Byte.MAX_VALUE)
  public void convertConfiguration() {
    if (Files.exists(this.configPath())) {
      var old = JsonDocument.newDocument(this.configPath()).get("config", NPCConfiguration.class);
      if (old != null) {
        var newEntries = old.configurations().stream()
          .map(entry -> eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry.builder()
            .targetGroup(entry.targetGroup())
            .infoLineDistance(entry.infoLineDistance())
            .knockbackDistance(entry.knockbackDistance())
            .knockbackStrength(entry.knockbackStrength())
            .emoteConfiguration(LabyModEmoteConfiguration.builder()
              .emoteIds(entry.labyModEmotes().emoteIds())
              .onJoinEmoteIds(entry.labyModEmotes().onJoinEmoteIds())
              .onKnockbackEmoteIds(entry.labyModEmotes().onKnockbackEmoteIds())
              .minEmoteDelayTicks(entry.labyModEmotes().minEmoteDelayTicks())
              .maxEmoteDelayTicks(entry.labyModEmotes().maxEmoteDelayTicks())
              .build())
            .inventoryConfiguration(InventoryConfiguration.builder()
              .defaultItems(new ItemLayoutHolder(
                this.convertItemLayout(entry.emptyItem()),
                this.convertItemLayout(entry.onlineItem()),
                this.convertItemLayout(entry.fullItem())))
              .fixedItems(entry.inventoryLayout().entrySet().stream()
                .map(mapEntry -> new Pair<>(mapEntry.getKey(), this.convertItemLayout(mapEntry.getValue())))
                .collect(Collectors.toMap(Pair::first, Pair::second)))
              .showFullServices(entry.showFullServices())
              .inventorySize(entry.inventorySize())
              .build())
            .npcPoolOptions(NPCPoolOptions.builder()
              .tabListRemoveTicks(entry.npcTabListRemoveTicks())
              .build())
            .build())
          .collect(Collectors.toSet());
        // write the new config
        JsonDocument
          .newDocument(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder()
            .entries(newEntries)
            .build())
          .write(this.configPath());
      }
    }

    // convert the old database
    Database db = CloudNet.instance().databaseProvider().database("cloudNet_module_configuration");
    var npcStore = db.get("npc_store");
    if (npcStore != null) {
      Collection<CloudNPC> theOldOnes = npcStore.get("npcs", NPCConstants.NPC_COLLECTION_TYPE);
      // remove the old entries
      db.delete("npc_store");
      if (theOldOnes != null) {
        // get the new database
        Database target = CloudNet.instance().databaseProvider().database(DATABASE_NAME);
        // convert the old entries
        theOldOnes.stream()
          .map(npc -> NPC.builder()
            .profileProperties(npc.profileProperties().stream()
              .map(property -> new ProfileProperty(property.name(), property.value(), property.signature()))
              .collect(Collectors.toSet()))
            .location(npc.position())
            .displayName(npc.displayName())
            .infoLines(Collections.singletonList(npc.infoLine()))
            .targetGroup(npc.targetGroup())
            .items(ImmutableMap.of(0, npc.itemInHand()))
            .lookAtPlayer(npc.lookAtPlayer())
            .imitatePlayer(npc.imitatePlayer())
            .rightClickAction(ClickAction.valueOf(npc.rightClickAction().name()))
            .leftClickAction(ClickAction.valueOf(npc.leftClickAction().name()))
            .build())
          .forEach(npc -> target.insert(
            NodeNPCManagement.documentKey(npc.location()),
            JsonDocument.newDocument(npc)));
      }
    }
  }

  @ModuleTask
  public void initModule() {
    var config = this.loadConfig();
    Database database = CloudNet.instance().databaseProvider().database(DATABASE_NAME);
    // management init
    var management = new NodeNPCManagement(
      config,
      database,
      this.configPath(),
      CloudNet.instance().eventManager());
    management.registerToServiceRegistry();
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    var management = this.serviceRegistry().firstService(NPCManagement.class);
    if (management != null) {
      management.npcConfiguration(this.loadConfig());
    }
  }

  private @NonNull eu.cloudnetservice.modules.npc.configuration.NPCConfiguration loadConfig() {
    if (Files.notExists(this.configPath())) {
      JsonDocument
        .newDocument(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder().build())
        .write(this.configPath());
    }
    // load the config
    return JsonDocument.newDocument(this.configPath())
      .toInstanceOf(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.class);
  }

  private @NonNull ItemLayout convertItemLayout(
    @NonNull eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry.ItemLayout oldLayout
  ) {
    return ItemLayout.builder()
      .material(oldLayout.material())
      .subId(oldLayout.subId())
      .lore(oldLayout.lore())
      .displayName(oldLayout.displayName())
      .build();
  }
}
