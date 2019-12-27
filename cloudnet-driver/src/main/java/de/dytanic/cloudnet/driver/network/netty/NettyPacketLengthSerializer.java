/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.driver.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class NettyPacketLengthSerializer extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        int readableBytes = in.readableBytes();
        int lengthByteSpace = getVarIntSize(readableBytes);

        if (lengthByteSpace > 3) {
            throw new RuntimeException("Unable to fit " + readableBytes + " into " + 3);
        }

        out.ensureWritable(lengthByteSpace + readableBytes);
        NettyUtils.writeVarInt(out, readableBytes);
        out.writeBytes(in, in.readerIndex(), readableBytes);
    }

    private int getVarIntSize(int value) {
        for (int i = 1; i < 5; ++i) {
            if ((value & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

}
