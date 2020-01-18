package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
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

        try {
            Packet packet = new Packet(
                    NettyUtils.readVarInt(byteBuf),
                    UUID.fromString(NettyUtils.readString(byteBuf)),
                    JsonDocument.newDocument(NettyUtils.readString(byteBuf)),
                    NettyUtils.toByteArray(byteBuf, NettyUtils.readVarInt(byteBuf))
            );
            out.add(packet);

            if (packet.isShowDebug()) {
                CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> cloudNetDriver.getLogger().debug(
                        String.format(
                                "Successfully decoded packet on channel %d with id %s, header=%s;body=%d",
                                packet.getChannel(),
                                packet.getUniqueId().toString(),
                                packet.getHeader().toJson(),
                                packet.getBody() != null ? packet.getBody().length : 0
                        )
                ));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}