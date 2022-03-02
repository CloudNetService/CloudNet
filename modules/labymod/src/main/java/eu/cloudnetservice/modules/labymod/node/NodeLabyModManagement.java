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

package eu.cloudnetservice.modules.labymod.node;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import lombok.NonNull;

public class NodeLabyModManagement implements LabyModManagement {

  private final CloudNetLabyModModule labyModModule;
  private LabyModConfiguration configuration;

  public NodeLabyModManagement(
    @NonNull CloudNetLabyModModule labyModModule,
    @NonNull LabyModConfiguration configuration,
    @NonNull RPCFactory rpcFactory
  ) {
    this.labyModModule = labyModModule;
    this.configuration = configuration;
    rpcFactory.newHandler(LabyModManagement.class, this).registerToDefaultRegistry();
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
    this.labyModModule.writeConfig(JsonDocument.newDocument(configuration));
  }
}
