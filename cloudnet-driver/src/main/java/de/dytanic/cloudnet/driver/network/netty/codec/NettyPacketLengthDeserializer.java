/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public final class NettyPacketLengthDeserializer extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isActive()) {
            in.skipBytes(in.readableBytes());
            return;
        }

        in.markReaderIndex();
        byte[] lengthBytes = new byte[5];

        for (int i = 0; i < 5; i++) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }

            lengthBytes[i] = in.readByte();

            if (lengthBytes[i] >= 0) {
                ByteBuf buffer = Unpooled.wrappedBuffer(lengthBytes);

                try {
                    int packetLength = NettyUtils.readVarInt(buffer);

                    if (in.readableBytes() < packetLength) {
                        in.resetReaderIndex();
                        return;
                    }

                    out.add(in.readBytes(packetLength));
                } finally {
                    buffer.release();
                }

                return;
            }
        }
    }
}