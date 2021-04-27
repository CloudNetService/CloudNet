package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.UUID;

final class NettyPacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        if (byteBuf.readableBytes() == 0) {
            return;
        }

        // This weired null check is needed for the 'NettyPacketEncoderDecoderTest' which uses 'null'
        // as 'ChannelHandlerContext' for testing reasons
        if (ctx != null && !ctx.channel().isActive()) {
            byteBuf.skipBytes(byteBuf.readableBytes());
            return;
        }

        try {
            ProtocolBuffer in = ProtocolBuffer.wrap(byteBuf);

            int channel = in.readVarInt();
            UUID uniqueId = in.readUUID();
            JsonDocument header = JsonDocument.newDocument(in.readString());
            ProtocolBuffer body = ProtocolBuffer.wrap(in.readArray());
            body.resetReaderIndex();

            Packet packet = new Packet(channel, uniqueId, header, body);
            out.add(packet);

            if (packet.isShowDebug()) {
                CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                    if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                        cloudNetDriver.getLogger().debug(
                                String.format(
                                        "Successfully decoded packet on channel %d with id %s, header=%s;body=%d",
                                        packet.getChannel(),
                                        packet.getUniqueId().toString(),
                                        packet.getHeader().toJson(),
                                        packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
                                )
                        );
                    }
                });
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}