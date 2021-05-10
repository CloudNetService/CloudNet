package de.dytanic.cloudnet.driver.network.protocol.chunk.listener;

import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedQueryResponse;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.function.Consumer;

public class ConsumingChunkedPacketListener extends CachedChunkedPacketListener {

    private final Consumer<ChunkedQueryResponse> consumer;

    public ConsumingChunkedPacketListener(Consumer<ChunkedQueryResponse> consumer) {
        this.consumer = consumer;
    }

    @Override
    protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream) {
        this.consumer.accept(new ChunkedQueryResponse(session, session.getFirstPacket(), session.getLastPacket(), inputStream));
    }
}
