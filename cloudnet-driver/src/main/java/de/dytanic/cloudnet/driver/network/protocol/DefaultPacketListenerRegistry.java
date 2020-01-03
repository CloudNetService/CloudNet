package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

import java.util.*;

/**
 * Default IPacketListenerRegistry implementation
 */
public final class DefaultPacketListenerRegistry implements IPacketListenerRegistry {

    private final Map<Integer, List<IPacketListener>> listeners = Maps.newConcurrentHashMap();

    private final IPacketListenerRegistry parent;

    public DefaultPacketListenerRegistry() {
        this.parent = null;
    }

    public DefaultPacketListenerRegistry(IPacketListenerRegistry parent) {
        this.parent = parent;
    }

    @Override
    public void addListener(int channel, IPacketListener... listeners) {
        Validate.checkNotNull(listeners);

        if (!this.listeners.containsKey(channel)) {
            this.listeners.put(channel, Iterables.newCopyOnWriteArrayList());
        }

        for (IPacketListener listener : listeners) {
            Validate.checkNotNull(listener);
            this.listeners.get(channel).add(listener);
        }
    }

    @Override
    public void removeListener(int channel, IPacketListener... listeners) {
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
        for (Map.Entry<Integer, List<IPacketListener>> listenerCollectionEntry : this.listeners.entrySet()) {
            for (IPacketListener listener : listenerCollectionEntry.getValue()) {
                if (listener.getClass().getClassLoader().equals(classLoader)) {
                    listenerCollectionEntry.getValue().remove(listener);
                }
            }
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
        Collection<IPacketListener> listeners = Iterables.newCopyOnWriteArrayList();

        for (List<IPacketListener> list : this.listeners.values()) {
            listeners.addAll(list);
        }

        return listeners;
    }

    @Override
    public void handlePacket(INetworkChannel channel, IPacket packet) {
        Validate.checkNotNull(packet);

        if (packet.isShowDebug()) {
            CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> cloudNetDriver.getLogger().debug(
                    String.format(
                            "Handling packet by %s on channel %d with id %s, header=%s;body=%d",
                            channel.getClientAddress().toString(),
                            packet.getChannel(),
                            packet.getUniqueId().toString(),
                            packet.getHeader().toJson(),
                            packet.getBody() != null ? packet.getBody().length : 0
                    )
            ));
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