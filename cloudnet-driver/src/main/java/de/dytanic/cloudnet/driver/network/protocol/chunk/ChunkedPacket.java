package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class ChunkedPacket extends Packet {

    private int chunkId;
    private int chunkSize;
    private int dataLength;
    private boolean end;
    private byte[] data;
    private int chunks;

    protected ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, int chunkId, int chunkSize, int dataLength, boolean end, byte[] data, int chunks) {
        super(channel, uniqueId, header);
        this.chunkId = chunkId;
        this.chunkSize = chunkSize;
        this.dataLength = dataLength;
        this.data = data;
        this.end = end;
        this.chunks = chunks;
    }

    protected ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, ProtocolBuffer body) {
        super(channel, uniqueId, header, body);
    }

    public static ChunkedPacket createIncomingPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, ProtocolBuffer body) {
        return new ChunkedPacket(channel, uniqueId, header, body);
    }

    public ChunkedPacket fillBuffer() {
        if (super.body != null) { // The buffer is filled already
            return this;
        }

        super.body = ProtocolBuffer.create().writeVarInt(this.chunkId);
        if (this.chunkId == 0) {
            super.body.writeInt(this.chunkSize);
            return this;
        }

        super.body.writeBoolean(this.end);
        if (this.end) {
            super.body.writeVarInt(this.chunks);
            return this;
        }

        super.body.writeInt(this.dataLength).writeBytes(this.data, 0, this.dataLength);
        return this;
    }

    public @NotNull ChunkedPacket readBuffer() {
        this.chunkId = super.body.readVarInt();
        if (this.chunkId == 0) {
            this.chunkSize = super.body.readInt();
            return this;
        }

        this.end = super.body.readBoolean();
        if (this.end) {
            this.chunks = super.body.readVarInt();
        }

        return this;
    }

    public void readData(@NotNull OutputStream outputStream) throws IOException {
        this.dataLength = body.readInt();
        body.readBytes(outputStream, this.dataLength);
    }

    public int getChunks() {
        return this.chunks;
    }

    public int getChunkId() {
        return this.chunkId;
    }

    public int getDataLength() {
        return this.dataLength;
    }

    public boolean isEnd() {
        return this.end;
    }

    public byte[] getData() {
        return this.data;
    }

    public void clearData() {
        if (super.body != null) {
            while (super.body.refCnt() > 0) {
                super.body.release();
            }
        }

        this.data = null;
    }
}
