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

package eu.cloudnetservice.modules.syncproxy.impl.node;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConfigurationUpdateEvent;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@Provides(SyncProxyManagement.class)
public class NodeSyncProxyManagement implements SyncProxyManagement {

  private final EventManager eventManager;
  private final CloudNetSyncProxyModule syncProxyModule;

  private SyncProxyConfiguration configuration;

  @Inject
  public NodeSyncProxyManagement(
    @NonNull EventManager eventManager,
    @NonNull CloudNetSyncProxyModule syncProxyModule,
    @NonNull SyncProxyConfiguration configuration,
    @NonNull RPCHandlerRegistry rpcRegistry,
    @NonNull RPCFactory rpcFactory
  ) {
    this.syncProxyModule = syncProxyModule;
    this.configuration = configuration;
    this.eventManager = eventManager;

    var rpcHandler = rpcFactory.newRPCHandlerBuilder(SyncProxyManagement.class).targetInstance(this).build();
    rpcRegistry.registerHandler(rpcHandler);
  }

  @Override
  public @NonNull SyncProxyConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull SyncProxyConfiguration configuration) {
    // write the configuration to the file
    this.configurationSilently(configuration);
    // call the local event for the update of the config
    this.eventManager.callEvent(new SyncProxyConfigurationUpdateEvent(configuration));
    // send an update with the configuration to other components
    configuration.sendUpdate();
  }

  public void configurationSilently(@NonNull SyncProxyConfiguration configuration) {
    this.configuration = configuration;
    this.syncProxyModule.writeConfig(Document.newJsonDocument().appendTree(configuration));
  }
}
