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

package eu.cloudnetservice.cloudnet.ext.syncproxy.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyConfigurationUpdateEvent;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyConfiguration;
import org.jetbrains.annotations.NotNull;

public class NodeSyncProxyManagement implements SyncProxyManagement {

  private final CloudNetSyncProxyModule syncProxyModule;
  private SyncProxyConfiguration configuration;

  public NodeSyncProxyManagement(
    @NotNull CloudNetSyncProxyModule syncProxyModule,
    @NotNull SyncProxyConfiguration configuration,
    @NotNull RPCProviderFactory rpcProviderFactory
  ) {
    this.syncProxyModule = syncProxyModule;
    this.configuration = configuration;
    rpcProviderFactory.newHandler(SyncProxyManagement.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NotNull SyncProxyConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  public void setConfiguration(@NotNull SyncProxyConfiguration configuration) {
    // write the configuration to the file
    this.setConfigurationSilently(configuration);
    // call the local event for the update of the config
    this.syncProxyModule.getEventManager().callEvent(new SyncProxyConfigurationUpdateEvent(configuration));
    // send an update with the configuration to other components
    configuration.sendUpdate();
  }

  @Override
  public void registerService(@NotNull IServicesRegistry registry) {
    registry.registerService(SyncProxyManagement.class, "NodeSyncProxyManagement", this);
  }

  @Override
  public void unregisterService(@NotNull IServicesRegistry registry) {
    registry.unregisterService(SyncProxyManagement.class, "NodeSyncProxyManagement");
  }

  public void setConfigurationSilently(@NotNull SyncProxyConfiguration configuration) {
    this.configuration = configuration;
    this.syncProxyModule.writeConfig(JsonDocument.newDocument(configuration));
  }
}
