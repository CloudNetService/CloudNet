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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.DriverTestUtility;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NettyUtilsTest {

  @Test
  void testWrapperThreadAmount() {
    Mockito
      .when(DriverTestUtility.mockAndSetDriverInstance().getDriverEnvironment())
      .thenReturn(DriverEnvironment.WRAPPER);

    Assertions.assertEquals(4, NettyUtils.getThreadAmount());
  }

  @Test
  void testNodeThreadAmount() {
    Mockito
      .when(DriverTestUtility.mockAndSetDriverInstance().getDriverEnvironment())
      .thenReturn(DriverEnvironment.CLOUDNET);

    Assertions.assertEquals(Runtime.getRuntime().availableProcessors() * 2, NettyUtils.getThreadAmount());
  }

  @RepeatedTest(30)
  public void testVarIntCoding() {
    ByteBuf byteBuf = Unpooled.buffer();
    int i = ThreadLocalRandom.current().nextInt();

    try {
      Assertions.assertNotNull(NettyUtils.writeVarInt(byteBuf, i));
      Assertions.assertEquals(i, NettyUtils.readVarInt(byteBuf));
    } finally {
      byteBuf.release();
    }
  }
}
