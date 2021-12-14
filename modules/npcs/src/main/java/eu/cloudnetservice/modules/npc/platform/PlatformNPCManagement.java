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
import org.jetbrains.annotations.NotNull;
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
    var groups = Wrapper.getInstance().getServiceConfiguration().getGroups().toArray(new String[0]);
    for (var npc : this.getNPCs(groups)) {
      this.npcs.put(npc.getLocation(), npc);
    }
    // register the listeners
    CloudNetDriver.getInstance().getEventManager().registerListener(new CloudNetServiceListener(this));
  }

  protected static @Nullable NPCConfiguration loadNPCConfiguration() {
    var response = ChannelMessage.builder()
      .channel(NPC_CHANNEL_NAME)
      .message(NPC_REQUEST_CONFIG)
      .targetNode(Wrapper.getInstance().getNodeUniqueId())
      .build()
      .sendSingleQuery();
    return response == null ? null : response.getContent().readObject(NPCConfiguration.class);
  }

  @Override
  public void createNPC(@NotNull NPC npc) {
    this.channelMessage(NPC_CREATE)
      .buffer(DataBuf.empty().writeObject(npc))
      .build().send();
  }

  @Override
  public void deleteNPC(@NotNull WorldPosition position) {
    this.channelMessage(NPC_DELETE)
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllNPCs(@NotNull String group) {
    var response = this.channelMessage(NPC_BULK_DELETE)
      .buffer(DataBuf.empty().writeString(group))
      .build().sendSingleQuery();
    return response == null ? 0 : response.getContent().readInt();
  }

  @Override
  public int deleteAllNPCs() {
    var response = this.channelMessage(NPC_ALL_DELETE)
      .buffer(DataBuf.empty().writeObject(this.npcs.keySet()))
      .build().sendSingleQuery();
    return response == null ? 0 : response.getContent().readInt();
  }

  @Override
  public @NotNull Collection<NPC> getNPCs(@NotNull String[] groups) {
    var response = this.channelMessage(NPC_GET_NPCS_BY_GROUP)
      .buffer(DataBuf.empty().writeObject(groups))
      .build().sendSingleQuery();
    return response == null ? Collections.emptySet() : response.getContent().readObject(NPC.COLLECTION_NPC);
  }

  @Override
  public void setNPCConfiguration(@NotNull NPCConfiguration configuration) {
    this.channelMessage(NPC_SET_CONFIG)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build().send();
  }

  @Override
  public void handleInternalNPCCreate(@NotNull NPC npc) {
    // check if the npc is on this group
    if (Wrapper.getInstance().getServiceConfiguration().getGroups().contains(npc.getLocation().group())) {
      super.handleInternalNPCCreate(npc);
      // remove the old selector npc
      var entity = this.trackedEntities.remove(npc.getLocation());
      if (entity != null && entity.isSpawned()) {
        entity.remove();
      }
      // create and spawn a new selector npc
      entity = this.createSelectorEntity(npc);
      // spawn the npc if possible
      if (entity.canSpawn()) {
        entity.spawn();
      }
      // start tracking the npc
      this.trackedEntities.put(npc.getLocation(), entity);
      // apply the tracked services
      for (var service : this.trackedServices.values()) {
        if (service.getConfiguration().getGroups().contains(entity.getNPC().getTargetGroup())) {
          entity.trackService(service);
        }
      }
    }
  }

  @Override
  public void handleInternalNPCRemove(@NotNull WorldPosition position) {
    super.handleInternalNPCRemove(position);
    // remove the platform npc if spawned
    var entity = this.trackedEntities.remove(position);
    if (entity != null && entity.isSpawned()) {
      entity.remove();
    }
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NotNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    // update all selector entities
    this.trackedEntities.values().forEach(PlatformSelectorEntity::update);
  }

  @Override
  protected Builder channelMessage(@NotNull String message) {
    return ChannelMessage.builder()
      .channel(NPC_CHANNEL_NAME)
      .message(message)
      .target(Type.NODE, Wrapper.getInstance().getNodeUniqueId());
  }

  public void initialize() {
    // start tracking all entities
    for (var value : this.npcs.values()) {
      this.trackedEntities.put(value.getLocation(), this.createSelectorEntity(value));
    }
    // initialize the services now
    CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
      for (var service : services) {
        if (this.shouldTrack(service)) {
          this.handleServiceUpdate(service);
        }
      }
    });
  }

  public @Nullable NPCConfigurationEntry getApplicableNPCConfigurationEntry() {
    for (var entry : this.npcConfiguration.getEntries()) {
      if (Wrapper.getInstance().getServiceConfiguration().getGroups().contains(entry.getTargetGroup())) {
        return entry;
      }
    }
    return null;
  }

  public @NotNull InventoryConfiguration getInventoryConfiguration() {
    // get the npc configuration entry
    var entry = this.getApplicableNPCConfigurationEntry();
    if (entry == null) {
      throw new IllegalStateException("no npc config entry for the current service groups found");
    }
    // find an inventory configuration which explicitly targets a group of the snapshot
    return entry.getInventoryConfiguration();
  }

  public void handleServiceUpdate(@NotNull ServiceInfoSnapshot service) {
    for (var entity : this.trackedEntities.values()) {
      if (service.getConfiguration().getGroups().contains(entity.getNPC().getTargetGroup())) {
        entity.trackService(service);
      }
    }
    // mark the service as tracked
    this.trackedServices.put(service.getServiceId().getUniqueId(), service);
  }

  public void handleServiceRemove(@NotNull ServiceInfoSnapshot service) {
    for (var entity : this.trackedEntities.values()) {
      if (service.getConfiguration().getGroups().contains(entity.getNPC().getTargetGroup())) {
        entity.stopTrackingService(service);
      }
    }
    // stop tracking the service
    this.trackedServices.remove(service.getServiceId().getUniqueId());
  }

  public int getRandomEmoteId(@NotNull LabyModEmoteConfiguration configuration, int[] emoteIds) {
    if (emoteIds.length == 0) {
      // no ids - skip
      return -2;
    } else if (configuration.isSyncEmotesBetweenNPCs()) {
      // return a random id from the emote list
      return emoteIds[ThreadLocalRandom.current().nextInt(0, emoteIds.length)];
    } else {
      // -1 -> choose a random one every time
      return -1;
    }
  }

  public @NotNull Map<WorldPosition, PlatformSelectorEntity<L, P, M, I>> getTrackedEntities() {
    return this.trackedEntities;
  }

  protected abstract @NotNull PlatformSelectorEntity<L, P, M, I> createSelectorEntity(@NotNull NPC base);

  protected abstract @NotNull WorldPosition toWorldPosition(@NotNull L location, @NotNull String group);

  protected abstract @NotNull L toPlatformLocation(@NotNull WorldPosition position);

  protected abstract boolean shouldTrack(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
