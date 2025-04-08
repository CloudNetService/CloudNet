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

package eu.cloudnetservice.driver.impl.network.protocol;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of the packet listener registry.
 *
 * @since 4.0
 */
public class DefaultPacketListenerRegistry implements PacketListenerRegistry {

  private final PacketListenerRegistry parent;
  private final Multimap<Integer, PacketListener> listeners;

  /**
   * Constructs a new packet listener registry instance with no parent packet listener given. This call is equivalent to
   * {@code new DefaultPacketListenerRegistry(null)}.
   */
  public DefaultPacketListenerRegistry() {
    this(null);
  }

  /**
   * Constructs a new packet listener registry instance.
   *
   * @param parent the parent registry or null if no parent registry should be set.
   */
  public DefaultPacketListenerRegistry(@Nullable PacketListenerRegistry parent) {
    this.parent = parent;
    this.listeners = Multimaps.newMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable PacketListenerRegistry parent() {
    return this.parent;
  }

  @Override
  public void addListener(int channel, @NonNull Class<? extends PacketListener> listenerClass) {
    // validate that the user is not trying to listen to a reserved channel
    Preconditions.checkArgument(channel != -1, "Tried to register listeners to forbidden channel id -1");
    this.listeners.put(channel, InjectionLayer.findLayerOf(listenerClass).instance(listenerClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(int channel, @NonNull PacketListener listener) {
    // validate that the user is not trying to listen to a reserved channel
    Preconditions.checkArgument(channel != -1, "Tried to register listeners to forbidden channel id -1");
    this.listeners.put(channel, listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(int channel, @NonNull PacketListener listener) {
    var registeredListeners = this.listeners.get(channel);
    if (!registeredListeners.isEmpty()) {
      registeredListeners.remove(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListeners(int channel) {
    this.listeners.removeAll(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListeners(@NonNull ClassLoader classLoader) {
    for (var entry : this.listeners.entries()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasListeners(int channel) {
    return this.listeners.containsKey(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListeners() {
    this.listeners.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @UnmodifiableView Collection<Integer> channels() {
    return Collections.unmodifiableCollection(this.listeners.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @UnmodifiableView Collection<PacketListener> listeners() {
    return this.listeners.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @UnmodifiableView Map<Integer, Collection<PacketListener>> packetListeners() {
    return Collections.unmodifiableMap(this.listeners.asMap());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlePacket(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // check if the parent handler registry is present and can handle the packet
    var parentDidHandle = this.parent != null && this.parent.handlePacket(channel, packet);

    // get the listeners that are registered in this packet registry for the packet channel
    var registeredListeners = this.listeners.get(packet.channel());
    if (registeredListeners.isEmpty()) {
      return parentDidHandle;
    }

    // post the packet to each listener that handles the packet
    for (var listener : registeredListeners) {
      try {
        listener.handle(channel, packet);
      } catch (Exception exception) {
        throw new IllegalStateException(String.format(
          "Exception posting packet from channel %d to handler %s",
          packet.channel(), listener.getClass().getCanonicalName()
        ), exception);
      }
    }

    // at least one listener handled the packet
    return true;
  }
}
