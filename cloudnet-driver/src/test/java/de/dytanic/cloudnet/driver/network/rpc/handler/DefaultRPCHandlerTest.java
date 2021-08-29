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

package de.dytanic.cloudnet.driver.network.rpc.handler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.packet.RPCQueryPacket;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ThreadSnapshot;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

public class DefaultRPCHandlerTest {

  @Test
  @Timeout(20)
  void testMethodInvocation() {
    // factory init
    RPCProviderFactory factory = new DefaultRPCProviderFactory(
      new DefaultObjectMapper(),
      DataBufFactory.defaultFactory());
    // handler initialize
    AtomicLong backingHandler = new AtomicLong();
    RPCHandler handler = factory.newHandler(TestApiClass.class, new TestApiClass(backingHandler));
    // networking mocks
    INetworkChannel channel = Mockito.mock(INetworkChannel.class);
    Mockito
      .doAnswer(invocation -> {
        DataBuf buf = invocation.getArgument(0, IPacket.class).getContent();
        Assertions.assertEquals(TestApiClass.class.getCanonicalName(), buf.readString());

        return CompletedTask.create(new RPCQueryPacket(handler.handleRPC((INetworkChannel) invocation.getMock(), buf)));
      })
      .when(channel)
      .sendQueryAsync(Mockito.any(IPacket.class));
    // network component
    INetworkComponent component = Mockito.mock(INetworkComponent.class);
    Mockito.when(component.getFirstChannel()).thenReturn(channel);
    // RPC sender
    RPCSender sender = factory.providerForClass(component, TestApiClass.class);
    // pre-save the arguments we are using
    ProcessSnapshot snapshot = ProcessSnapshot.self();
    List<Integer> integers = Arrays.asList(185, 186, 188);
    // send an invoke request of the method to the handler
    Map<Long, Map<String, String>> result = sender
      .invokeMethod("handleProcessSnapshot", snapshot, integers, 187)
      .fireSync();
    // ensure that the handler received the request
    long key = TestApiClass.calculateResult(snapshot, integers, 187);
    Assertions.assertEquals(key, backingHandler.get());
    // ensure that the result is there
    Assertions.assertNotNull(result.get(key));
    Assertions.assertTrue(Maps
      .difference(ImmutableMap.of("test1", "test2", "test3", "test4"), result.get(key))
      .areEqual());
  }

  public static final class TestApiClass {

    private final AtomicLong eventCounter;

    public TestApiClass(AtomicLong eventCounter) {
      this.eventCounter = eventCounter;
    }

    static long calculateResult(ProcessSnapshot snapshot, List<Integer> integers, int primary) {
      // some values from the snapshot and the fixed 187 argument
      long result = primary + snapshot.getCurrentLoadedClassCount() + snapshot.getPid();
      // the integers submitted summed up
      result += integers.stream().mapToInt(Integer::intValue).sum();
      // the thread ids summed up
      return result + snapshot.getThreads().stream().mapToLong(ThreadSnapshot::getId).sum();
    }

    public Map<Long, Map<String, String>> handleProcessSnapshot(ProcessSnapshot s, List<Integer> i, int primaryId) {
      return ImmutableMap.of(
        eventCounter.addAndGet(calculateResult(s, i, primaryId)),
        ImmutableMap.of("test1", "test2", "test3", "test4"));
    }
  }
}
