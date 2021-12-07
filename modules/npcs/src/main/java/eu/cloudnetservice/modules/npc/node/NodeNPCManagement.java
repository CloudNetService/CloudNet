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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.node.listeners.NodeChannelMessageListener;
import eu.cloudnetservice.modules.npc.node.listeners.NodePluginIncludeListener;
import eu.cloudnetservice.modules.npc.node.listeners.NodeSetupListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class NodeNPCManagement extends AbstractNPCManagement {

  private final Database database;
  private final Path configurationPath;

  public NodeNPCManagement(
    @NotNull NPCConfiguration npcConfiguration,
    @NotNull Database database,
    @NotNull Path configPath,
    @NotNull IEventManager eventManager
  ) {
    super(npcConfiguration);
    this.database = database;
    this.configurationPath = configPath;

    // load all existing npcs
    this.database.documentsAsync().onComplete(jsonDocuments -> {
      for (JsonDocument document : jsonDocuments) {
        NPC npc = document.toInstanceOf(NPC.class);
        this.npcs.put(npc.getLocation(), npc);
      }
    });

    eventManager.registerListener(new NodeSetupListener(this));
    eventManager.registerListener(new NodePluginIncludeListener(this));
    eventManager.registerListener(new NodeChannelMessageListener(this));
  }

  static @NotNull String getDocumentKey(@NotNull WorldPosition position) {
    return position.getWorld()
      + '.' + position.getGroup()
      + '.' + position.getX()
      + '.' + position.getY()
      + '.' + position.getZ()
      + '.' + position.getYaw()
      + '.' + position.getPitch();
  }

  @Override
  public void createNPC(@NotNull NPC npc) {
    this.database.insert(getDocumentKey(npc.getLocation()), JsonDocument.newDocument(npc));
    this.npcs.put(npc.getLocation(), npc);

    this.channelMessage(NPC_CREATED)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(npc))
      .build().send();
  }

  @Override
  public void deleteNPC(@NotNull WorldPosition position) {
    this.npcs.remove(position);
    this.database.delete(getDocumentKey(position));

    this.channelMessage(NPC_DELETED)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllNPCs(@NotNull String group) {
    Collection<WorldPosition> positions = this.npcs.entrySet().stream()
      .filter(entry -> entry.getValue().getTargetGroup().equals(group))
      .map(Entry::getKey)
      .collect(Collectors.toList());
    positions.forEach(position -> {
      this.npcs.remove(position);
      this.database.delete(getDocumentKey(position));
    });

    this.channelMessage(NPC_BULK_DELETE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(positions))
      .build().send();
    return positions.size();
  }

  @Override
  public int deleteAllNPCs() {
    Set<WorldPosition> positions = new HashSet<>(this.npcs.keySet());
    for (WorldPosition position : positions) {
      this.npcs.remove(position);
      this.database.delete(getDocumentKey(position));
    }

    this.channelMessage(NPC_BULK_DELETE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(positions))
      .build().send();
    return positions.size();
  }

  @Override
  public @NotNull Collection<NPC> getNPCs(@NotNull String[] groups) {
    Arrays.sort(groups);
    // filter all npcs
    return this.npcs.values().stream()
      .filter(npc -> Arrays.binarySearch(groups, npc.getLocation().getGroup()) >= 0)
      .collect(Collectors.toList());
  }

  @Override
  public void setNPCConfiguration(@NotNull NPCConfiguration configuration) {
    this.handleInternalNPCConfigUpdate(configuration);
    this.channelMessage(NPC_CONFIGURATION_UPDATE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(configuration))
      .build().send();
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NotNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    JsonDocument.newDocument(configuration).write(this.configurationPath);
  }
}
