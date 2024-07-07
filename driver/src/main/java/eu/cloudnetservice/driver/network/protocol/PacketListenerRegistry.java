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
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A packet listener registry is responsible for registering packet listeners and posting packet listeners to their
 * handler. Multiple packet listeners can be registered to a packet listener registry. If a parent registry is set, the
 * received packet will be posted to the parent registry first.
 *
 * @since 4.0
 */
public interface PacketListenerRegistry {

  /**
   * Get the parent listener registry of this registry or null if no parent registry is present.
   *
   * @return the parent listener registry of this registry.
   */
  @Nullable
  PacketListenerRegistry parent();

  /**
   * Adds a packet listener for each packet that uses the provided channel id. Multiple listeners for a channel are
   * indeed possible and will be called in the order of registration.
   * <p>
   * The only channel to which no listener can be registered is the channel with the id -1 as it is used for query
   * responses and therefore reserved.
   * <p>
   * This method takes a class instead of an instance and creates the instance using our dependency injection framework.
   * Make sure that the given class supports the instantiation using dependency injection.
   *
   * @param channel       the channel the listener wants to listen to.
   * @param listenerClass the listener class to instantiate and register.
   * @throws NullPointerException     if the given listeners are null.
   * @throws IllegalArgumentException if the given channel id is invalid.
   */
  void addListener(int channel, @NonNull Class<? extends PacketListener> listenerClass);

  /**
   * Adds a packet listener for each packet that uses the provided channel id. Multiple listeners for a channel are
   * indeed possible and will be called in the order of registration.
   * <p>
   * The only channel to which no listener can be registered is the channel with the id -1 as it is used for query
   * responses and therefore reserved.
   *
   * @param channel  the channel the listener wants to listen to.
   * @param listener the listeners to register for the channel.
   * @throws NullPointerException     if the given listeners are null.
   * @throws IllegalArgumentException if the given channel id is invalid.
   */
  void addListener(int channel, @NonNull PacketListener listener);

  /**
   * Removes the given listener from the given channel if it was registered previously.
   *
   * @param channel  the id of the channel to remove the listener from.
   * @param listener the listener to remove from the channel.
   * @throws NullPointerException if the given listeners are null.
   */
  void removeListener(int channel, @NonNull PacketListener listener);

  /**
   * Removes all previously registered listeners from the given channel.
   *
   * @param channel the id of the channel to remove the listeners from.
   */
  void removeListeners(int channel);

  /**
   * Removes all listeners from this registry whose classes were loaded by the given class loader.
   *
   * @param classLoader the class loader of the packet listeners to unregister.
   * @throws NullPointerException if the given class loader is null.
   */
  void removeListeners(@NonNull ClassLoader classLoader);

  /**
   * Removes all listeners from this registry which are registered to any channel.
   */
  void removeListeners();

  /**
   * Checks if this registry has at least one listener for the specified channel.
   *
   * @param channel the channel to check.
   * @return true if at least one listener is registered to the channel, false otherwise.
   */
  boolean hasListeners(int channel);

  /**
   * Get all channel ids to which a listener was registered previously.
   *
   * @return all channels which have a listener in this registry.
   */
  @NonNull
  @UnmodifiableView
  Collection<Integer> channels();

  /**
   * Get all listener instances which are registered to any channel in this registry.
   *
   * @return all listener instances registered in this registry.
   */
  @NonNull
  @UnmodifiableView
  Collection<PacketListener> listeners();

  /**
   * Get a channel id - listeners mapping of listeners which were registered to this registry.
   *
   * @return all channel ids mapped to the listeners this registry has listeners for.
   */
  @NonNull
  @UnmodifiableView
  Map<Integer, Collection<PacketListener>> packetListeners();

  /**
   * Handles an incoming packets and post this to all listeners which are registered to the channel the packet was sent
   * to. The parent listener registry (if one exists) is called before this one. The listeners are handled in a frfc
   * (first registered, first called) order.
   *
   * @param channel the channel from which the packet came.
   * @param packet  the packet which was received.
   * @return true if a listener handled the packet, false otherwise.
   * @throws NullPointerException if either the given channel or packet is null.
   */
  boolean handlePacket(@NonNull NetworkChannel channel, @NonNull Packet packet);
}
