/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.node.listeners.NodeChannelMessageListener;
import eu.cloudnetservice.modules.npc.node.listeners.NodeSetupListener;
import eu.cloudnetservice.node.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.node.module.listener.PluginIncludeListener;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.NonNull;

public final class NodeNPCManagement extends AbstractNPCManagement {

  private static final Path PROTOCOL_LIB_CACHE_PATH = FileUtil.TEMP_DIR.resolve("caches/ProtocolLib.jar");

  private final Database database;
  private final Path configurationPath;
  private final AtomicBoolean protocolLibAvailable;

  public NodeNPCManagement(
    @NonNull NPCConfiguration npcConfiguration,
    @NonNull Database database,
    @NonNull Path configPath,
    @NonNull EventManager eventManager
  ) {
    super(npcConfiguration);
    this.database = database;
    this.configurationPath = configPath;
    this.protocolLibAvailable = new AtomicBoolean();

    // load all existing npcs
    this.database.documentsAsync().thenAccept(jsonDocuments -> {
      for (var document : jsonDocuments) {
        var npc = document.toInstanceOf(NPC.class);
        this.npcs.put(npc.location(), npc);
      }
    });

    // download protocol lib
    ConsoleProgressWrappers.wrapDownload(
      "https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar",
      stream -> {
        FileUtil.copy(stream, PROTOCOL_LIB_CACHE_PATH);
        this.protocolLibAvailable.set(true);
      }
    );

    // listener register
    eventManager.registerListener(new NodeSetupListener(this));
    eventManager.registerListener(new NodeChannelMessageListener(this));
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-npcs",
      CloudNetNPCModule.class,
      service -> this.protocolLibAvailable.get()
        && ServiceEnvironmentType.minecraftServer(service.serviceId().environment())
        && this.npcConfiguration
        .entries()
        .stream()
        .anyMatch(entry -> service.serviceConfiguration().groups().contains(entry.targetGroup())),
      (service, $) -> {
        var protocolLibPath = service.pluginDirectory().resolve("ProtocolLib.jar");
        FileUtil.copy(PROTOCOL_LIB_CACHE_PATH, protocolLibPath);
      }));
  }

  static @NonNull String documentKey(@NonNull WorldPosition position) {
    return position.world()
      + '.' + position.group()
      + '.' + position.x()
      + '.' + position.y()
      + '.' + position.z()
      + '.' + position.yaw()
      + '.' + position.pitch();
  }

  @Override
  public void createNPC(@NonNull NPC npc) {
    this.database.insert(documentKey(npc.location()), JsonDocument.newDocument(npc));
    this.npcs.put(npc.location(), npc);

    this.channelMessage(NPC_CREATED)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(npc))
      .build().send();
  }

  @Override
  public void deleteNPC(@NonNull WorldPosition position) {
    this.npcs.remove(position);
    this.database.delete(documentKey(position));

    this.channelMessage(NPC_DELETED)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllNPCs(@NonNull String group) {
    var positions = this.npcs.entrySet().stream()
      .filter(entry -> entry.getValue().targetGroup().equals(group))
      .map(Map.Entry::getKey)
      .toList();
    positions.forEach(position -> {
      this.npcs.remove(position);
      this.database.delete(documentKey(position));
    });

    this.channelMessage(NPC_BULK_DELETE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(positions))
      .build().send();
    return positions.size();
  }

  @Override
  public int deleteAllNPCs() {
    var positions = new HashSet<>(this.npcs.keySet());
    for (var position : positions) {
      this.npcs.remove(position);
      this.database.delete(documentKey(position));
    }

    this.channelMessage(NPC_BULK_DELETE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(positions))
      .build().send();
    return positions.size();
  }

  @Override
  public @NonNull Collection<NPC> npcs(@NonNull Collection<String> groups) {
    // filter all npcs
    return this.npcs.values().stream()
      .filter(npc -> groups.contains(npc.location().group()))
      .collect(Collectors.toList());
  }

  @Override
  public void npcConfiguration(@NonNull NPCConfiguration configuration) {
    this.handleInternalNPCConfigUpdate(configuration);
    this.channelMessage(NPC_CONFIGURATION_UPDATE)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(configuration))
      .build().send();
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    JsonDocument.newDocument(configuration).write(this.configurationPath);
  }
}
