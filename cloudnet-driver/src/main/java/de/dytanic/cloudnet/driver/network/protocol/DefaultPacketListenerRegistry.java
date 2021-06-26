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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default IPacketListenerRegistry implementation
 */
public final class DefaultPacketListenerRegistry implements IPacketListenerRegistry {

  private final Map<Integer, List<IPacketListener>> listeners = new ConcurrentHashMap<>();

  private final IPacketListenerRegistry parent;

  public DefaultPacketListenerRegistry() {
    this.parent = null;
  }

  public DefaultPacketListenerRegistry(IPacketListenerRegistry parent) {
    this.parent = parent;
  }

  @Override
  public void addListener(int channel, IPacketListener... listeners) {
    Preconditions.checkNotNull(listeners);

    if (!this.listeners.containsKey(channel)) {
      this.listeners.put(channel, new CopyOnWriteArrayList<>());
    }

    for (IPacketListener listener : listeners) {
      Preconditions.checkNotNull(listener);
      this.listeners.get(channel).add(listener);
    }
  }

  @Override
  public void removeListener(int channel, IPacketListener... listeners) {
    Preconditions.checkNotNull(listeners);

    if (this.listeners.containsKey(channel)) {
      this.listeners.get(channel).removeAll(Arrays.asList(listeners));

      if (this.listeners.get(channel).isEmpty()) {
        this.listeners.remove(channel);
      }
    }
  }

  @Override
  public void removeListeners(int channel) {
    if (this.listeners.containsKey(channel)) {
      this.listeners.get(channel).clear();
      this.listeners.remove(channel);
    }
  }

  @Override
  public void removeListeners(ClassLoader classLoader) {
    for (Map.Entry<Integer, List<IPacketListener>> listenerCollectionEntry : this.listeners.entrySet()) {
      listenerCollectionEntry.getValue().removeIf(listener -> listener.getClass().getClassLoader().equals(classLoader));
    }
  }

  @Override
  public boolean hasListener(Class<? extends IPacketListener> clazz) {
    for (Map.Entry<Integer, List<IPacketListener>> listenerCollectionEntry : this.listeners.entrySet()) {
      for (IPacketListener listener : listenerCollectionEntry.getValue()) {
        if (listener.getClass().equals(clazz)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public void removeListeners() {
    this.listeners.clear();
  }

  @Override
  public Collection<Integer> getChannels() {
    return Collections.unmodifiableCollection(this.listeners.keySet());
  }

  @Override
  public Collection<IPacketListener> getListeners() {
    Collection<IPacketListener> listeners = new CopyOnWriteArrayList<>();

    for (List<IPacketListener> list : this.listeners.values()) {
      listeners.addAll(list);
    }

    return listeners;
  }

  @Override
  public void handlePacket(INetworkChannel channel, IPacket packet) {
    Preconditions.checkNotNull(packet);

    if (packet.isShowDebug() && this.parent == null) {
      CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
        if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
          cloudNetDriver.getLogger().debug(
            String.format(
              "Handling packet by %s on channel %d with id %s, header=%s;body=%d",
              channel.getClientAddress().toString(),
              packet.getChannel(),
              packet.getUniqueId().toString(),
              packet.getHeader().toJson(),
              packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
            )
          );
        }
      });
    }

    if (this.parent != null) {
      this.parent.handlePacket(channel, packet);
    }

    if (this.listeners.containsKey(packet.getChannel())) {
      for (IPacketListener listener : this.listeners.get(packet.getChannel())) {
        try {
          listener.handle(channel, packet);
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  }

  public IPacketListenerRegistry getParent() {
    return this.parent;
  }
}
