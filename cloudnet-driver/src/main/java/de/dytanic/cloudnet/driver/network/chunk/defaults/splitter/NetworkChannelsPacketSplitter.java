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

package de.dytanic.cloudnet.driver.network.chunk.defaults.splitter;

import com.google.common.collect.ImmutableList;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.util.Collection;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class NetworkChannelsPacketSplitter implements Consumer<IPacket> {

  private final Collection<INetworkChannel> channels;

  public NetworkChannelsPacketSplitter(@NotNull Collection<INetworkChannel> channels) {
    this.channels = ImmutableList.copyOf(channels);
  }

  @Override
  public void accept(IPacket packet) {
    // disable releasing of the content as we need to content multiple times
    packet.getContent().disableReleasing();
    // write to all channels
    for (INetworkChannel channel : this.channels) {
      // mark the current indexes of the packet
      packet.getContent().startTransaction();
      // write the packet to the channel
      channel.sendPacketSync(packet);
      // redo the transaction to allow further writes
      packet.getContent().redoTransaction();
    }
    // enable the releasing and free the memory
    packet.getContent().enableReleasing().release();
  }
}
