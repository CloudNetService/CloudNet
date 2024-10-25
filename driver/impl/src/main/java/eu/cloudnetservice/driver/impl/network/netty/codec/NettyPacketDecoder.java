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
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import java.util.UUID;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internal implementation of the packet decoder used for client to server communication. This decoder reverses the
 * encoding steps done in {@link NettyPacketEncoder} while ensuring that the channel is open and data was transferred to
 * the component before starting the decoding process.
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
public final class NettyPacketDecoder extends ByteToMessageDecoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyPacketDecoder.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull Buffer in) {
    // validates that the channel associated to this decoder call is still active and actually
    // transferred data before beginning to read.
    if (!ctx.channel().isActive() || in.readableBytes() <= 0) {
      return;
    }

    try {
      // read the required base data from the buffer
      var channel = NettyUtil.readVarInt(in);
      var prioritized = in.readBoolean();
      var queryUniqueId = in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null;

      // extract the body
      var bodyLength = NettyUtil.readVarInt(in);
      var body = new NettyImmutableDataBuf(in.copy(in.readerOffset(), bodyLength));
      in.skipReadableBytes(bodyLength);

      // construct the packet
      var packet = new BasePacket(channel, prioritized, body);
      packet.uniqueId(queryUniqueId);

      // register the packet for further downstream handling
      ctx.fireChannelRead(packet);
    } catch (Exception exception) {
      LOGGER.error("Exception while decoding packet", exception);
    }
  }
}
