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

package eu.cloudnetservice.node.impl.module;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.DefaultModuleProviderHandler;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleProviderHandler;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
public final class NodeModuleProviderHandler extends DefaultModuleProviderHandler implements ModuleProviderHandler {

  private final NetworkClient networkClient;
  private final NetworkServer networkServer;
  private final RPCHandlerRegistry rpcHandlerRegistry;

  private final I18n i18n;
  private final CommandProvider commandProvider;
  private final DataSyncRegistry dataSyncRegistry;

  @Inject
  public NodeModuleProviderHandler(
    @NonNull ModuleProvider moduleProvider,
    @NonNull NetworkClient networkClient,
    @NonNull NetworkServer networkServer,
    @NonNull RPCHandlerRegistry rpcHandlerRegistry,
    @NonNull EventManager eventManager,
    @NonNull CommandProvider commandProvider,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull @Service I18n i18n,
    @NonNull DataSyncRegistry dataSyncRegistry
  ) {
    super(eventManager, moduleProvider, serviceRegistry);

    this.networkClient = networkClient;
    this.networkServer = networkServer;
    this.rpcHandlerRegistry = rpcHandlerRegistry;
    this.commandProvider = commandProvider;
    this.i18n = i18n;
    this.dataSyncRegistry = dataSyncRegistry;
  }

  @Override
  public void handlePostModuleStop(@NonNull ModuleWrapper moduleWrapper) {
    super.handlePostModuleStop(moduleWrapper);

    // unregister all listeners added to the network handlers
    this.networkClient.packetRegistry().removeListeners(moduleWrapper.classLoader());
    this.networkServer.packetRegistry().removeListeners(moduleWrapper.classLoader());
    // remove all rpc handlers
    this.rpcHandlerRegistry.unregisterHandlers(moduleWrapper.classLoader());
    // unregister all listeners added to the network channels
    this.removeListeners(this.networkClient.channels(), moduleWrapper.classLoader());
    this.removeListeners(this.networkServer.channels(), moduleWrapper.classLoader());
    // unregister all commands
    this.commandProvider.unregister(moduleWrapper.classLoader());
    // unregister everything the module syncs to the cluster
    this.dataSyncRegistry.unregisterHandler(moduleWrapper.classLoader());
    // unregister all object mappers which are registered
    DefaultObjectMapper.DEFAULT_MAPPER.unregisterBindings(moduleWrapper.classLoader());
    // unregister all language files
    this.i18n.unregisterProviders(moduleWrapper.classLoader());
  }

  private void removeListeners(@NonNull Collection<NetworkChannel> channels, @NonNull ClassLoader loader) {
    for (var channel : channels) {
      channel.packetRegistry().removeListeners(loader);
    }
  }
}
