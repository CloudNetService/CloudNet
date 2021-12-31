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

package de.dytanic.cloudnet.driver.network.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.Mockito;

public class NettyPacketLengthCodecTest {

  @RepeatedTest(30)
  void testPacketLengthCodec() {
    // dummy write
    var input = Unpooled.buffer()
      .writeBoolean(true)
      .writeInt(1234)
      .writeDouble(5D);
    var output = Unpooled.buffer();

    var serializer = new NettyPacketLengthSerializer();
    serializer.encode(Mockito.mock(ChannelHandlerContext.class), input, output);

    Assertions.assertTrue(output.readableBytes() > 0);

    // test read
    var channel = Mockito.mock(Channel.class);
    Mockito.when(channel.isActive()).thenReturn(true);

    var ctx = Mockito.mock(ChannelHandlerContext.class);
    Mockito.when(ctx.channel()).thenReturn(channel);

    List<Object> results = new ArrayList<>();
    var deserializer = new NettyPacketLengthDeserializer();
    deserializer.decode(ctx, output, results);

    Assertions.assertEquals(1, results.size());
    Assertions.assertTrue(((ByteBuf) results.get(0)).readBoolean());
    Assertions.assertEquals(1234, ((ByteBuf) results.get(0)).readInt());
    Assertions.assertEquals(5D, ((ByteBuf) results.get(0)).readDouble());

    input.release();
    output.release();
  }
}
