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
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtils;
import eu.cloudnetservice.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.BasePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

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
@Internal
public final class NettyPacketDecoder extends ByteToMessageDecoder {

  private static final Logger LOGGER = LogManager.logger(NettyPacketDecoder.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull ByteBuf buf, @NonNull List<Object> out) {
    // validates that the channel associated to this decoder call is still active and actually transferred data before
    // beginning to read.
    if (!ctx.channel().isActive() || !buf.isReadable()) {
      buf.clear();
      return;
    }

    try {
      // read the required base data from the buffer
      var channel = NettyUtils.readVarInt(buf);
      var queryUniqueId = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
      var body = new NettyImmutableDataBuf(buf.readBytes(NettyUtils.readVarInt(buf)));

      // construct the packet
      var packet = new BasePacket(channel, body);
      packet.uniqueId(queryUniqueId);

      // register the packet for further downstream handling
      out.add(packet);
    } catch (Exception exception) {
      LOGGER.severe("Exception while decoding packet", exception);
    }
  }
}
