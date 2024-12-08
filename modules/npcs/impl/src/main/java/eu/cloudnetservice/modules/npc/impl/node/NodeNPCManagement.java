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

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.impl.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.impl.InternalNPCManagement;
import eu.cloudnetservice.modules.npc.impl.node.listeners.NodeChannelMessageListener;
import eu.cloudnetservice.modules.npc.impl.node.listeners.NodeSetupListener;
import eu.cloudnetservice.node.impl.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.impl.module.listener.PluginIncludeListener;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

@Singleton
@Provides({AbstractNPCManagement.class, InternalNPCManagement.class, NPCManagement.class})
public final class NodeNPCManagement extends AbstractNPCManagement {

  private static final Path PROTOCOL_LIB_CACHE_PATH = FileUtil.TEMP_DIR.resolve("caches/ProtocolLib.jar");
  private static final String PROTOCOL_LIB_DOWNLOAD_URL = System.getProperty(
    "cloudnet.protocollib.download",
    "https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/build/libs/ProtocolLib.jar");

  private final Database database;
  private final Path configurationPath;

  @Inject
  public NodeNPCManagement(
    @NonNull NPCConfiguration npcConfiguration,
    @NonNull DatabaseProvider databaseProvider,
    @NonNull @Named("dataDirectory") Path dataDirectory,
    @NonNull Parsers parsers,
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ConsoleProgressWrappers progressWrappers
  ) {
    super(npcConfiguration, eventManager);
    this.configurationPath = dataDirectory.resolve("config.json");
    this.database = databaseProvider.database(CloudNetNPCModule.DATABASE_NAME);

    // load all existing npcs
    this.database.documentsAsync().thenAccept(jsonDocuments -> {
      for (var document : jsonDocuments) {
        var npc = document.toInstanceOf(NPC.class);
        this.npcs.put(npc.location(), npc);
      }
    });

    // download protocol lib
    progressWrappers.wrapDownload(PROTOCOL_LIB_DOWNLOAD_URL, stream -> FileUtil.copy(stream, PROTOCOL_LIB_CACHE_PATH));

    // listener register
    eventManager.registerListener(new NodeSetupListener(this, parsers));
    eventManager.registerListener(new NodeChannelMessageListener(this));
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-npcs",
      CloudNetNPCModule.class,
      moduleHelper,
      service -> ServiceEnvironmentType.minecraftServer(service.serviceId().environment())
        && this.npcConfiguration
        .entries()
        .stream()
        .anyMatch(entry -> service.serviceConfiguration().groups().contains(entry.targetGroup())),
      (service, $) -> {
        var protocolLibPath = service.pluginDirectory().resolve("ProtocolLib.jar");
        // make sure to only copy the cached protocollib jar if it exists
        if (Files.exists(PROTOCOL_LIB_CACHE_PATH)) {
          FileUtil.copy(PROTOCOL_LIB_CACHE_PATH, protocolLibPath);
        }
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
    this.database.insert(documentKey(npc.location()), Document.newJsonDocument().appendTree(npc));
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
    Document.newJsonDocument().appendTree(configuration).writeTo(this.configurationPath);
  }
}
