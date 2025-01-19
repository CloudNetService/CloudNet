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

package eu.cloudnetservice.driver.network.protocol;

import eu.cloudnetservice.driver.network.NetworkChannel;
import lombok.NonNull;

/**
 * Represents a listener for a specific packet. A packet listener can be registered through the packet listener registry
 * on a per-network-component and a per-channel basis. Each packet listener gets called in the order the listener was
 * registered, the first registered listener will be called first.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface PacketListener {

  /**
   * Handles the incoming packet. A packet handle should be release-safe and transactional to preserve the content of
   * the packet for listeners which are following in the chain. If a packet listener is not doing that, it might lead to
   * unexpected exceptions in other packet listener implementations for the same packet.
   *
   * @param channel the channel from which the original packet came.
   * @param packet  the packet which was received.
   * @throws Exception            if any exception occurs during handling of the packet.
   * @throws NullPointerException if either the given channel or packet is null.
   */
  void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception;
}
