package de.dytanic.cloudnet.driver.network.def.internal;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the internal api channel for synchronized communication between driver api and cloudnet node.
 * This Class is unsafe about the architecture, because the complete network framework based of async listening.
 * <p>
 * It's only for the base API by the CloudNetDriver class and the Bridge API
 *
 * @see de.dytanic.cloudnet.driver.CloudNetDriver
 */
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
                syncEntry.response = packet;
                syncEntry.task.call();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            WAITING_PACKETS.remove(packet.getUniqueId());

            return true;

        } else {
            return false;
        }
    }

    @NotNull
    public static ITask<IPacket> sendCallablePacket(@NotNull INetworkChannel channel, @NotNull JsonDocument header, byte[] body) {
        return sendCallablePacket(channel, header, body, null);
    }

    @NotNull
    public static ITask<IPacket> sendCallablePacket(@NotNull INetworkChannel channel, @NotNull JsonDocument header, byte[] body, ITaskListener<IPacket> listener) {
        Packet packet = new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, header, body);
        checkCachedValidation();

        SynchronizedCallback syncEntry = new SynchronizedCallback();
        syncEntry.task = new ListenableTask<>(() -> syncEntry.response, listener);

        WAITING_PACKETS.put(packet.getUniqueId(), syncEntry);
        channel.sendPacket(packet);

        return syncEntry.task;
    }

    private static void checkCachedValidation() {
        long systemCurrent = System.currentTimeMillis();

        for (Map.Entry<UUID, SynchronizedCallback> entry : WAITING_PACKETS.entrySet()) {
            if (entry.getValue().timeOut < systemCurrent) {
                WAITING_PACKETS.remove(entry.getKey());

                try {
                    entry.getValue().response = Packet.EMPTY;
                    entry.getValue().task.call();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    private static class SynchronizedCallback {

        private final long timeOut = System.currentTimeMillis() + 30000;
        private IPacket response;
        private volatile ITask<IPacket> task;
    }
}