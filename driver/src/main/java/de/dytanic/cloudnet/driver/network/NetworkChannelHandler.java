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

package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.BasePacket;
import lombok.NonNull;

/**
 * A networkChannelHandler provides the operation with the INetworkChannel
 *
 * @see NetworkChannel
 */
public interface NetworkChannelHandler {

  /**
   * Handles an new open connected channel
   *
   * @param channel the providing channel on that this handler is sets on this
   */
  void handleChannelInitialize(@NonNull NetworkChannel channel) throws Exception;

  /**
   * Handles a incoming packet from a provided channel, that contains that channel handler
   *
   * @param channel the providing channel on that this handler is sets on this
   * @param packet  the packet, that will received from the remote component
   * @return should return true that, the packet that was received is allowed to handle from the packet listeners at the
   * packetListenerRegistry
   */
  boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull BasePacket packet) throws Exception;

  /**
   * Handles the close phase from a NetworkChannel
   *
   * @param channel the providing channel on that this handler is sets on this
   */
  void handleChannelClose(@NonNull NetworkChannel channel) throws Exception;
}
