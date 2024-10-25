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
import io.netty5.channel.ChannelHandlerAdapter;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.concurrent.Future;
import io.netty5.util.concurrent.Promise;
import io.netty5.util.concurrent.PromiseCombiner;
import lombok.NonNull;

public final class VarInt32FramePrepender extends ChannelHandlerAdapter {

  public static final VarInt32FramePrepender INSTANCE = new VarInt32FramePrepender();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Future<Void> write(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) {
    if (msg instanceof Buffer packetDataBuffer) {
      // first write the buffer that contains the length of the following buffer
      var length = packetDataBuffer.readableBytes();
      var encodedLengthFieldLength = NettyUtil.varIntBytes(length);
      var lengthBuffer = ctx.bufferAllocator().allocate(encodedLengthFieldLength);
      NettyUtil.writeVarInt(lengthBuffer, length);

      // write both the length buffer and the message buffer into the channel
      var combiner = new PromiseCombiner(ctx.executor());
      combiner.add(ctx.write(lengthBuffer));
      combiner.add(ctx.write(msg));

      // finish the combining and merge all steps into one future
      Promise<Void> promise = ctx.newPromise();
      combiner.finish(promise);
      return promise.asFuture();
    } else {
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
