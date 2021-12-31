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

package eu.cloudnetservice.modules.npc.platform;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessage.Builder;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget.Type;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.npc.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.LabyModEmoteConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformNPCManagement<L, P, M, I> extends AbstractNPCManagement {

  public static final String NPC_CREATE = "npcs_npc_create";
  public static final String NPC_DELETE = "npcs_npc_delete";
  public static final String NPC_BULK_DELETE = "npcs_bulk_delete";
  public static final String NPC_ALL_DELETE = "npcs_npc_delete_all";
  public static final String NPC_GET_NPCS_BY_GROUP = "npcs_get_by_group";
  public static final String NPC_REQUEST_CONFIG = "npcs_request_config";
  public static final String NPC_SET_CONFIG = "npcs_update_npc_config";

  protected final Map<UUID, ServiceInfoSnapshot> trackedServices = new ConcurrentHashMap<>();
  protected final Map<WorldPosition, PlatformSelectorEntity<L, P, M, I>> trackedEntities = new ConcurrentHashMap<>();

  public PlatformNPCManagement() {
    super(loadNPCConfiguration());
    // get the npcs for the current group
    var groups = Wrapper.instance().serviceConfiguration().groups();
    for (var npc : this.npcs(groups)) {
      this.npcs.put(npc.location(), npc);
    }
    // register the listeners
    CloudNetDriver.instance().eventManager().registerListener(new CloudNetServiceListener(this));
  }

  protected static @Nullable NPCConfiguration loadNPCConfiguration() {
    var response = ChannelMessage.builder()
      .channel(NPC_CHANNEL_NAME)
      .message(NPC_REQUEST_CONFIG)
      .targetNode(Wrapper.instance().nodeUniqueId())
      .build()
      .sendSingleQuery();
    return response == null ? null : response.content().readObject(NPCConfiguration.class);
  }

  @Override
  public void createNPC(@NonNull NPC npc) {
    this.channelMessage(NPC_CREATE)
      .buffer(DataBuf.empty().writeObject(npc))
      .build().send();
  }

  @Override
  public void deleteNPC(@NonNull WorldPosition position) {
    this.channelMessage(NPC_DELETE)
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllNPCs(@NonNull String group) {
    var response = this.channelMessage(NPC_BULK_DELETE)
      .buffer(DataBuf.empty().writeString(group))
      .build().sendSingleQuery();
    return response == null ? 0 : response.content().readInt();
  }

  @Override
  public int deleteAllNPCs() {
    var response = this.channelMessage(NPC_ALL_DELETE)
      .buffer(DataBuf.empty().writeObject(this.npcs.keySet()))
      .build().sendSingleQuery();
    return response == null ? 0 : response.content().readInt();
  }

  @Override
  public @NonNull Collection<NPC> npcs(@NonNull Collection<String> groups) {
    var response = this.channelMessage(NPC_GET_NPCS_BY_GROUP)
      .buffer(DataBuf.empty().writeObject(groups))
      .build().sendSingleQuery();
    return response == null ? Collections.emptySet() : response.content().readObject(NPC.COLLECTION_NPC);
  }

  @Override
  public void npcConfiguration(@NonNull NPCConfiguration configuration) {
    this.channelMessage(NPC_SET_CONFIG)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build().send();
  }

  @Override
  public void handleInternalNPCCreate(@NonNull NPC npc) {
    // check if the npc is on this group
    if (Wrapper.instance().serviceConfiguration().groups().contains(npc.location().group())) {
      super.handleInternalNPCCreate(npc);
      // remove the old selector npc
      var entity = this.trackedEntities.remove(npc.location());
      if (entity != null && entity.spawned()) {
        entity.remove();
      }
      // create and spawn a new selector npc
      entity = this.createSelectorEntity(npc);
      // spawn the npc if possible
      if (entity.canSpawn()) {
        entity.spawn();
      }
      // start tracking the npc
      this.trackedEntities.put(npc.location(), entity);
      // apply the tracked services
      for (var service : this.trackedServices.values()) {
        if (service.configuration().groups().contains(entity.npc().targetGroup())) {
          entity.trackService(service);
        }
      }
    }
  }

  @Override
  public void handleInternalNPCRemove(@NonNull WorldPosition position) {
    super.handleInternalNPCRemove(position);
    // remove the platform npc if spawned
    var entity = this.trackedEntities.remove(position);
    if (entity != null && entity.spawned()) {
      entity.remove();
    }
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    // update all selector entities
    this.trackedEntities.values().forEach(PlatformSelectorEntity::update);
  }

  @Override
  protected Builder channelMessage(@NonNull String message) {
    return ChannelMessage.builder()
      .channel(NPC_CHANNEL_NAME)
      .message(message)
      .target(Type.NODE, Wrapper.instance().nodeUniqueId());
  }

  public void initialize() {
    // start tracking all entities
    for (var value : this.npcs.values()) {
      this.trackedEntities.put(value.location(), this.createSelectorEntity(value));
    }
    // initialize the services now
    CloudNetDriver.instance().cloudServiceProvider().servicesAsync().onComplete(services -> {
      for (var service : services) {
        if (this.shouldTrack(service)) {
          this.handleServiceUpdate(service);
        }
      }
    });
  }

  public @Nullable NPCConfigurationEntry applicableNPCConfigurationEntry() {
    for (var entry : this.npcConfiguration.entries()) {
      if (Wrapper.instance().serviceConfiguration().groups().contains(entry.targetGroup())) {
        return entry;
      }
    }
    return null;
  }

  public @NonNull InventoryConfiguration inventoryConfiguration() {
    // get the npc configuration entry
    var entry = this.applicableNPCConfigurationEntry();
    if (entry == null) {
      throw new IllegalStateException("no npc config entry for the current service groups found");
    }
    // find an inventory configuration which explicitly targets a group of the snapshot
    return entry.inventoryConfiguration();
  }

  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot service) {
    for (var entity : this.trackedEntities.values()) {
      if (service.configuration().groups().contains(entity.npc().targetGroup())) {
        entity.trackService(service);
      }
    }
    // mark the service as tracked
    this.trackedServices.put(service.serviceId().uniqueId(), service);
  }

  public void handleServiceRemove(@NonNull ServiceInfoSnapshot service) {
    for (var entity : this.trackedEntities.values()) {
      if (service.configuration().groups().contains(entity.npc().targetGroup())) {
        entity.stopTrackingService(service);
      }
    }
    // stop tracking the service
    this.trackedServices.remove(service.serviceId().uniqueId());
  }

  public int randomEmoteId(@NonNull LabyModEmoteConfiguration configuration, int[] emoteIds) {
    if (emoteIds.length == 0) {
      // no ids - skip
      return -2;
    } else if (configuration.syncEmotesBetweenNPCs()) {
      // return a random id from the emote list
      return emoteIds[ThreadLocalRandom.current().nextInt(0, emoteIds.length)];
    } else {
      // -1 -> choose a random one every time
      return -1;
    }
  }

  public @NonNull Map<WorldPosition, PlatformSelectorEntity<L, P, M, I>> trackedEntities() {
    return this.trackedEntities;
  }

  protected abstract @NonNull PlatformSelectorEntity<L, P, M, I> createSelectorEntity(@NonNull NPC base);

  protected abstract @NonNull WorldPosition toWorldPosition(@NonNull L location, @NonNull String group);

  protected abstract @NonNull L toPlatformLocation(@NonNull WorldPosition position);

  protected abstract boolean shouldTrack(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);
}
