package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.UUID;

final class NettyPacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out)
    {
        if (byteBuf.readableBytes() == 0) return;

        try
        {
            out.add(new Packet(
                NettyUtils.readVarInt(byteBuf),
                UUID.fromString(NettyUtils.readString(byteBuf)),
                JsonDocument.newDocument(NettyUtils.readString(byteBuf)),
                NettyUtils.toByteArray(byteBuf, NettyUtils.readVarInt(byteBuf))
            ));
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}