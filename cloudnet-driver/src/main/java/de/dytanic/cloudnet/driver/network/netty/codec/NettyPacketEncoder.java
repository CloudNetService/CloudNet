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

import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NettyPacketEncoder extends MessageToByteEncoder<IPacket> {

  @Override
  protected void encode(ChannelHandlerContext ctx, IPacket packet, ByteBuf byteBuf) {
    // channel
    NettyUtils.writeVarInt(byteBuf, packet.getChannel());
    // query id (if present)
    UUID queryUniqueId = packet.getUniqueId();
    byteBuf.writeBoolean(queryUniqueId != null);
    if (queryUniqueId != null) {
      byteBuf
        .writeLong(queryUniqueId.getMostSignificantBits())
        .writeLong(queryUniqueId.getLeastSignificantBits());
    }
    // body
    // we only support netty buf
    ByteBuf buf = ((NettyImmutableDataBuf) packet.getContent()).getByteBuf();
    // write information to buffer
    int length = buf.readableBytes();
    NettyUtils.writeVarInt(byteBuf, length);
    byteBuf.writeBytes(buf, 0, length);
    // release the content of the packet now
    packet.getContent().release();
  }
}
