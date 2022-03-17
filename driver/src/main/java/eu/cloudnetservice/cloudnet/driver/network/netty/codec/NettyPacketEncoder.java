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
import io.netty5.channel.ChannelHandlerAdapter;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.concurrent.Future;
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
public final class NettyPacketEncoder extends ChannelHandlerAdapter {

  public static final NettyPacketEncoder INSTANCE = new NettyPacketEncoder();

  /**
   * Constructs a new netty packet encoder instance.
   */
  private NettyPacketEncoder() {
    // singleton
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> write(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof Packet packet) {
      var buf = ctx.bufferAllocator().allocate(0);
      // general info
      NettyUtil.writeVarInt(buf, packet.channel());
      // query id (if present)
      var queryUniqueId = packet.uniqueId();
      buf.ensureWritable(Byte.BYTES * 2)
        .writeBoolean(packet.prioritized())
        .writeBoolean(queryUniqueId != null);
      if (queryUniqueId != null) {
        buf
          .ensureWritable(Long.BYTES * 2)
          .writeLong(queryUniqueId.getMostSignificantBits())
          .writeLong(queryUniqueId.getLeastSignificantBits());
      }

      // we only support netty buf
      var content = ((NettyImmutableDataBuf) packet.content()).buffer();
      // write information to buffer
      var length = content.readableBytes();
      NettyUtil.writeVarInt(buf, length);
      // copy the packet content into the outbound buffer and move the writer index to the last written byte
      content.copyInto(0, buf.ensureWritable(length), buf.writerOffset(), length);
      buf.skipWritable(length);
      // release the content of the packet now, don't use the local field to respect if releasing was disabled in the
      // original buffer.
      packet.content().release();

      // write the serialized packet to the pipeline
      return ctx.write(buf);
    } else {
      // this should not happen, but whatever
      return ctx.write(msg);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharable() {
    return true;
  }
}
