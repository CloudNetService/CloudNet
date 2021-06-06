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

package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import org.junit.Assert;
import org.junit.Test;

public class DefaultPacketRegistryTest {

  private String testValue = null;

  private int property = 0;

  @Test
  public void testPacketRegistry() throws Throwable {
    final int channelId = 4;

    IPacketListener listener = new PacketListenerImpl();

    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();
    registry.addListener(channelId, listener);

    Assert.assertEquals(1, registry.getListeners().size());

    registry.handlePacket(null, new Packet(channelId, new JsonDocument("testProperty", 65), "TestValue".getBytes()));

    Assert.assertEquals(65, this.property);
    Assert.assertEquals("TestValue", this.testValue);

    registry.removeListeners(channelId);

    Assert.assertEquals(0, registry.getListeners().size());

    registry.addListener(channelId, listener);
    registry.removeListener(channelId, listener);

    Assert.assertEquals(0, registry.getListeners().size());
  }

  private final class PacketListenerImpl implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
      DefaultPacketRegistryTest.this.testValue = new String(packet.getBodyAsArray());
      DefaultPacketRegistryTest.this.property = packet.getHeader().getInt("testProperty");
    }
  }
}
