package de.dytanic.cloudnet.driver.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class NettyPacketLengthSerializer extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        int readableBytes = in.readableBytes(), lengthByteSpace = this.getVarIntSize(readableBytes);

        if (lengthByteSpace > 5) {
            throw new IllegalArgumentException();
        }

        out.ensureWritable(lengthByteSpace + readableBytes);
        NettyUtils.writeVarInt(out, readableBytes);
        out.writeBytes(in, in.readerIndex(), readableBytes);
    }

    private int getVarIntSize(int value) {
        if ((value & -128) == 0) {
            return 1;
        } else if ((value & -16384) == 0) {
            return 2;
        } else if ((value & -2097152) == 0) {
            return 3;
        } else if ((value & -268435456) == 0) {
            return 4;
        }

        return 5;
    }

}
