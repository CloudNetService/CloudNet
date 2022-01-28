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

import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtil;
import eu.cloudnetservice.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * An internal implementation of the packet encoder used for client to server communication.
 * <p>
 * A packet always contains the following data:
 * <ol>
 *   <li>The numeric id of the channel being sent to, by default a var int.
 *   <li>An optional query unique id if the packet is a query.
 *   <li>The data transferred to this component, might be empty.
 * </ol>
 *
 * @since 4.0
 */
@Internal
public final class NettyPacketEncoder extends MessageToByteEncoder<Packet> {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void encode(@NonNull ChannelHandlerContext ctx, @NonNull Packet packet, @NonNull ByteBuf buf) {
    // channel
    NettyUtil.writeVarInt(buf, packet.channel());
    // packet priority
    buf.writeBoolean(packet.prioritized());
    // query id (if present)
    var queryUniqueId = packet.uniqueId();
    buf.writeBoolean(queryUniqueId != null);
    if (queryUniqueId != null) {
      buf
        .writeLong(queryUniqueId.getMostSignificantBits())
        .writeLong(queryUniqueId.getLeastSignificantBits());
    }
    // body
    // we only support netty buf
    var content = ((NettyImmutableDataBuf) packet.content()).byteBuf();
    // write information to buffer
    var length = content.readableBytes();
    NettyUtil.writeVarInt(buf, length);
    buf.writeBytes(content, 0, length);
    // release the content of the packet now, don't use the local field to respect if releasing was disabled in the
    // original buffer.
    packet.content().release();
  }
}
