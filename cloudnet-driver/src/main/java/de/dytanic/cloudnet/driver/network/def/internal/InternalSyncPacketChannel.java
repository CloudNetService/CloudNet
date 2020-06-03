package de.dytanic.cloudnet.driver.network.def.internal;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class InternalSyncPacketChannel {

    private final static Map<UUID, SynchronizedCallback> WAITING_PACKETS = new ConcurrentHashMap<>();

    private InternalSyncPacketChannel() {
        throw new UnsupportedOperationException();
    }

    public static boolean handleIncomingChannel(Packet packet) {
        Preconditions.checkNotNull(packet);

        if (WAITING_PACKETS.containsKey(packet.getUniqueId())) {
            try {
                SynchronizedCallback syncEntry = WAITING_PACKETS.get(packet.getUniqueId());
                syncEntry.consumer.accept(packet);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            WAITING_PACKETS.remove(packet.getUniqueId());

            return true;

        } else {
            return false;
        }
    }

    public static void registerQueryHandler(UUID uniqueId, Consumer<IPacket> consumer) {
        checkCachedValidation();
        WAITING_PACKETS.put(uniqueId, new SynchronizedCallback(consumer));
    }

    private static void checkCachedValidation() {
        long systemCurrent = System.currentTimeMillis();

        for (Map.Entry<UUID, SynchronizedCallback> entry : WAITING_PACKETS.entrySet()) {
            if (entry.getValue().timeOut < systemCurrent) {
                WAITING_PACKETS.remove(entry.getKey());

                try {
                    entry.getValue().consumer.accept(Packet.EMPTY);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    private static class SynchronizedCallback {

        private final long timeOut = System.currentTimeMillis() + 30000;
        private final Consumer<IPacket> consumer;

        public SynchronizedCallback(Consumer<IPacket> consumer) {
            this.consumer = consumer;
        }
    }
}