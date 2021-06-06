package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NettyPacketDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
    if (ctx != null && (!ctx.channel().isActive() || !byteBuf.isReadable())) {
      byteBuf.clear();
      return;
    }

    try {
      int channel = NettyUtils.readVarInt(byteBuf);
      UUID uniqueId = new UUID(byteBuf.readLong(), byteBuf.readLong());
      JsonDocument header = this.readHeader(byteBuf);
      ProtocolBuffer body = ProtocolBuffer.wrap(NettyUtils.readByteArray(byteBuf, NettyUtils.readVarInt(byteBuf)));

      Packet packet = new Packet(channel, uniqueId, header, body);
      out.add(packet);

      this.showDebug(packet);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  protected void showDebug(IPacket packet) {
    if (packet.isShowDebug()) {
      CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
        if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
          cloudNetDriver.getLogger().debug(
            String.format(
              "Successfully decoded packet on channel %d with id %s, header=%s;body=%d",
              packet.getChannel(),
              packet.getUniqueId(),
              packet.getHeader().toJson(),
              packet.getBuffer() != null ? packet.getBuffer().readableBytes() : 0
            )
          );
        }
      });
    }
  }

  protected JsonDocument readHeader(ByteBuf buf) {
    int length = NettyUtils.readVarInt(buf);
    if (length == 0) {
      return JsonDocument.EMPTY;
    } else {
      byte[] content = new byte[length];
      buf.readBytes(content);
      return JsonDocument.newDocument(new String(content, StandardCharsets.UTF_8));
    }
  }
}
