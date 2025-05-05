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

package eu.cloudnetservice.driver.impl.network.protocol;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultPacketListenerRegistryTest {

  @Test
  @Order(0)
  void testListenerRegister() {
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    registry.addListener(123, (channel, packet) -> {});
    registry.addListener(123, (channel, packet) -> {});

    registry.addListener(456, (channel, packet) -> {
    });

    Assertions.assertTrue(registry.hasListeners(123));
    Assertions.assertTrue(registry.hasListeners(456));

    Assertions.assertEquals(2, registry.packetListeners().get(123).size());
    Assertions.assertEquals(1, registry.packetListeners().get(456).size());

    Assertions.assertEquals(3, registry.listeners().size());
    Assertions.assertEquals(2, registry.channels().size());
  }

  @Test
  @Order(10)
  void testListenerUnregister() {
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    var firstListener = Mockito.mock(PacketListener.class);
    var secondListener = Mockito.mock(PacketListener.class);

    registry.addListener(123, firstListener);
    registry.addListener(123, secondListener);
    Assertions.assertEquals(2, registry.packetListeners().get(123).size());

    registry.removeListener(123, firstListener);
    Assertions.assertEquals(1, registry.packetListeners().get(123).size());

    registry.removeListener(123, secondListener);
    Assertions.assertNull(registry.packetListeners().get(123));

    Assertions.assertFalse(registry.hasListeners(123));
  }

  @Test
  @Order(20)
  void testListenerUnregisterByChannel() {
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    var firstListener = Mockito.mock(PacketListener.class);
    var secondListener = Mockito.mock(PacketListener.class);

    registry.addListener(123, firstListener);
    registry.addListener(123, secondListener);
    Assertions.assertEquals(2, registry.packetListeners().get(123).size());

    registry.removeListeners(123);
    Assertions.assertNull(registry.packetListeners().get(123));
  }

  @Test
  @Order(30)
  void testListenerUnregisterByClassLoader() {
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    var firstListener = Mockito.mock(PacketListener.class);
    var secondListener = Mockito.mock(PacketListener.class);

    registry.addListener(123, firstListener);
    registry.addListener(123, secondListener);
    Assertions.assertEquals(2, registry.packetListeners().get(123).size());

    registry.removeListeners(firstListener.getClass().getClassLoader());
    Assertions.assertNull(registry.packetListeners().get(123));
  }

  @Test
  @Order(40)
  void testListenerUnregisterAll() {
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    var firstListener = Mockito.mock(PacketListener.class);
    var secondListener = Mockito.mock(PacketListener.class);

    registry.addListener(123, firstListener);
    registry.addListener(123, secondListener);
    Assertions.assertEquals(2, registry.packetListeners().get(123).size());

    registry.removeListeners();
    Assertions.assertNull(registry.packetListeners().get(123));
  }

  @Test
  @Order(50)
  void testListenerPost() {
    var eventCounter = new AtomicInteger();
    PacketListenerRegistry registry = new DefaultPacketListenerRegistry();

    registry.addListener(123, (channel, packet) -> eventCounter.incrementAndGet());
    registry.addListener(456, (channel, packet) -> eventCounter.addAndGet(5));

    registry.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(123));
    registry.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(123));

    Assertions.assertEquals(2, eventCounter.get());

    registry.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(456));
    registry.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(456));

    Assertions.assertEquals(12, eventCounter.getAndSet(0));

    // test with child registry

    PacketListenerRegistry child = new DefaultPacketListenerRegistry(registry);
    child.addListener(456, (channel, packet) -> eventCounter.addAndGet(10));
    child.addListener(789, (channel, packet) -> eventCounter.addAndGet(20));

    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(123));
    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(123));

    Assertions.assertEquals(2, eventCounter.get());

    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(456));
    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(456));

    Assertions.assertEquals(32, eventCounter.get());

    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(789));
    child.handlePacket(Mockito.mock(NetworkChannel.class), this.mockPacketForChannel(789));

    Assertions.assertEquals(72, eventCounter.get());
  }

  private Packet mockPacketForChannel(int channel) {
    var packet = Mockito.mock(Packet.class);
    Mockito.when(packet.channel()).thenReturn(channel);

    return packet;
  }
}
