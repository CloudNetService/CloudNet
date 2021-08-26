/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NettyPacketDecoder extends ByteToMessageDecoder {

  private static final Logger LOGGER = LogManager.getLogger(NettyPacketDecoder.class);

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
    if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
      byteBuf.clear();
      return;
    }

    try {
      int channel = NettyUtils.readVarInt(byteBuf);
      UUID queryUniqueId = byteBuf.readBoolean() ? new UUID(byteBuf.readLong(), byteBuf.readLong()) : null;
      DataBuf body = new NettyImmutableDataBuf(byteBuf.readBytes(NettyUtils.readVarInt(byteBuf)));

      Packet packet = new Packet(channel, body);
      packet.setUniqueId(queryUniqueId);

      out.add(packet);
    } catch (Exception exception) {
      LOGGER.severe("Exception while decoding packet", exception);
    }
  }
}
