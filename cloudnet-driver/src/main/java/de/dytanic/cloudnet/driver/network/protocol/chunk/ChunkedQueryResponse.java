package de.dytanic.cloudnet.driver.network.protocol.chunk;

import java.io.InputStream;

public class ChunkedQueryResponse {

    private final ChunkedPacket beginPacket;
    private final ChunkedPacket endPacket;
    private final InputStream inputStream;

    public ChunkedQueryResponse(ChunkedPacket beginPacket, ChunkedPacket endPacket, InputStream inputStream) {
        this.beginPacket = beginPacket;
        this.endPacket = endPacket;
        this.inputStream = inputStream;
    }

    public ChunkedPacket getBeginPacket() {
        return this.beginPacket;
    }

    public ChunkedPacket getEndPacket() {
        return this.endPacket;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }
}
