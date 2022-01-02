/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.netty.codec;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtils;
import eu.cloudnetservice.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.BasePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class NettyPacketDecoder extends ByteToMessageDecoder {

  private static final Logger LOGGER = LogManager.logger(NettyPacketDecoder.class);

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
    if (!ctx.channel().isActive() || !byteBuf.isReadable()) {
      byteBuf.clear();
      return;
    }

    try {
      var channel = NettyUtils.readVarInt(byteBuf);
      var queryUniqueId = byteBuf.readBoolean() ? new UUID(byteBuf.readLong(), byteBuf.readLong()) : null;
      DataBuf body = new NettyImmutableDataBuf(byteBuf.readBytes(NettyUtils.readVarInt(byteBuf)));

      var packet = new BasePacket(channel, body);
      packet.uniqueId(queryUniqueId);

      out.add(packet);
    } catch (Exception exception) {
      LOGGER.severe("Exception while decoding packet", exception);
    }
  }
}
