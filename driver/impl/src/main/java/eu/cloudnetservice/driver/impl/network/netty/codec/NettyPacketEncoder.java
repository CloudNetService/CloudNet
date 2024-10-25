/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.netty.codec;

import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.impl.network.netty.buffer.NettyImmutableDataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.MessageToByteEncoder;
import lombok.NonNull;

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
public final class NettyPacketEncoder extends MessageToByteEncoder<Packet> {

  public static final NettyPacketEncoder INSTANCE = new NettyPacketEncoder();

  /**
   * {@inheritDoc}
   */
  @Override
  protected Buffer allocateBuffer(@NonNull ChannelHandlerContext ctx, @NonNull Packet msg) {
    // we allocate 2 booleans (prioritized and isQuery) + content length + channel in advance
    var bufferLength = 2
      + msg.content().readableBytes()
      + NettyUtil.varIntBytes(msg.channel())
      + NettyUtil.varIntBytes(msg.content().readableBytes());
    // if the given packet has a query unique id we need two longs for that unique id as well
    if (msg.uniqueId() != null) {
      bufferLength += 16;
    }

    return ctx.bufferAllocator().allocate(bufferLength);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void encode(@NonNull ChannelHandlerContext ctx, @NonNull Packet msg, @NonNull Buffer out) {
    NettyUtil.writeVarInt(out, msg.channel());
    out.writeBoolean(msg.prioritized());

    var queryUniqueId = msg.uniqueId();
    out.writeBoolean(queryUniqueId != null);
    if (queryUniqueId != null) {
      out
        .writeLong(queryUniqueId.getMostSignificantBits())
        .writeLong(queryUniqueId.getLeastSignificantBits());
    }

    // copy over the packet body into the output buffer
    var content = ((NettyImmutableDataBuf) msg.content()).buffer();
    var length = content.readableBytes();
    NettyUtil.writeVarInt(out, length);
    content.copyInto(0, out, out.writerOffset(), length);
    out.skipWritableBytes(length);

    // release the packet content once
    msg.content().release();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharable() {
    return true;
  }
}
