package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;

public class ChunkedPacket extends Packet {

    public static final int DEFAULT_CHUNK_SIZE = 131072;

    private int id;
    private int chunkSize;
    private int dataLength;
    private boolean end;
    private byte[] data;

    public ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, int id, int chunkSize, int dataLength, boolean end, byte[] data) {
        super(channel, uniqueId, header);
        this.id = id;
        this.chunkSize = chunkSize;
        this.dataLength = dataLength;
        this.data = data;
        this.end = end;
    }

    public ChunkedPacket(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, ProtocolBuffer body) {
        super(channel, uniqueId, header, body);
    }

    public static void createChunkedPackets(InputStream stream, JsonDocument header, int channel, Consumer<ChunkedPacket> consumer) throws IOException {
        createChunkedPackets(stream, header, channel, DEFAULT_CHUNK_SIZE, consumer);
    }

    public static void createChunkedPackets(InputStream stream, JsonDocument header, int channel, int chunkSize, Consumer<ChunkedPacket> consumer) throws IOException {
        UUID uniqueId = UUID.randomUUID();

        consumer.accept(new ChunkedPacket(channel, uniqueId, header, 0, chunkSize, 0, false, new byte[0]).fillBuffer());

        int id = 1;

        byte[] buffer = new byte[chunkSize];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            consumer.accept(new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id++, 0, read, false, buffer).fillBuffer());
        }

        consumer.accept(new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, 0, 0, true, new byte[0]).fillBuffer());
    }

    public ChunkedPacket fillBuffer() {
        if (this.id == 0) {
            super.body = ProtocolBuffer.create().writeVarInt(0).writeInt(chunkSize);
            return this;
        }

        super.body = ProtocolBuffer.create()
                .writeVarInt(this.id)
                .writeBoolean(this.end);
        if (this.end) {
            return this;
        }

        super.body.writeBytes(this.data, 0, this.dataLength);

        return this;
    }

    public void readBuffer(ChunkedPacket startPacket) {

        ProtocolBuffer body = super.body;

        this.id = body.readVarInt();
        if (this.id == 0) {
            this.chunkSize = body.readInt();
            return;
        }

        this.end = body.readBoolean();
        if (this.end) {
            return;
        }

        this.dataLength = startPacket.chunkSize;

        this.data = new byte[this.dataLength];
        body.readBytes(this.data);
    }

    public int getId() {
        return this.id;
    }

    public int getChunkSize() {
        return this.chunkSize;
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
}
