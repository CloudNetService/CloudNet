package de.dytanic.cloudnet.driver.network.protocol.chunk;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.OutputStream;

public class ChunkedPacketSession {

    private final ChunkedPacketListener listener;
    private final OutputStream outputStream;
    private final ChunkedPacket startPacket;
    private int id = 0;
    private boolean closed;

    public ChunkedPacketSession(ChunkedPacketListener listener, OutputStream outputStream, ChunkedPacket startPacket) {
        this.listener = listener;
        this.outputStream = outputStream;
        this.startPacket = startPacket;
    }

    public int getId() {
        return this.id;
    }

    public void handlePacket(ChunkedPacket packet) throws IOException {
        Preconditions.checkArgument(this.id + 1 == packet.getId() || packet.isEnd(), "wrong chunk id received", packet.getId(), this.id + 1);
        Preconditions.checkState(!this.closed, "session is already closed");

        ++this.id;

        if (packet.isEnd()) {
            this.close();
            return;
        }

        this.outputStream.write(packet.getData(), 0, packet.getDataLength());
    }

    protected void close() throws IOException {
        this.closed = true;
        this.listener.getSessions().remove(this.startPacket.getUniqueId());
        this.outputStream.close();
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public ChunkedPacket getStartPacket() {
        return this.startPacket;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
