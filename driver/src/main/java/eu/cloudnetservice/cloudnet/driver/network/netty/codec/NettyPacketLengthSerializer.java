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
import io.netty5.buffer.api.Buffer;
import io.netty5.channel.ChannelHandlerAdapter;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.concurrent.Future;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * An encoder for the length of the packet sent into a netty channel.
 *
 * @since 4.0
 */
@Internal
public final class NettyPacketLengthSerializer extends ChannelHandlerAdapter {

  public static final NettyPacketLengthSerializer INSTANCE = new NettyPacketLengthSerializer();

  /**
   * Constructs a new netty packet length serializer instance.
   */
  private NettyPacketLengthSerializer() {
    // singleton
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> write(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof Buffer buffer) {
      try (buffer) {
        // the buffer we're actually going to send out
        var initialSize = NettyUtil.varIntByteAmount(buffer.readableBytes()) + buffer.readableBytes();
        var out = ctx.bufferAllocator().allocate(initialSize);

        // put in the length information
        NettyUtil.writeVarInt(out, buffer.readableBytes());
        out.writeBytes(buffer);

        // send out the buffer
        return ctx.write(out);
      }
    } else {
      // this should not happen...
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
