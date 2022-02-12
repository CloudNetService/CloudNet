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

package eu.cloudnetservice.modules.bridge.node;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.registry.ServicesRegistry;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeConfigurationUpdateEvent;
import eu.cloudnetservice.modules.bridge.node.network.NodeBridgeChannelMessageListener;
import eu.cloudnetservice.modules.bridge.node.player.NodePlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import lombok.NonNull;

public class NodeBridgeManagement implements BridgeManagement {

  private final EventManager eventManager;
  private final PlayerManager playerManager;
  private final CloudNetBridgeModule bridgeModule;

  private BridgeConfiguration configuration;

  public NodeBridgeManagement(
    @NonNull CloudNetBridgeModule bridgeModule,
    @NonNull BridgeConfiguration configuration,
    @NonNull EventManager eventManager,
    @NonNull DataSyncRegistry registry,
    @NonNull RPCFactory providerFactory
  ) {
    this.eventManager = eventManager;
    this.bridgeModule = bridgeModule;
    this.configuration = configuration;
    // init the player manager
    this.playerManager = new NodePlayerManager(BRIDGE_PLAYER_DB_NAME, eventManager, registry, providerFactory, this);
    // register the listener
    eventManager.registerListener(new NodeBridgeChannelMessageListener(this, eventManager));
    // register the rpc handler
    providerFactory.newHandler(BridgeManagement.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NonNull BridgeConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull BridgeConfiguration configuration) {
    // update the configuration locally
    this.configurationSilently(configuration);
    // sync the config to the cluster
    ChannelMessage.builder()
      .targetAll()
      .channel(BRIDGE_CHANNEL_NAME)
      .message("update_bridge_configuration")
      .buffer(DataBuf.empty().writeObject(configuration))
      .build()
      .send();
    // call the event locally
    this.eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
  }

  @Override
  public @NonNull PlayerManager playerManager() {
    return this.playerManager;
  }

  @Override
  public void registerServices(@NonNull ServicesRegistry registry) {
    registry.registerService(BridgeManagement.class, "NodeBridgeManagement", this);
    registry.registerService(PlayerManager.class, "NodePlayerManager", this.playerManager);
  }

  @Override
  public void postInit() {
    for (var task : CloudNet.instance().serviceTaskProvider().serviceTasks()) {
      // check if the required permission is set
      if (!task.properties().contains("requiredPermission")) {
        task.properties().appendNull("requiredPermission");
        CloudNet.instance().serviceTaskProvider().addServiceTask(task);
      }
    }
  }

  public void configurationSilently(@NonNull BridgeConfiguration configuration) {
    // set and write the config
    this.configuration = configuration;
    this.bridgeModule.writeConfig(JsonDocument.newDocument(configuration));
  }
}
