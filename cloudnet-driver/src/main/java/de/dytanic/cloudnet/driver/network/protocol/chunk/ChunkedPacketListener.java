package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ChunkedPacketListener implements IPacketListener {

    private final Map<UUID, ChunkedPacketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        ChunkedPacket chunkedPacket = new ChunkedPacket(packet.getChannel(), packet.getUniqueId(), packet.getHeader(), packet.getBuffer());
        if (!this.sessions.containsKey(packet.getUniqueId())) {
            chunkedPacket.readBuffer(null);
            this.sessions.put(packet.getUniqueId(), this.createSession(chunkedPacket));
            return;
        }

        ChunkedPacketSession session = this.sessions.get(packet.getUniqueId());
        chunkedPacket.readBuffer(session.getStartPacket());

        session.handlePacket(chunkedPacket);
    }

    public Map<UUID, ChunkedPacketSession> getSessions() {
        return this.sessions;
    }

    @NotNull
    protected ChunkedPacketSession createSession(ChunkedPacket startPacket) throws IOException {
        return new ChunkedPacketSession(this, this.createOutputStream(startPacket), startPacket);
    }

    @NotNull
    protected abstract OutputStream createOutputStream(ChunkedPacket startPacket) throws IOException;

}
