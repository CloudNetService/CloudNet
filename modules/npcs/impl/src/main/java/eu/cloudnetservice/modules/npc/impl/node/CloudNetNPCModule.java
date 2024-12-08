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

package eu.cloudnetservice.modules.npc.impl.node;

import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.configuration.LabyModEmoteConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCPoolOptions;
import eu.cloudnetservice.modules.npc.impl._deprecated.CloudNPC;
import eu.cloudnetservice.modules.npc.impl._deprecated.NPCConstants;
import eu.cloudnetservice.modules.npc.impl._deprecated.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.impl._deprecated.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.node.command.CommandProvider;
import io.leangen.geantyref.TypeFactory;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class CloudNetNPCModule extends DriverModule {

  public static final String DATABASE_NAME = "cloudnet_npcs";

  @Inject
  public CloudNetNPCModule(@NonNull @Named("module") InjectionLayer<?> layer) {
    layer.installAutoConfigureBindings(this.getClass().getClassLoader(), "npcs");
  }

  @ModuleTask(order = Byte.MAX_VALUE)
  public void convertConfiguration(@NonNull DatabaseProvider databaseProvider) {
    if (Files.exists(this.configPath())) {
      var old = this.readConfig(DocumentFactory.json()).readObject("config", NPCConfiguration.class);
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
              .defaultItems(new InventoryConfiguration.ItemLayoutHolder(
                this.convertItemLayout(entry.emptyItem()),
                this.convertItemLayout(entry.onlineItem()),
                this.convertItemLayout(entry.fullItem()),
                this.convertItemLayout(entry.fullItem())))
              .fixedItems(entry.inventoryLayout().entrySet().stream()
                .map(mapEntry -> new Tuple2<>(mapEntry.getKey(), this.convertItemLayout(mapEntry.getValue())))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2)))
              .inventorySize(entry.inventorySize())
              .build())
            .npcPoolOptions(NPCPoolOptions.builder()
              .tabListRemoveTicks(entry.npcTabListRemoveTicks() > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) entry.npcTabListRemoveTicks())
              .build())
            .build())
          .collect(Collectors.toSet());
        // write the new config
        Document.newJsonDocument()
          .appendTree(eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder()
            .entries(newEntries)
            .build())
          .writeTo(this.configPath());
      } else {
        // we have to read the config manually as we have to upgrade it
        var config = this.readConfig(DocumentFactory.json());
        List<Document> entries = config.readObject(
          "entries",
          TypeFactory.parameterizedClass(List.class, Document.class));

        for (var entry : entries) {
          // check if the ingameLayout is missing - upgrade if its the case
          var inventoryConfig = entry.readDocument("inventoryConfiguration").readMutableDocument("defaultItems");
          if (!inventoryConfig.contains("ingameLayout")) {
            inventoryConfig.append("ingameLayout", ItemLayout.builder()
              .material("REDSTONE")
              .displayName("§7%name%")
              .lore(Arrays.asList(
                "§8● §eIngame",
                "§8● §7%online_players%§8/§7%max_players%",
                "§8● §7%motd%")).build());
          }
        }

        this.writeConfig(config);
      }
    }

    // convert the old database (old h2 databases convert the name to lower case - we need to check both names)
    var db = databaseProvider.database("cloudNet_module_configuration");
    if (db.documentCount() == 0) {
      db = databaseProvider.database("cloudnet_module_configuration");
    }

    // get the npc_store field of the database entry
    var npcStore = db.get("npc_store");
    if (npcStore != null) {
      Collection<CloudNPC> theOldOnes = npcStore.readObject("npcs", NPCConstants.NPC_COLLECTION_TYPE);
      // remove the old entries
      db.delete("npc_store");
      if (theOldOnes != null) {
        // get the new database
        var target = databaseProvider.database(DATABASE_NAME);
        // convert the old entries
        theOldOnes.stream()
          .map(npc -> NPC.builder()
            .profileProperties(npc.profileProperties().stream()
              .map(property -> new NPC.ProfileProperty(property.name(), property.value(), property.signature()))
              .collect(Collectors.toSet()))
            .location(npc.position())
            .infoLines(Arrays.asList(npc.displayName(), npc.infoLine()))
            .inventoryName(npc.displayName())
            .targetGroup(npc.targetGroup())
            .items(ImmutableMap.of(0, npc.itemInHand()))
            .lookAtPlayer(npc.lookAtPlayer())
            .imitatePlayer(npc.imitatePlayer())
            .rightClickAction(NPC.ClickAction.valueOf(npc.rightClickAction().name()))
            .leftClickAction(NPC.ClickAction.valueOf(npc.leftClickAction().name()))
            .build())
          .forEach(npc -> target.insert(
            NodeNPCManagement.documentKey(npc.location()),
            Document.newJsonDocument().appendTree(npc)));
      }
    } else {
      // convert 4.0.0-RC1 npcs to RC2 npcs
      var database = databaseProvider.database(DATABASE_NAME);
      database.documents().stream()
        .filter(doc -> doc.contains("displayName"))
        .map(doc -> {
          var displayName = doc.getString("displayName");
          var npc = doc.toInstanceOf(NPC.class);
          var npcBuilder = NPC.builder(npc).inventoryName(displayName);

          // make the old display name - if present - the first (0th) info line
          if (displayName != null) {
            npc.infoLines().add(0, displayName);
            npcBuilder.infoLines(npc.infoLines());
          }

          return npcBuilder.build();
        })
        .forEach(npc -> database.insert(
          NodeNPCManagement.documentKey(npc.location()),
          Document.newJsonDocument().appendTree(npc)));
    }
  }

  @ModuleTask
  public void initModule(
    @NonNull CommandProvider commandProvider,
    @NonNull @Named("module") InjectionLayer<?> injectionLayer
  ) {
    // management init
    this.readConfigAndInstantiate(
      injectionLayer,
      eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.class,
      () -> eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder().build(),
      NodeNPCManagement.class,
      DocumentFactory.json());
    // register the npc module command
    commandProvider.register(NPCCommand.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload(@Nullable @Service NPCManagement management) {
    if (management != null) {
      management.npcConfiguration(this.loadConfig());
    }
  }

  private @NonNull eu.cloudnetservice.modules.npc.configuration.NPCConfiguration loadConfig() {
    return this.readConfig(
      eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.class,
      () -> eu.cloudnetservice.modules.npc.configuration.NPCConfiguration.builder().build(),
      DocumentFactory.json());
  }

  private @NonNull ItemLayout convertItemLayout(
    @NonNull NPCConfigurationEntry.ItemLayout oldLayout
  ) {
    return ItemLayout.builder()
      .material(oldLayout.material())
      .subId(oldLayout.subId())
      .lore(oldLayout.lore())
      .displayName(oldLayout.displayName())
      .build();
  }
}
