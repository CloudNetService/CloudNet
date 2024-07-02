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
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A delegate default interface for a network component implementation.
 *
 * @since 4.0
 */
public interface DefaultNetworkComponent extends NetworkComponent {

  Logger LOGGER = LoggerFactory.getLogger(DefaultNetworkComponent.class);

  /**
   * Get the collection of channels which are connected to this network component. The returned collection is
   * modifiable, but should not be used by developers to make any changes to it. If you need all channels which are
   * connected to the component use {@link #channels()} instead.
   *
   * @return all channels which are connected to this component, modifiable.
   */
  @ApiStatus.Internal
  @NonNull Collection<NetworkChannel> modifiableChannels();

  /**
   * {@inheritDoc}
   */
  @Override
  default void closeChannels() {
    var iterator = this.modifiableChannels().iterator();
    while (iterator.hasNext()) {
      // close the next channel and remove it
      iterator.next().close();
      iterator.remove();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default void sendPacket(@NonNull Packet packet) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacket(packet);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default void sendPacketSync(@NonNull Packet packet) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacketSync(packet);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default void sendPacket(@NonNull Packet... packets) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacket(packets);
    }
  }
}
