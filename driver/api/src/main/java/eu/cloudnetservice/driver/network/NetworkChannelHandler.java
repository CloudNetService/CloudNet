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

package eu.cloudnetservice.driver.network;

import eu.cloudnetservice.driver.network.protocol.Packet;
import lombok.NonNull;

/**
 * A handler for operations performed on a channel.
 *
 * @since 4.0
 */
public interface NetworkChannelHandler {

  /**
   * Handles the initialization of the channel, called the first time when a channel becomes active.
   *
   * @param channel the channel which was opened and to which this handler is bound.
   * @throws NullPointerException if the given channel is null.
   * @throws Exception            if any exception occurs during the event processing.
   */
  void handleChannelInitialize(@NonNull NetworkChannel channel) throws Exception;

  /**
   * Handles the receive of a packet on the channel this handler is bound to.
   *
   * @param channel the channel to which the packet was sent and the handler is bound.
   * @param packet  the packet which was received.
   * @return true if the packet should get processed by the packet registry, false otherwise.
   * @throws NullPointerException if either the given channel or packet is null.
   * @throws Exception            if any exception occurs during the event processing.
   */
  boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception;

  /**
   * Handles the inactivation processing of a channel, at this point the channel was closed either by a local handler or
   * the remote directly.
   *
   * @param channel the channel which was closed and this handler is bound to.
   * @throws NullPointerException if the given channel is null.
   * @throws Exception            if any exception occurs during the event processing.
   */
  void handleChannelClose(@NonNull NetworkChannel channel) throws Exception;
}
