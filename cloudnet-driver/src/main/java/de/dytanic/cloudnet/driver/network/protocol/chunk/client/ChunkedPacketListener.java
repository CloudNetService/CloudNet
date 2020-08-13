package de.dytanic.cloudnet.driver.network.protocol.chunk.client;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ChunkedPacketListener implements IPacketListener {

    private final Lock lock = new ReentrantLock();
    private final Map<UUID, ChunkedPacketSession> sessions = new HashMap<>();

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        this.lock.lock();
        try {
            ChunkedPacket chunk = ChunkedPacket.createIncomingPacket(packet.getChannel(), packet.getUniqueId(), packet.getHeader(), packet.getBuffer()).readBuffer();
            if (!this.sessions.containsKey(packet.getUniqueId())) {
                this.sessions.put(packet.getUniqueId(), this.createSession(packet.getUniqueId(), new HashMap<>()));
            }

            this.sessions.get(packet.getUniqueId()).handleIncomingChunk(chunk);
        } finally {
            this.lock.unlock();
        }
    }

    public @NotNull Map<UUID, ChunkedPacketSession> getSessions() {
        return this.sessions;
    }

    @NotNull
    protected ChunkedPacketSession createSession(@NotNull UUID sessionUniqueId, @NotNull Map<String, Object> properties) throws IOException {
        return new ChunkedPacketSession(this, sessionUniqueId, this.createOutputStream(sessionUniqueId, properties), properties);
    }

    @NotNull
    protected abstract OutputStream createOutputStream(@NotNull UUID sessionUniqueId, @NotNull Map<String, Object> properties) throws IOException;

    protected void handleComplete(@NotNull ChunkedPacketSession session) throws IOException {
    }
}
