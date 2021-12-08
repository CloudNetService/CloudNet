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

package eu.cloudnetservice.cloudnet.modules.labymod.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import eu.cloudnetservice.cloudnet.modules.labymod.LabyModManagement;
import eu.cloudnetservice.cloudnet.modules.labymod.config.LabyModConfiguration;
import org.jetbrains.annotations.NotNull;

public class NodeLabyModManagement implements LabyModManagement {

  private final CloudNetLabyModModule labyModModule;
  private LabyModConfiguration configuration;

  public NodeLabyModManagement(
    @NotNull CloudNetLabyModModule labyModModule,
    @NotNull LabyModConfiguration configuration,
    @NotNull RPCProviderFactory rpcProviderFactory
  ) {
    this.labyModModule = labyModModule;
    this.configuration = configuration;
    rpcProviderFactory.newHandler(LabyModManagement.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NotNull LabyModConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  public void setConfiguration(@NotNull LabyModConfiguration configuration) {
    this.setConfigurationSilently(configuration);

    ChannelMessage.builder()
      .channel(LabyModManagement.LABYMOD_MODULE_CHANNEL)
      .message(LabyModManagement.LABYMOD_UPDATE_CONFIG)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build()
      .send();
  }

  public void setConfigurationSilently(@NotNull LabyModConfiguration configuration) {
    this.configuration = configuration;
    this.labyModModule.writeConfig(JsonDocument.newDocument(configuration));
  }
}
