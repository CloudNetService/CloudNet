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

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.DefaultMessenger;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class WrapperMessenger extends DefaultMessenger implements CloudMessenger {

  private final Wrapper wrapper;

  public WrapperMessenger(Wrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void sendChannelMessage(@NotNull ChannelMessage channelMessage) {
    this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(channelMessage, false));
  }

  @Override
  public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NotNull ChannelMessage channelMessage) {
    return this.wrapper.getNetworkClient().getFirstChannel()
      .sendQueryAsync(new PacketClientServerChannelMessage(channelMessage, true))
      .map(packet -> packet.getBuffer().readableBytes() <= 1 ? Collections.emptyList()
        : packet.getBuffer().readObjectCollection(ChannelMessage.class));
  }

}
