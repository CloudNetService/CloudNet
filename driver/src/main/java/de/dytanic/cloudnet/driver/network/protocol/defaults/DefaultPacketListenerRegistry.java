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

package de.dytanic.cloudnet.driver.network.protocol.defaults;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.driver.network.protocol.PacketListenerRegistry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Default IPacketListenerRegistry implementation
 */
public class DefaultPacketListenerRegistry implements PacketListenerRegistry {

  private final PacketListenerRegistry parent;
  private final Multimap<Integer, PacketListener> listeners;

  public DefaultPacketListenerRegistry() {
    this(null);
  }

  public DefaultPacketListenerRegistry(PacketListenerRegistry parent) {
    this.parent = parent;
    this.listeners = Multimaps.newMultimap(new ConcurrentHashMap<>(), ConcurrentLinkedQueue::new);
  }

  @Override
  public @Nullable PacketListenerRegistry parent() {
    return this.parent;
  }

  @Override
  public void addListener(int channel, @NonNull PacketListener... listeners) {
    this.listeners.putAll(channel, List.of(listeners));
  }

  @Override
  public void removeListener(int channel, @NonNull PacketListener... listeners) {
    var registeredListeners = this.listeners.get(channel);
    if (!registeredListeners.isEmpty()) {
      // remove all listeners if no specific listeners are provided
      if (listeners.length == 0) {
        registeredListeners.clear();
        this.listeners.removeAll(channel);
      } else {
        // remove the selected listeners
        registeredListeners.removeAll(Arrays.asList(listeners));
      }
    }
  }

  @Override
  public void removeListeners(int channel) {
    this.listeners.removeAll(channel);
  }

  @Override
  public void removeListeners(@NonNull ClassLoader classLoader) {
    for (var entry : this.listeners.entries()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public boolean hasListeners(int channel) {
    return this.listeners.containsKey(channel);
  }

  @Override
  public void removeListeners() {
    this.listeners.clear();
  }

  @Override
  public @NonNull @UnmodifiableView Collection<Integer> channels() {
    return Collections.unmodifiableCollection(this.listeners.keySet());
  }

  @Override
  public @NonNull @UnmodifiableView Collection<PacketListener> listeners() {
    return this.listeners.values();
  }

  @Override
  public @NonNull @UnmodifiableView Map<Integer, Collection<PacketListener>> packetListeners() {
    return Collections.unmodifiableMap(this.listeners.asMap());
  }

  @Override
  public void handlePacket(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    if (this.parent != null) {
      this.parent.handlePacket(channel, packet);
    }

    var registeredListeners = this.listeners.get(packet.channel());
    if (!registeredListeners.isEmpty()) {
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
    }
  }
}
