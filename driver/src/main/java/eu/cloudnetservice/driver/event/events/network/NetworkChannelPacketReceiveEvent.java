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

package eu.cloudnetservice.driver.event.events.network;

import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import lombok.NonNull;

/**
 * An event fired always when a packet is received in a channel. Reads to the packet buffer will get reflected down the
 * line.
 *
 * @since 4.0
 */
public final class NetworkChannelPacketReceiveEvent extends NetworkEvent implements Cancelable {

  private final Packet packet;
  private boolean cancelled;

  /**
   * Creates a new instance of this network event.
   *
   * @param channel the channel which is associated with this event.
   * @param packet  the packet which was received.
   * @throws NullPointerException if the given channel or packet is null.
   */
  public NetworkChannelPacketReceiveEvent(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    super(channel);
    this.packet = packet;
  }

  /**
   * Get the packet which got send by the other network component.
   *
   * @return the packet which got send.
   */
  public @NonNull Packet packet() {
    return this.packet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
