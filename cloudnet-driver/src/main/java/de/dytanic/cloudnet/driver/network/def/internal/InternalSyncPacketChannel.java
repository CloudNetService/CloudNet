package de.dytanic.cloudnet.driver.network.def.internal;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedQueryResponse;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ConsumingChunkedPacketListener;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class InternalSyncPacketChannel {

    private final static Map<UUID, SynchronizedCallback> WAITING_PACKETS = new ConcurrentHashMap<>();

    private InternalSyncPacketChannel() {
        throw new UnsupportedOperationException();
    }

    public static boolean handleIncomingChannel(INetworkChannel channel, Packet packet) {
        Preconditions.checkNotNull(packet);

        if (WAITING_PACKETS.containsKey(packet.getUniqueId())) {
            SynchronizedCallback syncEntry = null;

            try {
                syncEntry = WAITING_PACKETS.get(packet.getUniqueId());
                syncEntry.consumer.accept(channel, packet);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (syncEntry != null && syncEntry.autoRemove) {
                WAITING_PACKETS.remove(packet.getUniqueId());
            }

            return true;

        } else {
            return false;
        }
    }

    public static void removeEntry(UUID uniqueId) {
        WAITING_PACKETS.remove(uniqueId);
    }

    public static void registerChunkedQueryHandler(UUID uniqueId, Consumer<ChunkedQueryResponse> consumer) {
        ChunkedPacketListener listener = new ConsumingChunkedPacketListener(response -> {
            removeEntry(uniqueId);
            consumer.accept(response);
        });

        registerQueryHandler(uniqueId, false, (channel, packet) -> {
            try {
                listener.handle(channel, packet);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public static void registerQueryHandler(UUID uniqueId, Consumer<IPacket> consumer) {
        registerQueryHandler(uniqueId, true, (channel, packet) -> consumer.accept(packet));
    }

    private static void registerQueryHandler(UUID uniqueId, boolean autoRemove, BiConsumer<INetworkChannel, IPacket> consumer) {
        checkCachedValidation();
        WAITING_PACKETS.put(uniqueId, new SynchronizedCallback(autoRemove, consumer));
    }

    private static void checkCachedValidation() {
        long systemCurrent = System.currentTimeMillis();

        for (Map.Entry<UUID, SynchronizedCallback> entry : WAITING_PACKETS.entrySet()) {
            if (entry.getValue().autoRemove && entry.getValue().timeOut < systemCurrent) {
                WAITING_PACKETS.remove(entry.getKey());

                try {
                    entry.getValue().consumer.accept(null, Packet.EMPTY);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    private static class SynchronizedCallback {

        private final long timeOut = System.currentTimeMillis() + 30000;
        private final boolean autoRemove;
        private final BiConsumer<INetworkChannel, IPacket> consumer;

        public SynchronizedCallback(boolean autoRemove, BiConsumer<INetworkChannel, IPacket> consumer) {
            this.autoRemove = autoRemove;
            this.consumer = consumer;
        }
    }
}