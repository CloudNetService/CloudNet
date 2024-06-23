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

package eu.cloudnetservice.driver.network.netty;

import eu.cloudnetservice.driver.DriverEnvironment;
import io.netty5.buffer.DefaultBufferAllocators;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class NettyUtilTest {
/*
  @Test
  void testWrapperThreadAmount() {
    Assertions.assertEquals(4, NettyUtil.threadAmount(DriverEnvironment.WRAPPER));
  }

  @Test
  void testNodeThreadAmount() {
    Assertions.assertTrue(NettyUtil.threadAmount(DriverEnvironment.NODE) >= 8);
  }
*/
  @RepeatedTest(30)
  public void testVarIntCoding() {
    try (var buffer = DefaultBufferAllocators.onHeapAllocator().allocate(0)) {
      var i = ThreadLocalRandom.current().nextInt();

      Assertions.assertNotNull(NettyUtil.writeVarInt(buffer, i));
      Assertions.assertEquals(i, NettyUtil.readVarInt(buffer));
    }
  }
}
