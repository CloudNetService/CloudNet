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

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public class DefaultQueryPacketManagerTest {

  @Test
  void testSendQueryPacket() {
    var queryRequest = new BasePacket(-1, DataBuf.empty());
    var manager = new DefaultQueryPacketManager(Mockito.mock(NetworkChannel.class));

    var responseTask = manager.sendQueryPacket(queryRequest);
    Assertions.assertNotNull(responseTask);
    Assertions.assertEquals(Future.State.RUNNING, responseTask.state());

    var assignedId = queryRequest.uniqueId();
    Assertions.assertNotNull(assignedId);
    Assertions.assertTrue(manager.hasWaitingHandler(assignedId));
    Assertions.assertEquals(1, manager.waitingHandlerCount());

    var registeredResponseTask = manager.waitingHandler(assignedId);
    Assertions.assertNotNull(registeredResponseTask);
    Assertions.assertSame(responseTask, registeredResponseTask);
    Assertions.assertFalse(manager.hasWaitingHandler(assignedId));
    Assertions.assertEquals(0, manager.waitingHandlerCount());
    Assertions.assertEquals(Future.State.RUNNING, registeredResponseTask.state());
  }

  @Test
  void testSendQueryPacketWithFixedId() {
    var id = UUID.randomUUID();
    var queryRequest = new BasePacket(-1, DataBuf.empty());
    queryRequest.uniqueId(id);

    var manager = new DefaultQueryPacketManager(Mockito.mock(NetworkChannel.class));
    var firstResponseTask = manager.sendQueryPacket(queryRequest);
    Assertions.assertNotNull(firstResponseTask);
    Assertions.assertEquals(Future.State.RUNNING, firstResponseTask.state());
    Assertions.assertEquals(id, queryRequest.uniqueId());

    var secondResponseTask = manager.sendQueryPacket(queryRequest);
    Assertions.assertNotNull(secondResponseTask);
    Assertions.assertEquals(Future.State.RUNNING, secondResponseTask.state());
    Assertions.assertThrows(CompletionException.class, firstResponseTask::join);
    Assertions.assertInstanceOf(TimeoutException.class, firstResponseTask.exceptionNow());
  }
}
