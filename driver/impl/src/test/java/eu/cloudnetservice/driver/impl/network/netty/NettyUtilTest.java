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

package eu.cloudnetservice.driver.impl.network.netty;

import eu.cloudnetservice.driver.DriverEnvironment;
import io.netty5.buffer.BufferAllocator;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.handler.ssl.OpenSsl;
import io.netty5.handler.ssl.SslProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NettyUtilTest {

  @Test
  void testBossEventLoopGroupCreation() {
    var bossEventLoopGroup = NettyUtil.createBossEventLoopGroup();
    var threadedGroup = Assertions.assertInstanceOf(MultithreadEventLoopGroup.class, bossEventLoopGroup);
    Assertions.assertEquals(1, threadedGroup.executorCount());
  }

  @Test
  void testWorkerEventLoopGroupCreation() {
    {
      // test node event loop group
      var workerEventLoopGroup = NettyUtil.createWorkerEventLoopGroup(DriverEnvironment.NODE);
      var threadedGroup = Assertions.assertInstanceOf(MultithreadEventLoopGroup.class, workerEventLoopGroup);
      Assertions.assertEquals(6, threadedGroup.executorCount());
    }

    {
      // test wrapper event loop group
      var workerEventLoopGroup = NettyUtil.createWorkerEventLoopGroup(DriverEnvironment.WRAPPER);
      var threadedGroup = Assertions.assertInstanceOf(MultithreadEventLoopGroup.class, workerEventLoopGroup);
      Assertions.assertEquals(2, threadedGroup.executorCount());
    }
  }

  @Test
  void testSslProviderSelection() {
    var selectedSslProvider = NettyUtil.selectedSslProvider();
    if (OpenSsl.isAvailable()) {
      Assertions.assertEquals(SslProvider.OPENSSL, selectedSslProvider);
    } else {
      Assertions.assertEquals(SslProvider.JDK, selectedSslProvider);
    }
  }

  @Test
  void testVarIntBytes() {
    Assertions.assertEquals(1, NettyUtil.varIntBytes(0));
    Assertions.assertEquals(1, NettyUtil.varIntBytes(1));
    Assertions.assertEquals(5, NettyUtil.varIntBytes(-1));
    Assertions.assertEquals(5, NettyUtil.varIntBytes(-2048));
    Assertions.assertEquals(5, NettyUtil.varIntBytes(Integer.MIN_VALUE));
  }

  @Test
  void testVarIntBytesAndCodec() {
    try (var buffer = BufferAllocator.onHeapUnpooled().allocate(5)) {
      for (var num = Integer.MIN_VALUE; num < Integer.MAX_VALUE; num += Byte.MAX_VALUE) {
        // write var int, validate written bytes were as expected, read var int
        NettyUtil.writeVarInt(buffer, num);
        Assertions.assertEquals(buffer.writerOffset(), NettyUtil.varIntBytes(num));
        Assertions.assertEquals(num, NettyUtil.readVarInt(buffer));

        // reset the buffer indexes and fill the buffer with 0 for the next run
        buffer.resetOffsets().fill((byte) 0);
      }
    }
  }
}
