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
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPC.ClickAction;
import eu.cloudnetservice.modules.npc.NPC.ProfileProperty;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration.ItemLayoutHolder;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.configuration.LabyModEmoteConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCPoolOptions;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CloudNetNPCModule extends DriverModule {

  protected static final String DATABASE_NAME = "cloudnet_npcs";

  @ModuleTask(order = Byte.MAX_VALUE)
  public void convertConfiguration() {
    if (Files.exists(this.getConfigPath())) {
      var old = JsonDocument.newDocument(this.getConfigPath()).get("config", NPCConfiguration.class);
      if (old != null) {
        var newEntries = old.getConfigurations().stream()
          .map(entry -> eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry.builder()
            .targetGroup(entry.getTargetGroup())
            .infoLineDistance(entry.getInfoLineDistance())
            .knockbackDistance(entry.getKnockbackDistance())
            .knockbackStrength(entry.getKnockbackStrength())
            .emoteConfiguration(LabyModEmoteConfiguration.builder()
              .emoteIds(entry.getLabyModEmotes().getEmoteIds())
              .onJoinEmoteIds(entry.getLabyModEmotes().getOnJoinEmoteIds())
              .onKnockbackEmoteIds(entry.getLabyModEmotes().getOnKnockbackEmoteIds())
              .minEmoteDelayTicks(entry.getLabyModEmotes().getMinEmoteDelayTicks())
              .maxEmoteDelayTicks(entry.getLabyModEmotes().getMaxEmoteDelayTicks())
              .build())
            .inventoryConfiguration(InventoryConfiguration.builder()
              .defaultItems(new ItemLayoutHolder(
                this.convertItemLayout(entry.getEmptyItem()),
                this.convertItemLayout(entry.getOnlineItem()),
                this.convertItemLayout(entry.getFullItem())))
              .fixedItems(entry.getInventoryLayout().entrySet().stream()
                .map(mapEntry -> new Pair<>(mapEntry.getKey(), this.convertItemLayout(mapEntry.getValue())))
                .collect(Collectors.toMap(Pair::first, Pair::second)))
              .showFullServices(entry.isShowFullServices())
              .inventorySize(entry.getInventorySize())
              .build())
            .npcPoolOptions(NPCPoolOptions.builder()
              .tabListRemoveTicks(entry.getNPCTabListRemoveTicks())
              .build())
            .build())
          .collect(Collectors.toSet());
        // write the new config
        JsonDocument
          .newDocument(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder()
            .entries(newEntries)
            .build())
          .write(this.getConfigPath());
      }
    }

    // convert the old database
    Database db = CloudNet.getInstance().getDatabaseProvider().getDatabase("cloudNet_module_configuration");
    var npcStore = db.get("npc_store");
    if (npcStore != null) {
      Collection<CloudNPC> theOldOnes = npcStore.get("npcs", NPCConstants.NPC_COLLECTION_TYPE);
      // remove the old entries
      db.delete("npc_store");
      if (theOldOnes != null) {
        // get the new database
        Database target = CloudNet.getInstance().getDatabaseProvider().getDatabase(DATABASE_NAME);
        // convert the old entries
        theOldOnes.stream()
          .map(npc -> NPC.builder()
            .profileProperties(npc.getProfileProperties().stream()
              .map(property -> new ProfileProperty(property.getName(), property.getValue(), property.getSignature()))
              .collect(Collectors.toSet()))
            .location(npc.getPosition())
            .displayName(npc.getDisplayName())
            .infoLines(Collections.singletonList(npc.getInfoLine()))
            .targetGroup(npc.getTargetGroup())
            .items(ImmutableMap.of(0, npc.getItemInHand()))
            .lookAtPlayer(npc.isLookAtPlayer())
            .imitatePlayer(npc.isImitatePlayer())
            .rightClickAction(ClickAction.valueOf(npc.getRightClickAction().name()))
            .leftClickAction(ClickAction.valueOf(npc.getLeftClickAction().name()))
            .build())
          .forEach(npc -> target.insert(
            NodeNPCManagement.getDocumentKey(npc.getLocation()),
            JsonDocument.newDocument(npc)));
      }
    }
  }

  @ModuleTask
  public void initModule() {
    var config = this.loadConfig();
    Database database = CloudNet.getInstance().getDatabaseProvider().getDatabase(DATABASE_NAME);
    // management init
    var management = new NodeNPCManagement(
      config,
      database,
      this.getConfigPath(),
      CloudNet.getInstance().getEventManager());
    management.registerToServiceRegistry();
  }

  private @NotNull eu.cloudnetservice.modules.npc.configuration.NPCConfiguration loadConfig() {
    if (Files.notExists(this.getConfigPath())) {
      JsonDocument
        .newDocument(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder().build())
        .write(this.getConfigPath());
    }
    // load the config
    return JsonDocument.newDocument(this.getConfigPath())
      .toInstanceOf(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.class);
  }

  private @NotNull ItemLayout convertItemLayout(
    @NotNull eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry.ItemLayout oldLayout
  ) {
    return ItemLayout.builder()
      .material(oldLayout.getMaterial())
      .subId(oldLayout.getSubId())
      .lore(oldLayout.getLore())
      .displayName(oldLayout.getDisplayName())
      .build();
  }
}
