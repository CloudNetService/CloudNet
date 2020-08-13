package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class ChunkedPacketProvider {

    public static boolean sendChunkedPackets(InputStream stream, JsonDocument header, int channel, Collection<INetworkChannel> networkChannels) throws IOException {
        boolean success = ChunkedPacketFactory.createChunkedPackets(stream, header, channel, packet -> {
            channelLoop:
            for (INetworkChannel networkChannel : networkChannels) {

                if (!networkChannel.isActive()) {
                    for (INetworkChannel otherChannel : networkChannels) {
                        if (otherChannel.isActive()) {
                            continue channelLoop;
                        }
                    }
                    throw ChunkInterrupt.INSTANCE;
                }

                while (!networkChannel.isWriteable()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException exception) {
                        throw ChunkInterrupt.INSTANCE;
                    }

                    if (!networkChannel.isActive()) {
                        continue channelLoop;
                    }
                }

                networkChannel.sendPacketSync(packet.fillBuffer());
            }

            packet.clearData();
        });

        System.gc();

        return success;
    }

}
