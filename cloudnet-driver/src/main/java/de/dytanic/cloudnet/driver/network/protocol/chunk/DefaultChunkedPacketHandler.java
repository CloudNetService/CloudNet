package de.dytanic.cloudnet.driver.network.protocol.chunk;

import de.dytanic.cloudnet.driver.network.INetworkChannel;

import java.util.Collection;
import java.util.function.Consumer;

public class DefaultChunkedPacketHandler {

    public static Consumer<ChunkedPacket> createHandler(Collection<INetworkChannel> channels) {
        return packet -> {
            for (INetworkChannel channel : channels) {
                if (!channel.isActive()) {
                    if (noneActive(channels)) {
                        throw ChunkInterrupt.INSTANCE;
                    }
                    continue;
                }

                if (!waitWritable(channel)) {
                    continue;
                }

                channel.sendPacketSync(packet.fillBuffer());
            }

            packet.clearData();
        };
    }

    private static boolean noneActive(Collection<INetworkChannel> channels) {
        for (INetworkChannel channel : channels) {
            if (channel.isActive()) {
                return false;
            }
        }
        return true;
    }

    private static boolean waitWritable(INetworkChannel channel) {
        while (!channel.isWriteable()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException exception) {
                throw ChunkInterrupt.INSTANCE;
            }

            if (!channel.isActive()) {
                return false;
            }
        }

        return true;
    }

}
