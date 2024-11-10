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

package eu.cloudnetservice.modules.bridge.impl.node;

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeConfigurationUpdateEvent;
import eu.cloudnetservice.modules.bridge.impl.InternalBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.node.listener.NodeSetupListener;
import eu.cloudnetservice.modules.bridge.impl.node.network.NodeBridgeChannelMessageListener;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.node.impl.module.listener.PluginIncludeListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import lombok.NonNull;

@Singleton
@Provides({InternalBridgeManagement.class, BridgeManagement.class})
public class NodeBridgeManagement implements InternalBridgeManagement {

  private final EventManager eventManager;
  private final PlayerManager playerManager;
  private final ServiceTaskProvider taskProvider;
  private final CloudNetBridgeModule bridgeModule;

  private BridgeConfiguration configuration;

  @Inject
  public NodeBridgeManagement(
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull RPCFactory providerFactory,
    @NonNull PlayerManager playerManager,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull CloudNetBridgeModule bridgeModule,
    @NonNull BridgeConfiguration configuration,
    @NonNull RPCHandlerRegistry rpcHandlerRegistry
  ) {
    this.eventManager = eventManager;
    this.bridgeModule = bridgeModule;
    this.configuration = configuration;
    this.playerManager = playerManager;
    this.taskProvider = taskProvider;
    // register the plugin include listener
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-bridge",
      NodeBridgeManagement.class,
      moduleHelper,
      service -> Collections.disjoint(this.configuration.excludedGroups(), service.serviceConfiguration().groups())));

    // register the rpc handler
    var rpcHandler = providerFactory.newRPCHandlerBuilder(BridgeManagement.class).targetInstance(this).build();
    rpcHandlerRegistry.registerHandler(rpcHandler);
  }

  @PostConstruct
  private void registerListener() {
    this.eventManager.registerListener(NodeBridgeChannelMessageListener.class);
    this.eventManager.registerListener(NodeSetupListener.class);
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
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(BridgeManagement.class, "NodeBridgeManagement", this);
    registry.registerProvider(PlayerManager.class, "NodePlayerManager", this.playerManager);
  }

  @Override
  public void postInit() {
    for (var task : this.taskProvider.serviceTasks()) {
      if (task.propertyAbsent(BridgeDocProperties.REQUIRED_PERMISSION)) {
        // the required permission is missing, add it to the task
        var newTask = ServiceTask.builder(task)
          .writeProperty(BridgeDocProperties.REQUIRED_PERMISSION, null)
          .build();
        this.taskProvider.addServiceTask(newTask);
      }
    }
  }

  public void configurationSilently(@NonNull BridgeConfiguration configuration) {
    // set and write the config
    this.configuration = configuration;
    this.bridgeModule.writeConfig(Document.newJsonDocument().appendTree(configuration));
  }
}
