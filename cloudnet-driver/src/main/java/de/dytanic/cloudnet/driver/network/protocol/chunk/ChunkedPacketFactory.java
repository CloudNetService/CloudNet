package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class ChunkedPacketFactory {

    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;

    private static ChunkedPacket createStartPacket(int channel, UUID uniqueId, JsonDocument header, int chunkSize) {
        return new ChunkedPacket(channel, uniqueId, header, 0, chunkSize, chunkSize, false, new byte[0], 0);
    }

    private static ChunkedPacket createSegment(int channel, UUID uniqueId, int id, int chunkSize, int length, byte[] data) {
        return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, length, false, data, 0);
    }

    private static ChunkedPacket createEndPacket(int channel, UUID uniqueId, int id, int chunkSize) {
        return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, 0, true, new byte[0], id - 1);
    }

    public static boolean createChunkedPackets(InputStream stream, JsonDocument header, int channel, Consumer<ChunkedPacket> consumer) throws IOException {
        return createChunkedPackets(stream, header, channel, DEFAULT_CHUNK_SIZE, consumer);
    }

    public static boolean createChunkedPackets(InputStream stream, JsonDocument header, int channel, int chunkSize, Consumer<ChunkedPacket> consumer) throws IOException {
        UUID uniqueId = UUID.randomUUID();

        try {
            consumer.accept(createStartPacket(channel, uniqueId, header, chunkSize));

            int chunkId = 1;

            int read;
            byte[] buffer = new byte[chunkSize];
            while ((read = stream.read(buffer)) != -1) {
                consumer.accept(createSegment(channel, uniqueId, chunkId++, chunkSize, read, Arrays.copyOf(buffer, buffer.length)));
            }

            consumer.accept(createEndPacket(channel, uniqueId, chunkId, chunkSize));

            return true;
        } catch (ChunkInterrupt interrupt) {
            return false;
        }
    }

}
