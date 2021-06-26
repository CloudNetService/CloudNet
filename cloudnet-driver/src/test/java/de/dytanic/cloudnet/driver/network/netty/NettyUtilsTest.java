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

import de.dytanic.cloudnet.common.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Assert;
import org.junit.Test;

public class NettyUtilsTest {

  @Test
  public void testNettyUtils() {
    ByteBuf byteBuf = Unpooled.buffer();

    int randomInt = ThreadLocalRandom.current().nextInt();
    long randomLong = ThreadLocalRandom.current().nextLong();
    String randomString = StringUtil.generateRandomString(10);

    NettyUtils.writeVarInt(byteBuf, randomInt);
    NettyUtils.writeVarLong(byteBuf, randomLong);
    NettyUtils.writeString(byteBuf, randomString);

    Assert.assertEquals(randomInt, NettyUtils.readVarInt(byteBuf));
    Assert.assertEquals(randomLong, NettyUtils.readVarLong(byteBuf));
    Assert.assertEquals(randomString, NettyUtils.readString(byteBuf));
  }
}
