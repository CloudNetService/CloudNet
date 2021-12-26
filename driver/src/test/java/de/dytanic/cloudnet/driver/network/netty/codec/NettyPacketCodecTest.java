/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.protocol.BasePacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NettyPacketCodecTest {

  @Test
  void testNettyPacketCodec() {
    // dummy write
    var packetChannel = ThreadLocalRandom.current().nextInt();
    DataBuf dataBuf = DataBufFactory.defaultFactory().createEmpty()
      .writeBoolean(true)
      .writeInt(1234)
      .writeDouble(5D);

    var output = Unpooled.buffer();

    var encoder = new NettyPacketEncoder();
    encoder.encode(Mockito.mock(ChannelHandlerContext.class), new BasePacket(packetChannel, dataBuf), output);

    Assertions.assertTrue(output.readableBytes() > 0);

    // test read
    var channel = Mockito.mock(Channel.class);
    Mockito.when(channel.isActive()).thenReturn(true);

    var ctx = Mockito.mock(ChannelHandlerContext.class);
    Mockito.when(ctx.channel()).thenReturn(channel);

    List<Object> results = new ArrayList<>();
    var decoder = new NettyPacketDecoder();
    decoder.decode(ctx, output, results);

    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(packetChannel, ((BasePacket) results.get(0)).channel());
    Assertions.assertTrue(((BasePacket) results.get(0)).content().readBoolean());
    Assertions.assertEquals(1234, ((BasePacket) results.get(0)).content().readInt());
    Assertions.assertEquals(5D, ((BasePacket) results.get(0)).content().readDouble());

    output.release();
  }
}
