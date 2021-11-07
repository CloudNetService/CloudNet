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

package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncRegistry;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.node.network.NodeBridgeChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import org.jetbrains.annotations.NotNull;

public class NodeBridgeManagement implements BridgeManagement {

  private final IEventManager eventManager;
  private final IPlayerManager playerManager;
  private final CloudNetBridgeModule bridgeModule;

  private BridgeConfiguration configuration;

  public NodeBridgeManagement(
    @NotNull CloudNetBridgeModule bridgeModule,
    @NotNull BridgeConfiguration configuration,
    @NotNull IEventManager eventManager,
    @NotNull DataSyncRegistry registry,
    @NotNull RPCProviderFactory providerFactory
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
  public @NotNull BridgeConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  public void setConfiguration(@NotNull BridgeConfiguration configuration) {
    // update the configuration locally
    this.setConfigurationSilently(configuration);
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
  public @NotNull IPlayerManager getPlayerManager() {
    return this.playerManager;
  }

  @Override
  public void registerServices(@NotNull IServicesRegistry registry) {
    registry.registerService(IPlayerManager.class, "NodePlayerManager", this.playerManager);
  }

  @Override
  public void postInit() {
    for (ServiceTask task : CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()) {
      // check if the required permission is set
      if (!task.getProperties().contains("requiredPermission")) {
        task.getProperties().appendNull("requiredPermission");
        CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(task);
      }
    }
  }

  public void setConfigurationSilently(@NotNull BridgeConfiguration configuration) {
    // set and write the config
    this.configuration = configuration;
    this.bridgeModule.writeConfig(JsonDocument.newDocument(configuration));
  }
}
