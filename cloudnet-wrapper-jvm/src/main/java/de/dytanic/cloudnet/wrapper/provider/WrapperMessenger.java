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

package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class WrapperMessenger implements CloudMessenger {

  protected final RPCSender rpcSender;

  public WrapperMessenger(RPCProviderFactory rpcProviderFactory, INetworkComponent networkComponent) {
    this.rpcSender = rpcProviderFactory.providerForClass(networkComponent, CloudMessenger.class);
  }

  @Override
  public void sendChannelMessage(@NotNull ChannelMessage channelMessage) {
    this.rpcSender.invokeMethod("sendChannelMessage", channelMessage).fireSync();
  }

  @Override
  public ChannelMessage sendSingleChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
    return this.rpcSender.invokeMethod("sendSingleChannelMessageQuery", channelMessage).fireSync();
  }

  @Override
  public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
    return this.rpcSender.invokeMethod("sendChannelMessageQuery", channelMessage).fireSync();
  }
}
