package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.function.Consumer;

public class ChunkedPacket extends Packet {

    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;

    private int chunkId;
    private int chunkSize;
    private int dataLength;
    private boolean end;
    private byte[] data;
    private int chunks;

    private ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, int chunkId, int chunkSize, int dataLength, boolean end, byte[] data, int chunks) {
        super(channel, uniqueId, header);
        this.chunkId = chunkId;
        this.chunkSize = chunkSize;
        this.dataLength = dataLength;
        this.data = data;
        this.end = end;
        this.chunks = chunks;
    }

    public ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, ProtocolBuffer body) {
        super(channel, uniqueId, header, body);
    }

    private static ChunkedPacket createStartPacket(int channel, UUID uniqueId, JsonDocument header, int chunkSize) {
        return new ChunkedPacket(channel, uniqueId, header, 0, chunkSize, chunkSize, false, new byte[0], 0);
    }

    private static ChunkedPacket createSegment(int channel, UUID uniqueId, int id, int chunkSize, int length, byte[] data) {
        return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, length, false, data, 0);
    }

    private static ChunkedPacket createEndPacket(int channel, UUID uniqueId, int id, int chunkSize) {
        return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, 0, true, new byte[0], id - 1);
    }

    public static void createChunkedPackets(InputStream stream, JsonDocument header, int channel, Consumer<ChunkedPacket> consumer) throws IOException {
        createChunkedPackets(stream, header, channel, DEFAULT_CHUNK_SIZE, consumer);
    }

    public static void createChunkedPackets(InputStream stream, JsonDocument header, int channel, int chunkSize, Consumer<ChunkedPacket> consumer) throws IOException {
        UUID uniqueId = UUID.randomUUID();

        consumer.accept(createStartPacket(channel, uniqueId, header, chunkSize));

        int chunkId = 1;

        int read;
        byte[] buffer = new byte[chunkSize];
        while ((read = stream.read(buffer)) != -1) {
            consumer.accept(createSegment(channel, uniqueId, chunkId++, chunkSize, read, buffer));
        }

        consumer.accept(createEndPacket(channel, uniqueId, chunkId, chunkSize));
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
        ProtocolBuffer body = super.body;

        this.chunkId = body.readVarInt();
        if (this.chunkId == 0) {
            this.chunkSize = body.readInt();
            return this;
        }

        this.end = body.readBoolean();
        if (this.end) {
            this.chunks = body.readVarInt();
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
