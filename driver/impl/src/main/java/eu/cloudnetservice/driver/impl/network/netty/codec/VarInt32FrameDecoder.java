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
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.NonNull;

public final class VarInt32FrameDecoder extends ByteToMessageDecoder {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull Buffer in) {
    // ensure that the channel we're reading from is still open
    if (!ctx.channel().isActive()) {
      return;
    }

    // try to read the full message length from the buffer, reset the buffer if we've read nothing
    var readerIndex = in.readerOffset();
    var length = NettyUtil.readVarIntOrNull(in);
    if (length == null || readerIndex == in.readerOffset()) {
      in.readerOffset(readerIndex);
      return;
    }

    // skip empty packets silently
    if (length <= 0) {
      // check if there are bytes to skip
      if (in.readableBytes() > 0) {
        in.skipReadableBytes(in.readableBytes());
      }
      return;
    }

    // check if the packet data supplied in the buffer is actually at least the transmitted size
    if (in.readableBytes() >= length) {
      // fire the channel read
      ctx.fireChannelRead(in.copy(in.readerOffset(), length));
      in.skipReadableBytes(length);
    } else {
      // reset the reader index, there is still data missing
      in.readerOffset(readerIndex);
    }
  }
}
