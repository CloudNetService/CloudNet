/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.network.netty.NettyUtil;
import io.netty5.buffer.DefaultBufferAllocators;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VarIntCodecTest {

  @Test
  void testNettyUtilVarIntWriteRead() {
    try (var buffer = DefaultBufferAllocators.onHeapAllocator().allocate(5)) {
      for (var curr = 1; curr < 5_000_000; curr += 31) {
        // write an extra long to try trick the deserializer
        NettyUtil.writeVarInt(buffer, curr);
        NettyUtil.writeVarInt(buffer, ThreadLocalRandom.current().nextInt());

        // read
        Assertions.assertEquals(curr, NettyUtil.readVarInt(buffer));

        // reset
        buffer.resetOffsets().fill((byte) 0);
      }
    }
  }
}
