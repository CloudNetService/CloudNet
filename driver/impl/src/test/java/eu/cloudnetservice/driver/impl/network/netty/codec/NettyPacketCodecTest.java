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

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import io.netty5.buffer.Buffer;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public class NettyPacketCodecTest {

  @Test
  void testNettyPacketCodec() {
    // dummy write
    var packetChannel = ThreadLocalRandom.current().nextInt();
    DataBuf dataBuf = DataBufFactory.defaultFactory().createEmpty()
      .writeBoolean(true)
      .writeInt(1234)
      .writeDouble(5D);

    var outCtx = Mockito.mock(ChannelHandlerContext.class);
    Mockito.when(outCtx.bufferAllocator()).thenReturn(NettyUtil.selectedBufferAllocator());
    Mockito.when(outCtx.write(Mockito.any(Buffer.class))).then(invocation -> {
      // called from within the encoder call
      Buffer buffer = invocation.getArgument(0);
      Assertions.assertTrue(buffer.readableBytes() > 0);

      // test deserialize
      var inChannel = Mockito.mock(Channel.class);
      Mockito.when(inChannel.isActive()).thenReturn(true);

      var inCtx = Mockito.mock(ChannelHandlerContext.class);
      Mockito.when(inCtx.channel()).thenReturn(inChannel);
      Mockito.when(inCtx.fireChannelRead(Mockito.any(Packet.class))).then(inv -> {
        Packet packet = inv.getArgument(0);
        // validate
        Assertions.assertEquals(packetChannel, packet.channel());
        Assertions.assertTrue(packet.content().readBoolean());
        Assertions.assertEquals(1234, packet.content().readInt());
        Assertions.assertEquals(5D, packet.content().readDouble());

        // whatever
        return null;
      });

      // decode the packet again
      var decoder = new NettyPacketDecoder();
      decoder.decode(inCtx, buffer);

      // whatever
      return null;
    });

    // encode the packet
    NettyPacketEncoder.INSTANCE.write(outCtx, new BasePacket(packetChannel, dataBuf));
  }
}
