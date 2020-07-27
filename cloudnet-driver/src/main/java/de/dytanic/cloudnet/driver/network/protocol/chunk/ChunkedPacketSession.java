package de.dytanic.cloudnet.driver.network.protocol.chunk;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ChunkedPacketSession {

    private final ChunkedPacketListener listener;
    private final OutputStream outputStream;
    private final ChunkedPacket startPacket;
    private final Lock lock = new ReentrantLock();
    private final Collection<ChunkedPacket> pendingPackets = new CopyOnWriteArrayList<>();
    private final Map<String, Object> properties;
    private int chunkId = 1;
    private volatile boolean closed;

    public ChunkedPacketSession(ChunkedPacketListener listener, OutputStream outputStream, ChunkedPacket startPacket, Map<String, Object> properties) {
        this.listener = listener;
        this.outputStream = outputStream;
        this.startPacket = startPacket;
        this.properties = properties;
    }

    public int getCurrentChunkId() {
        return this.chunkId;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void handlePacket(ChunkedPacket packet) throws IOException {
        Preconditions.checkState(!this.closed, "session is already closed", new Object[]{packet.getChunkId(), packet.isEnd()});

        if (this.chunkId != packet.getChunkId()) {
            this.pendingPackets.add(packet);
            this.checkPendingPackets();
            return;
        }

        this.handlePacket0(packet);
        this.checkPendingPackets();
    }

    private void handlePacket0(ChunkedPacket packet) throws IOException {
        if (this.closed) {
            return;
        }

        if (packet.isEnd()) {
            this.close();
            return;
        }

        ++this.chunkId;

        this.outputStream.write(packet.getData(), 0, packet.getDataLength());
        this.outputStream.flush();

        packet.clearData();
    }

    private void checkPendingPackets() throws IOException {
        if (!this.pendingPackets.isEmpty()) {
            for (ChunkedPacket pendingPacket : this.pendingPackets) {
                if (this.chunkId == pendingPacket.getChunkId() || (pendingPacket.isEnd() && this.chunkId - 1 == pendingPacket.getChunks())) {
                    this.pendingPackets.remove(pendingPacket);
                    this.handlePacket0(pendingPacket);
                }
            }
        }
    }

    protected void close() throws IOException {
        if (!this.pendingPackets.isEmpty()) {
            String packets = this.pendingPackets.stream().map(ChunkedPacket::getChunkId).map(String::valueOf).collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format("Closing with %d pending packets: %s", this.pendingPackets.size(), packets));
        }

        this.closed = true;
        this.listener.getSessions().remove(this.startPacket.getUniqueId());
        this.outputStream.close();
        this.listener.handleComplete(this);
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public ChunkedPacket getStartPacket() {
        return this.startPacket;
    }

    public JsonDocument getHeader() {
        return this.startPacket.getHeader();
    }

    public boolean isClosed() {
        return this.closed;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
