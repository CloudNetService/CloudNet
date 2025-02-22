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

package eu.cloudnetservice.driver.impl.network.standard;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import lombok.NonNull;

/**
 * A packet which gets sent to indicate that a channel message should get processed. Sending this packet to a node will
 * cause the node to either process it or redirect it into the cluster if needed. When sent to a wrapper, the wrapper
 * will always post the receive event without further checks (the node is required to do these).
 *
 * @since 4.0
 */
public final class ChannelMessagePacket extends BasePacket {

  /**
   * Constructs a new channel message packet instance.
   *
   * @param message the channel message (or content) which should be sent to the other component.
   * @param wrapper if the channel was sent by a wrapper component.
   * @throws NullPointerException if the given channel message is null.
   */
  public ChannelMessagePacket(@NonNull ChannelMessage message, boolean wrapper) {
    super(
      NetworkConstants.CHANNEL_MESSAGING_CHANNEL,
      message.prioritized(),
      DataBuf.empty().writeBoolean(wrapper).writeObject(message));
  }
}
