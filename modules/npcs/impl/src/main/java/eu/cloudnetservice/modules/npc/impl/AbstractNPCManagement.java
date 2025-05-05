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

package eu.cloudnetservice.modules.npc.impl;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractNPCManagement implements InternalNPCManagement {

  public static final String NPC_CHANNEL_NAME = "internal_npc_channel";

  protected static final String NPC_CREATED = "npcs_npc_created";
  protected static final String NPC_DELETED = "npcs_npc_deleted";
  protected static final String NPC_BULK_DELETE = "npcs_npc_bulk_deleted";
  protected static final String NPC_CONFIGURATION_UPDATE = "npcs_npc_config_update";

  protected final Map<WorldPosition, NPC> npcs = new ConcurrentHashMap<>();
  protected NPCConfiguration npcConfiguration;

  public AbstractNPCManagement(@Nullable NPCConfiguration npcConfiguration, @NonNull EventManager eventManager) {
    this.npcConfiguration = npcConfiguration;
    eventManager.registerListener(new SharedChannelMessageListener(this));
  }

  @Override
  public @Nullable NPC npcAt(@NonNull WorldPosition position) {
    return this.npcs.get(position);
  }

  @Override
  public void deleteNPC(@NonNull NPC npc) {
    this.deleteNPC(npc.location());
  }

  @Override
  public @NonNull Collection<NPC> npcs() {
    return this.npcs.values();
  }

  @Override
  public @NonNull NPCConfiguration npcConfiguration() {
    return this.npcConfiguration;
  }

  @Override
  public void npcConfiguration(@NonNull NPCConfiguration configuration) {
    this.npcConfiguration = configuration;
  }

  @Override
  public void handleInternalNPCCreate(@NonNull NPC npc) {
    this.npcs.put(npc.location(), npc);
  }

  @Override
  public void handleInternalNPCRemove(@NonNull WorldPosition position) {
    this.npcs.remove(position);
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration) {
    this.npcConfiguration = configuration;
  }

  protected @NonNull ChannelMessage.Builder channelMessage(@NonNull String message) {
    return ChannelMessage.builder()
      .channel(NPC_CHANNEL_NAME)
      .message(message);
  }
}
