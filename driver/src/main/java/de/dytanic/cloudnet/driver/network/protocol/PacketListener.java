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

package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.driver.network.NetworkChannel;
import lombok.NonNull;

/**
 * An packet listeners, allows to handle incoming packets, from some channel, that use the IPacketListenerRegistry in
 * that the listener has to be register
 * <p>
 * It will called on all channels, that the registry has register the listener
 *
 * @see PacketListenerRegistry
 */
@FunctionalInterface
public interface PacketListener {

  /**
   * Handles a new incoming packet message. The channel and the packet will not null
   *
   * @param channel the channel, from that the message was received
   * @param packet  the received packet message, which should handle from the listener
   * @throws Exception catch the exception, if the handle throws one
   */
  void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception;
}
