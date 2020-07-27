package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
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
        ChunkedPacket chunkedPacket = new ChunkedPacket(packet.getChannel(), packet.getUniqueId(), packet.getHeader(), packet.getBuffer());

        this.lock.lock();
        try {
            if (!this.sessions.containsKey(packet.getUniqueId())) {
                chunkedPacket.readBuffer(null);

                this.sessions.put(packet.getUniqueId(), this.createSession(chunkedPacket, new HashMap<>()));
                return;
            }
        } finally {
            this.lock.unlock();
        }

        ChunkedPacketSession session = this.sessions.get(packet.getUniqueId());
        chunkedPacket.readBuffer(session.getStartPacket());

        this.handlePacket(session, chunkedPacket);
    }

    private void handlePacket(ChunkedPacketSession session, ChunkedPacket packet) throws IOException {
        session.getLock().lock();

        try {
            session.handlePacket(packet);
        } finally {
            session.getLock().unlock();
        }
    }

    public Map<UUID, ChunkedPacketSession> getSessions() {
        return this.sessions;
    }

    @NotNull
    protected ChunkedPacketSession createSession(ChunkedPacket startPacket, Map<String, Object> properties) throws IOException {
        return new ChunkedPacketSession(this, this.createOutputStream(startPacket, properties), startPacket, properties);
    }

    @NotNull
    protected abstract OutputStream createOutputStream(ChunkedPacket startPacket, Map<String, Object> properties) throws IOException;

    protected void handleComplete(ChunkedPacketSession session) throws IOException {
    }

}
