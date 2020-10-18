package de.dytanic.cloudnet.driver.network.def.internal;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class InternalSyncPacketChannel {

    private final static Map<UUID, SynchronizedCallback> WAITING_PACKETS = new ConcurrentHashMap<>();

    private InternalSyncPacketChannel() {
        throw new UnsupportedOperationException();
    }

    public static boolean handleIncomingChannel(Packet packet) {
        Preconditions.checkNotNull(packet);

        SynchronizedCallback callback = WAITING_PACKETS.remove(packet.getUniqueId());
        if (callback != null) {
            try {
                callback.future.complete(packet);
            } catch (Throwable throwable) {
                callback.future.fail(throwable);
            }
        }

        return callback != null;
    }

    public static void registerQueryHandler(UUID uniqueId, CompletableTask<IPacket> consumer) {
        checkCachedValidation();
        WAITING_PACKETS.put(uniqueId, new SynchronizedCallback(consumer));
    }

    private static void checkCachedValidation() {
        long systemCurrent = System.currentTimeMillis();

        for (Map.Entry<UUID, SynchronizedCallback> entry : WAITING_PACKETS.entrySet()) {
            if (entry.getValue().timeOut < systemCurrent) {
                WAITING_PACKETS.remove(entry.getKey());

                try {
                    entry.getValue().future.complete(Packet.EMPTY);
                } catch (Throwable throwable) {
                    entry.getValue().future.fail(throwable);
                }
            }
        }
    }

    private static class SynchronizedCallback {

        private final long timeOut = System.currentTimeMillis() + 30000;
        private final CompletableTask<IPacket> future;

        public SynchronizedCallback(CompletableTask<IPacket> future) {
            this.future = future;
        }
    }
}