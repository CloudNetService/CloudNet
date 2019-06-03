package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.UUID;

final class NettyPacketEncoder extends MessageToByteEncoder<IPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IPacket packet, ByteBuf byteBuf) {
        //Writing the channelId
        NettyUtils.writeVarInt(byteBuf, packet.getChannel());

        //Writing the uniqueId
        NettyUtils.writeString(byteBuf, packet.getUniqueId() != null ? packet.getUniqueId().toString() : UUID.randomUUID().toString());

        byte[] data;

        //Writing the header
        if (packet.getHeader() != null) {
            data = packet.getHeader().toByteArray();
            NettyUtils.writeVarInt(byteBuf, data.length);
            byteBuf.writeBytes(data);
        } else {
            NettyUtils.writeString(byteBuf, "{}");
        }

        //Writing the body
        data = packet.getBody();

        if (data == null || data.length == 0) data = Packet.EMPTY_PACKET_BYTE_ARRAY;

        NettyUtils.writeVarInt(byteBuf, data.length).writeBytes(data);
    }
}