package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NettyPacketEncoder extends MessageToByteEncoder<IPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IPacket packet, ByteBuf byteBuf) {
        if (packet.isShowDebug()) {
            CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                    cloudNetDriver.getLogger().debug(
                            String.format(
                                    "Encoding packet on channel %d with id %s, header=%s;body=%d",
                                    packet.getChannel(),
                                    packet.getUniqueId(),
                                    packet.getHeader().toJson(),
                                    packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
                            )
                    );
                }
            });
        }

        // channel
        NettyUtils.writeVarInt(byteBuf, packet.getChannel());
        // unique id
        byteBuf
                .writeLong(packet.getUniqueId().getMostSignificantBits())
                .writeLong(packet.getUniqueId().getLeastSignificantBits());
        // header
        this.writeHeader(packet, byteBuf);
        // body
        if (packet.getBuffer() != null) {
            int amount = packet.getBuffer().readableBytes();
            NettyUtils.writeVarInt(byteBuf, amount);
            byteBuf.writeBytes(packet.getBuffer(), 0, amount);
        } else {
            NettyUtils.writeVarInt(byteBuf, 0);
        }
    }

    private void writeHeader(IPacket packet, ByteBuf byteBuf) {
        if (packet.getHeader() == null || packet.getHeader().isEmpty()) {
            NettyUtils.writeVarInt(byteBuf, 0);
        } else {
            NettyUtils.writeString(byteBuf, packet.getHeader().toJson());
        }
    }
}