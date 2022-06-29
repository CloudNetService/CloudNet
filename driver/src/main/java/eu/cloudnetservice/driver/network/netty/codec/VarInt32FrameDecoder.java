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

package eu.cloudnetservice.driver.network.netty.codec;

import eu.cloudnetservice.driver.network.exception.SilentDecoderException;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import io.netty5.buffer.api.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class VarInt32FrameDecoder extends ByteToMessageDecoder {

  private static final SilentDecoderException BAD_LENGTH = new SilentDecoderException("Bad packet length");

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull Buffer in) {
    // ensure that the channel we're reading from is still open
    if (!ctx.channel().isActive()) {
      in.close();
      return;
    }

    var readerIndex = in.readerOffset();
    var length = NettyUtil.readVarInt(in);

    // check if we've read any bytes
    if (readerIndex == in.readerOffset()) {
      return;
    }

    // check if the packet length is out of bounds
    if (length < 0) {
      throw BAD_LENGTH;
    }

    // skip empty packets silently
    if (length == 0) {
      in.skipReadableBytes(1);
      return;
    }

    // check if the packet data supplied in the buffer is actually at least the transmitted size
    if (in.readableBytes() >= length) {
      // fire the channel read
      ctx.fireChannelRead(in.copy(in.readerOffset(), length, true));
      in.skipReadableBytes(length);
    } else {
      // reset the reader index, there is still data missing
      in.readerOffset(readerIndex);
    }
  }
}
