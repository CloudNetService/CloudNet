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

package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.driver.network.NetworkChannel;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The packet listener registry allows to manage listeners that can handle input packet messages.
 */
public interface PacketListenerRegistry {

  /**
   * Returns the parent PacketListenerRegistry implementation instance if exists
   */
  @Nullable PacketListenerRegistry parent();

  /**
   * Adds a new listeners for packets that are received on a specific channel
   *
   * @param channel   the channel, that the listener should listen on
   * @param listeners the listeners that should be add for this channel
   */
  void addListener(int channel, @NonNull PacketListener... listeners);

  /**
   * Removes the listeners if they are registered on this listener registry. If the listeners items are null, then it
   * should ignored
   *
   * @param channel   the channel, that the listener should listen on
   * @param listeners the listeners, that should remove on this registry
   */
  void removeListener(int channel, @NonNull PacketListener... listeners);

  /**
   * Removes all listeners on a specific channel
   *
   * @param channel the channel id from that all listeners should remove
   */
  void removeListeners(int channel);

  /**
   * Remove all listeners that are registered on the specific classLoader instance
   *
   * @param classLoader the classLoader, from that all listeners that are contained on the registry should remove
   */
  void removeListeners(@NonNull ClassLoader classLoader);

  boolean hasListeners(int channel);

  /**
   * Removes all listeners by all channel from the registry
   */
  void removeListeners();

  /**
   * Returns all channelIds that the packet listener registry has listeners registered
   */
  @NonNull
  @UnmodifiableView Collection<Integer> channels();

  /**
   * Returns all listeners by all channels from the registry
   */
  @NonNull
  @UnmodifiableView Collection<PacketListener> listeners();

  @NonNull
  @UnmodifiableView Map<Integer, Collection<PacketListener>> packetListeners();

  /**
   * Handles an incoming packet and invoke all listeners that are registered in this registry
   *
   * @param channel the channel, from that the packet was received
   * @param packet  the packet that should handle
   */
  void handlePacket(@NonNull NetworkChannel channel, @NonNull Packet packet);
}
