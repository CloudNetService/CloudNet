package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.UUID;

final class NettyPacketEncoder extends MessageToByteEncoder<IPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IPacket packet, ByteBuf byteBuf) {
        if (packet.isShowDebug()) {
            CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                    cloudNetDriver.getLogger().debug(
                            String.format(
                                    "Encoding packet on channel %d with id %s, header=%s;body=%d",
                                    packet.getChannel(),
                                    packet.getUniqueId().toString(),
                                    packet.getHeader().toJson(),
                                    packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
                            )
                    );
                }
            });
        }

        ProtocolBuffer data = ProtocolBuffer.wrap(byteBuf);

        data.writeVarInt(packet.getChannel());
        data.writeUUID(packet.getUniqueId());

        data.writeString(packet.getHeader() != null ? packet.getHeader().toJson() : "{}");

        data.writeArray(packet.getBodyAsArray());
    }
}