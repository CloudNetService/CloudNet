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

package eu.cloudnetservice.modules.labymod.impl.node;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class NodeLabyModManagement implements LabyModManagement {

  private final CloudNetLabyModModule labyModModule;
  private LabyModConfiguration configuration;

  @Inject
  public NodeLabyModManagement(
    @NonNull CloudNetLabyModModule labyModModule,
    @NonNull LabyModConfiguration configuration,
    @NonNull RPCFactory rpcFactory,
    @NonNull RPCHandlerRegistry rpcHandlerRegistry
  ) {
    this.labyModModule = labyModModule;
    this.configuration = configuration;

    var rpcHandler = rpcFactory.newRPCHandlerBuilder(LabyModManagement.class).targetInstance(this).build();
    rpcHandlerRegistry.registerHandler(rpcHandler);
  }

  @Override
  public @NonNull LabyModConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull LabyModConfiguration configuration) {
    this.configurationSilently(configuration);

    ChannelMessage.builder()
      .targetAll()
      .channel(LabyModManagement.LABYMOD_MODULE_CHANNEL)
      .message(LabyModManagement.LABYMOD_UPDATE_CONFIG)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build()
      .send();
  }

  public void configurationSilently(@NonNull LabyModConfiguration configuration) {
    this.configuration = configuration;
    this.labyModModule.writeConfig(Document.newJsonDocument().appendTree(configuration));
  }
}
