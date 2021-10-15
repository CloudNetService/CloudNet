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

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultQueryPacketManager;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

public class DefaultQueryPacketManagerTest {

  @Test
  void testSendQueryPacket() {
    Pair<IPacket, AtomicReference<UUID>> mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(INetworkChannel.class));

    manager.sendQueryPacket(mockedPacket.getFirst());

    Assertions.assertTrue(manager.hasWaitingHandler(mockedPacket.getSecond().get()));
    Assertions.assertNotNull(manager.getWaitingHandlers().get(mockedPacket.getSecond().get()));

    Assertions.assertTrue(manager.unregisterWaitingHandler(mockedPacket.getSecond().get()));
    Assertions.assertFalse(manager.hasWaitingHandler(mockedPacket.getSecond().get()));
  }

  @Test
  void testSendQueryPacketWithFixedId() {
    UUID uniqueId = UUID.randomUUID();
    Pair<IPacket, AtomicReference<UUID>> mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(INetworkChannel.class));

    manager.sendQueryPacket(mockedPacket.getFirst(), uniqueId);

    Assertions.assertEquals(uniqueId, mockedPacket.getSecond().get());
    Assertions.assertTrue(manager.hasWaitingHandler(uniqueId));
    Assertions.assertNotNull(manager.getWaitingHandlers().get(uniqueId));

    Assertions.assertTrue(manager.unregisterWaitingHandler(uniqueId));
    Assertions.assertFalse(manager.hasWaitingHandler(uniqueId));
  }

  @Test
  void testGetAndRemoveHandler() {
    Pair<IPacket, AtomicReference<UUID>> mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(INetworkChannel.class));

    CompletableTask<IPacket> task = manager.sendQueryPacket(mockedPacket.getFirst());

    Assertions.assertEquals(task, manager.getWaitingHandler(mockedPacket.getSecond().get()));
    Assertions.assertFalse(manager.hasWaitingHandler(mockedPacket.getSecond().get()));
  }

  @Test
  @Timeout(10)
  void testHandlerTimeout() throws InterruptedException {
    Pair<IPacket, AtomicReference<UUID>> mockedPacket = this.mockUniqueIdAblePacket();
    QueryPacketManager manager = new DefaultQueryPacketManager(Mockito.mock(INetworkChannel.class), 2000);

    CompletableTask<IPacket> task = manager.sendQueryPacket(mockedPacket.getFirst());
    Assertions.assertTrue(manager.hasWaitingHandler(mockedPacket.getSecond().get()));

    Thread.sleep(2500);

    Assertions.assertNull(manager.getWaitingHandler(mockedPacket.getSecond().get()));
    manager.sendQueryPacket(mockedPacket.getFirst());

    Assertions.assertTrue(task.isDone());
  }

  private Pair<IPacket, AtomicReference<UUID>> mockUniqueIdAblePacket() {
    AtomicReference<UUID> reference = new AtomicReference<>();

    IPacket packet = Mockito.mock(IPacket.class);
    Mockito
      .doAnswer(invocation -> {
        reference.set(invocation.getArgument(0));
        return null;
      })
      .when(packet)
      .setUniqueId(Mockito.any());

    return new Pair<>(packet, reference);
  }
}
