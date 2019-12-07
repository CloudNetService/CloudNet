package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

import java.util.*;

/**
 * Default IPacketListenerRegistry implementation
 */
public final class DefaultPacketListenerRegistry implements PacketListenerRegistry {

    private final Map<Integer, List<PacketListener>> listeners = Maps.newConcurrentHashMap();

    private final PacketListenerRegistry parent;

    public DefaultPacketListenerRegistry() {
        this.parent = null;
    }

    public DefaultPacketListenerRegistry(PacketListenerRegistry parent) {
        this.parent = parent;
    }

    @Override
    public void addListener(int channel, PacketListener... listeners) {
        Validate.checkNotNull(listeners);

        if (!this.listeners.containsKey(channel)) {
            this.listeners.put(channel, Iterables.newCopyOnWriteArrayList());
        }

        for (PacketListener listener : listeners) {
            Validate.checkNotNull(listener);
            this.listeners.get(channel).add(listener);
        }
    }

    @Override
    public void removeListener(int channel, PacketListener... listeners) {
        Validate.checkNotNull(listeners);

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
        for (Map.Entry<Integer, List<PacketListener>> listenerCollectionEntry : this.listeners.entrySet()) {
            for (PacketListener listener : listenerCollectionEntry.getValue()) {
                if (listener.getClass().getClassLoader().equals(classLoader)) {
                    listenerCollectionEntry.getValue().remove(listener);
                }
            }
        }
    }

    @Override
    public boolean hasListener(Class<? extends PacketListener> clazz) {
        for (Map.Entry<Integer, List<PacketListener>> listenerCollectionEntry : this.listeners.entrySet()) {
            for (PacketListener listener : listenerCollectionEntry.getValue()) {
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
    public Collection<PacketListener> getListeners() {
        Collection<PacketListener> listeners = Iterables.newCopyOnWriteArrayList();

        for (List<PacketListener> list : this.listeners.values()) {
            listeners.addAll(list);
        }

        return listeners;
    }

    @Override
    public void handlePacket(INetworkChannel channel, Packet packet) {
        Validate.checkNotNull(packet);

        if (this.parent != null) {
            this.parent.handlePacket(channel, packet);
        }

        if (this.listeners.containsKey(packet.getChannel())) {
            for (PacketListener listener : this.listeners.get(packet.getChannel())) {
                try {
                    listener.handle(channel, packet);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public PacketListenerRegistry getParent() {
        return this.parent;
    }
}