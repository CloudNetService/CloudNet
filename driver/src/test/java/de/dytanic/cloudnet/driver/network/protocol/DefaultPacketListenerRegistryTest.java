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

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
public class DefaultPacketListenerRegistryTest {

  @Test
  @Order(0)
  void testListenerRegister() {
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    registry.addListener(
      123,
      (channel, packet) -> {
      },
      (channel, packet) -> {
      }
    );
    registry.addListener(456, (channel, packet) -> {
    });

    Assertions.assertTrue(registry.hasListeners(123));
    Assertions.assertTrue(registry.hasListeners(456));

    Assertions.assertEquals(2, registry.getPacketListeners().get(123).size());
    Assertions.assertEquals(1, registry.getPacketListeners().get(456).size());

    Assertions.assertEquals(3, registry.getListeners().size());
    Assertions.assertEquals(2, registry.getChannels().size());
  }

  @Test
  @Order(10)
  void testListenerUnregister() {
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    IPacketListener firstListener = Mockito.mock(IPacketListener.class);
    IPacketListener secondListener = Mockito.mock(IPacketListener.class);

    registry.addListener(123, firstListener, secondListener);
    Assertions.assertEquals(2, registry.getPacketListeners().get(123).size());

    registry.removeListener(123, firstListener);
    Assertions.assertEquals(1, registry.getPacketListeners().get(123).size());

    registry.removeListener(123, secondListener);
    Assertions.assertNull(registry.getPacketListeners().get(123));

    Assertions.assertFalse(registry.hasListeners(123));
  }

  @Test
  @Order(20)
  void testListenerUnregisterByChannel() {
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    IPacketListener firstListener = Mockito.mock(IPacketListener.class);
    IPacketListener secondListener = Mockito.mock(IPacketListener.class);

    registry.addListener(123, firstListener, secondListener);
    Assertions.assertEquals(2, registry.getPacketListeners().get(123).size());

    registry.removeListeners(123);
    Assertions.assertNull(registry.getPacketListeners().get(123));
  }

  @Test
  @Order(30)
  void testListenerUnregisterByClassLoader() {
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    IPacketListener firstListener = Mockito.mock(IPacketListener.class);
    IPacketListener secondListener = Mockito.mock(IPacketListener.class);

    registry.addListener(123, firstListener, secondListener);
    Assertions.assertEquals(2, registry.getPacketListeners().get(123).size());

    registry.removeListeners(firstListener.getClass().getClassLoader());
    Assertions.assertNull(registry.getPacketListeners().get(123));
  }

  @Test
  @Order(40)
  void testListenerUnregisterAll() {
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    IPacketListener firstListener = Mockito.mock(IPacketListener.class);
    IPacketListener secondListener = Mockito.mock(IPacketListener.class);

    registry.addListener(123, firstListener, secondListener);
    Assertions.assertEquals(2, registry.getPacketListeners().get(123).size());

    registry.removeListeners();
    Assertions.assertNull(registry.getPacketListeners().get(123));
  }

  @Test
  @Order(50)
  void testListenerPost() {
    AtomicInteger eventCounter = new AtomicInteger();
    IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    registry.addListener(123, (channel, packet) -> eventCounter.incrementAndGet());
    registry.addListener(456, (channel, packet) -> eventCounter.addAndGet(5));

    registry.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(123));
    registry.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(123));

    Assertions.assertEquals(2, eventCounter.get());

    registry.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(456));
    registry.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(456));

    Assertions.assertEquals(12, eventCounter.getAndSet(0));

    // test with child registry

    IPacketListenerRegistry child = new DefaultPacketListenerRegistry(registry);
    child.addListener(456, (channel, packet) -> eventCounter.addAndGet(10));
    child.addListener(789, (channel, packet) -> eventCounter.addAndGet(20));

    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(123));
    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(123));

    Assertions.assertEquals(2, eventCounter.get());

    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(456));
    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(456));

    Assertions.assertEquals(32, eventCounter.get());

    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(789));
    child.handlePacket(Mockito.mock(INetworkChannel.class), this.mockPacketForChannel(789));

    Assertions.assertEquals(72, eventCounter.get());
  }

  private IPacket mockPacketForChannel(int channel) {
    IPacket packet = Mockito.mock(IPacket.class);
    Mockito.when(packet.getChannel()).thenReturn(channel);

    return packet;
  }
}
