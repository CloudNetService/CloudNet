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

import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * An encoder for the length of the packet sent into a netty channel.
 *
 * @since 4.0
 */
@Internal
public final class NettyPacketLengthSerializer extends MessageToByteEncoder<ByteBuf> {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void encode(@NonNull ChannelHandlerContext ctx, @NonNull ByteBuf in, @NonNull ByteBuf out) {
    // write the var int before other content into the buffer, there is no need to expand the buffer as the buffer
    // is always large enough due to the overridden allocateBuffer method.
    NettyUtils.writeVarInt(out, in.readableBytes());
    out.writeBytes(in);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ByteBuf allocateBuffer(@NonNull ChannelHandlerContext ctx, @NonNull ByteBuf msg, boolean preferDirect) {
    // only pre-allocate exactly the amount of bytes we're needing to write the message prefixed by the length of it.
    var initialSize = NettyUtils.varIntByteAmount(msg.readableBytes()) + msg.readableBytes();
    return preferDirect
      ? ctx.alloc().heapBuffer(initialSize)
      : ctx.alloc().directBuffer(initialSize);
  }
}
