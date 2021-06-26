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

package eu.cloudnetservice.cloudnet.ext.npcs;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractNPCManagement extends ServiceInfoStateWatcher {

  protected final Set<CloudNPC> cloudNPCS;
  protected final Map<ServiceInfoState, NPCConfigurationEntry.ItemLayout> itemLayouts = new HashMap<>();
  protected NPCConfiguration npcConfiguration;
  protected NPCConfigurationEntry ownNPCConfigurationEntry;

  public AbstractNPCManagement() {
    this.setNPCConfiguration(this.getNPCConfigurationFromNode());

    Set<CloudNPC> npcsFromNode = this.getNPCsFromNode();
    this.cloudNPCS = npcsFromNode == null ? new HashSet<>() : npcsFromNode.stream()
      .filter(npc -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
        .contains(npc.getPosition().getGroup()))
      .collect(Collectors.toSet());

    super.includeExistingServices();
  }

  @Override
  protected boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot != null) {

      ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
      ServiceEnvironmentType serviceEnvironment = serviceInfoSnapshot.getServiceId().getEnvironment();

      return serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer();
    }

    return false;
  }

  @Override
  protected boolean shouldShowFullServices() {
    return this.ownNPCConfigurationEntry.isShowFullServices();
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {

    if (event.getChannel().equals(NPCConstants.NPC_CHANNEL_NAME) && event.getMessage() != null) {

      switch (event.getMessage().toLowerCase()) {
        case NPCConstants.NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE: {
          NPCConfiguration npcConfiguration = event.getData().get("npcConfiguration", NPCConfiguration.class);
          this.setNPCConfiguration(npcConfiguration);
        }
        break;
        case NPCConstants.NPC_CHANNEL_ADD_NPC_MESSAGE: {
          CloudNPC npc = event.getData().get("npc", CloudNPC.class);

          if (npc != null) {
            this.addNPC(npc);
          }
        }
        break;
        case NPCConstants.NPC_CHANNEL_REMOVE_NPC_MESSAGE: {
          CloudNPC npc = event.getData().get("npc", CloudNPC.class);

          if (npc != null) {
            this.removeNPC(npc);
          }
        }
        break;
        default:
          break;
      }

    }

  }

  public abstract void updateNPC(CloudNPC cloudNPC);

  public abstract boolean isWorldLoaded(CloudNPC cloudNPC);

  public List<Pair<ServiceInfoSnapshot, ServiceInfoState>> filterNPCServices(@NotNull CloudNPC cloudNPC) {
    return super.services.values().stream()
      .filter(pair -> (pair.getSecond() != ServiceInfoState.STOPPED && pair.getSecond() != ServiceInfoState.STARTING)
        && Arrays.asList(pair.getFirst().getConfiguration().getGroups()).contains(cloudNPC.getTargetGroup()))
      .sorted(Comparator.comparingInt(pair -> pair.getFirst().getServiceId().getTaskServiceId()))
      .collect(Collectors.toList());
  }

  /**
   * Adds a NPC to this wrapper instance
   *
   * @param npc the NPC to add
   * @return if the NPC is allowed to exist on this wrapper instance
   */
  public boolean addNPC(@NotNull CloudNPC npc) {
    if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
      .contains(npc.getPosition().getGroup())) {
      this.cloudNPCS.remove(npc);
      this.cloudNPCS.add(npc);

      return true;
    }
    return false;
  }

  /**
   * Removes a NPC from this wrapper instance
   *
   * @param npc the NPC to remove
   */
  public void removeNPC(@NotNull CloudNPC npc) {
    this.cloudNPCS.remove(npc);
  }

  /**
   * Adds a NPC to the whole cluster and the database
   *
   * @param npc the NPC to add
   */
  public void sendNPCAddUpdate(@NotNull CloudNPC npc) {
    ChannelMessage.builder()
      .channel(NPCConstants.NPC_CHANNEL_NAME)
      .message(NPCConstants.NPC_CHANNEL_ADD_NPC_MESSAGE)
      .json(new JsonDocument("npc", npc))
      .build()
      .send();
  }

  /**
   * Removes a NPC from the whole cluster and the database
   *
   * @param npc the NPC to remove
   */
  public void sendNPCRemoveUpdate(@NotNull CloudNPC npc) {
    ChannelMessage.builder()
      .channel(NPCConstants.NPC_CHANNEL_NAME)
      .message(NPCConstants.NPC_CHANNEL_REMOVE_NPC_MESSAGE)
      .json(new JsonDocument("npc", npc))
      .build()
      .send();
  }

  public NPCConfiguration getNPCConfigurationFromNode() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(NPCConstants.NPC_CHANNEL_NAME)
      .message(NPCConstants.NPC_CHANNEL_GET_CONFIGURATION_MESSAGE)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    return response == null ? null : response.getJson().get("npcConfiguration", NPCConfiguration.class);
  }

  /**
   * Returns all NPCs contained in the CloudNet NPC database
   *
   * @return all NPCs or null, if an error occurred
   */
  @Nullable
  public Set<CloudNPC> getNPCsFromNode() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(NPCConstants.NPC_CHANNEL_NAME)
      .message(NPCConstants.NPC_CHANNEL_GET_NPCS_MESSAGE)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    return response == null ? null : response.getJson().get("npcs", NPCConstants.NPC_COLLECTION_TYPE);
  }

  public NPCConfiguration getNPCConfiguration() {
    return this.npcConfiguration;
  }

  /**
   * Sets the NPCConfiguration for this wrapper instance
   *
   * @param npcConfiguration the NPCConfiguration to set
   */
  public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
    this.npcConfiguration = npcConfiguration;
    this.ownNPCConfigurationEntry = npcConfiguration.getConfigurations().stream()
      .filter(entry -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
        .contains(entry.getTargetGroup()))
      .findFirst()
      .orElse(new NPCConfigurationEntry());

    this.itemLayouts.put(ServiceInfoState.ONLINE, this.ownNPCConfigurationEntry.getOnlineItem());
    this.itemLayouts.put(ServiceInfoState.EMPTY_ONLINE, this.ownNPCConfigurationEntry.getEmptyItem());
    this.itemLayouts.put(ServiceInfoState.FULL_ONLINE, this.ownNPCConfigurationEntry.getFullItem());
  }

  public NPCConfigurationEntry getOwnNPCConfigurationEntry() {
    return this.ownNPCConfigurationEntry;
  }


  /**
   * Returns a copy of the NPCs allowed to exist on this wrapper instance Use {@link
   * AbstractNPCManagement#addNPC(CloudNPC)} and {@link AbstractNPCManagement#removeNPC(CloudNPC)} for local
   * modification
   *
   * @return a copy of the NPCs
   */
  public Set<CloudNPC> getCloudNPCS() {
    return new HashSet<>(this.cloudNPCS);
  }

}
