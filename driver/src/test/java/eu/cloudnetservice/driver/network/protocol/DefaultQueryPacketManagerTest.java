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

package eu.cloudnetservice.driver.network.protocol;

import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.defaults.DefaultQueryPacketManager;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

public class DefaultQueryPacketManagerTest {

  @Test
  void testSendQueryPacket() {
    var mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(NetworkChannel.class));

    manager.sendQueryPacket(mockedPacket.first());

    Assertions.assertTrue(manager.hasWaitingHandler(mockedPacket.second().get()));
    Assertions.assertNotNull(manager.waitingHandlers().get(mockedPacket.second().get()));

    Assertions.assertTrue(manager.unregisterWaitingHandler(mockedPacket.second().get()));
    Assertions.assertFalse(manager.hasWaitingHandler(mockedPacket.second().get()));
  }

  @Test
  void testSendQueryPacketWithFixedId() {
    var uniqueId = UUID.randomUUID();
    var mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(NetworkChannel.class));

    manager.sendQueryPacket(mockedPacket.first(), uniqueId);

    Assertions.assertEquals(uniqueId, mockedPacket.second().get());
    Assertions.assertTrue(manager.hasWaitingHandler(uniqueId));
    Assertions.assertNotNull(manager.waitingHandlers().get(uniqueId));

    Assertions.assertTrue(manager.unregisterWaitingHandler(uniqueId));
    Assertions.assertFalse(manager.hasWaitingHandler(uniqueId));
  }

  @Test
  void testGetAndRemoveHandler() {
    var mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(NetworkChannel.class));

    var task = manager.sendQueryPacket(mockedPacket.first());

    Assertions.assertEquals(task, manager.waitingHandler(mockedPacket.second().get()));
    Assertions.assertFalse(manager.hasWaitingHandler(mockedPacket.second().get()));
  }

  @Test
  @Timeout(10)
  void testHandlerTimeout() throws InterruptedException {
    var mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(
      Mockito.mock(NetworkChannel.class),
      Duration.ofSeconds(2));

    var task = manager.sendQueryPacket(mockedPacket.first());
    Assertions.assertTrue(manager.hasWaitingHandler(mockedPacket.second().get()));

    Thread.sleep(2500);

    Assertions.assertNull(manager.waitingHandler(mockedPacket.second().get()));
    manager.sendQueryPacket(mockedPacket.first());

    Assertions.assertTrue(task.isDone());
  }

  private Tuple2<Packet, AtomicReference<UUID>> mockUniqueIdAblePacket() {
    var reference = new AtomicReference<UUID>();

    var packet = Mockito.mock(Packet.class);
    Mockito
      .doAnswer(invocation -> {
        reference.set(invocation.getArgument(0));
        return null;
      })
      .when(packet)
      .uniqueId(Mockito.any());

    return new Tuple2<>(packet, reference);
  }
}
