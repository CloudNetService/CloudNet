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

package de.dytanic.cloudnet.driver.network.protocol.defaults;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Default IPacketListenerRegistry implementation
 */
public class DefaultPacketListenerRegistry implements IPacketListenerRegistry {

  private final IPacketListenerRegistry parent;
  private final Map<Integer, Collection<IPacketListener>> listeners;

  public DefaultPacketListenerRegistry() {
    this(null);
  }

  public DefaultPacketListenerRegistry(IPacketListenerRegistry parent) {
    this.parent = parent;
    this.listeners = new ConcurrentHashMap<>();
  }

  @Override
  public @Nullable IPacketListenerRegistry getParent() {
    return this.parent;
  }

  @Override
  public void addListener(int channel, @NotNull IPacketListener... listeners) {
    this.listeners.computeIfAbsent(channel, $ -> new CopyOnWriteArraySet<>()).addAll(Arrays.asList(listeners));
  }

  @Override
  public void removeListener(int channel, @NotNull IPacketListener... listeners) {
    Collection<IPacketListener> registeredListeners = this.listeners.get(channel);
    if (registeredListeners != null) {
      registeredListeners.removeAll(Arrays.asList(listeners));
      if (registeredListeners.isEmpty()) {
        this.listeners.remove(channel, registeredListeners);
      }
    }
  }

  @Override
  public void removeListeners(int channel) {
    this.listeners.remove(channel);
  }

  @Override
  public void removeListeners(@NotNull ClassLoader classLoader) {
    for (Entry<Integer, Collection<IPacketListener>> entry : this.listeners.entrySet()) {
      entry.getValue().removeIf(listener -> listener.getClass().getClassLoader().equals(classLoader));
      if (entry.getValue().isEmpty()) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public boolean hasListener(@NotNull Class<? extends IPacketListener> clazz) {
    for (Collection<IPacketListener> value : this.listeners.values()) {
      if (value.getClass().equals(clazz)) {
        return true;
      }
    }
    return false;
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
  public @NotNull @UnmodifiableView Collection<Integer> getChannels() {
    return Collections.unmodifiableCollection(this.listeners.keySet());
  }

  @Override
  public @NotNull @UnmodifiableView Collection<IPacketListener> getListeners() {
    return this.listeners.values().stream()
      .flatMap(Collection::stream)
      .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableCollection));
  }

  @Override
  public @NotNull @UnmodifiableView Map<Integer, ? super Collection<IPacketListener>> getPacketListeners() {
    return Collections.unmodifiableMap(this.listeners);
  }

  @Override
  public void handlePacket(@NotNull INetworkChannel channel, @NotNull IPacket packet) {
    if (this.parent != null) {
      this.parent.handlePacket(channel, packet);
    }

    Collection<IPacketListener> registeredListeners = this.listeners.get(packet.getChannel());
    if (registeredListeners != null) {
      for (IPacketListener listener : registeredListeners) {
        try {
          listener.handle(channel, packet);
        } catch (Exception exception) {
          throw new IllegalStateException(String.format(
            "Exception posting packet from channel %d to handler %s",
            packet.getChannel(), listener.getClass().getCanonicalName()
          ), exception);
        }
      }
    }
  }
}
