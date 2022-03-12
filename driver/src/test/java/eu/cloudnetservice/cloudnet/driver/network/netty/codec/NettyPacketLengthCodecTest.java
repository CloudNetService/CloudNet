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
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.Mockito;

public class NettyPacketLengthCodecTest {

  @RepeatedTest(30)
  void testPacketLengthCodec() {
    // dummy buffer
    var input = NettyUtil.allocator().allocate((Long.BYTES * 2) + Double.BYTES)
      .writeLong(12345678)
      .writeLong(87654321)
      .writeDouble(5D);

    var outCtx = Mockito.mock(ChannelHandlerContext.class);
    Mockito.when(outCtx.bufferAllocator()).thenReturn(NettyUtil.allocator());
    Mockito.when(outCtx.write(Mockito.any(Buffer.class))).then(invocation -> {
      Buffer buffer = invocation.getArgument(0);
      Assertions.assertTrue(buffer.readableBytes() > 0);

      // test deserialize
      var channel = Mockito.mock(Channel.class);
      Mockito.when(channel.isActive()).thenReturn(true);

      var inCtx = Mockito.mock(ChannelHandlerContext.class);
      Mockito.when(inCtx.channel()).thenReturn(channel);
      Mockito.when(inCtx.fireChannelRead(Mockito.any(Buffer.class))).then(inv -> {
        Buffer buf = inv.getArgument(0);
        Assertions.assertEquals(12345678, buf.readLong());
        Assertions.assertEquals(87654321, buf.readLong());
        Assertions.assertEquals(5D, buf.readDouble());

        // whatever
        return null;
      });

      // deserialize again
      var deserializer = new NettyPacketLengthDeserializer();
      deserializer.decode(inCtx, buffer);

      // whatever
      return null;
    });

    // encode the buffer
    NettyPacketLengthSerializer.INSTANCE.write(outCtx, input);
  }
}
