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
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkComponent;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.QueryPacketManager;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCExecutionException;
import de.dytanic.cloudnet.driver.network.rpc.listener.RPCPacketListener;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ThreadSnapshot;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

public class DefaultRPCHandlerTest {

  @Test
  @Timeout(20)
  void testMethodInvocation() {
    // handler registry init
    RPCHandlerRegistry registry = new DefaultRPCHandlerRegistry();
    // handler init
    IPacketListener listener = new RPCPacketListener(registry);
    // factory init
    RPCProviderFactory factory = new DefaultRPCProviderFactory(
      new DefaultObjectMapper(),
      DataBufFactory.defaultFactory());
    // handler initialize
    var backingHandler = new AtomicLong();
    var handlerNested = factory.newHandler(TestApiClassNested.class, null);
    var handler = factory.newHandler(TestApiClass.class, new TestApiClass(backingHandler));
    var veryHandlerNested = factory.newHandler(TestApiClassVeryNested.class, new TestApiClassVeryNestedImpl());
    // register the handler
    registry.registerHandler(handler);
    registry.registerHandler(handlerNested);
    registry.registerHandler(veryHandlerNested);
    // networking mocks
    var resultListener = new AtomicReference<CompletableTask<IPacket>>(new CompletableTask<>());
    // receiver
    // we need a query manager for the listener
    var manager = Mockito.mock(QueryPacketManager.class);
    Mockito.when(manager.sendQueryPacket(Mockito.any(), Mockito.any())).then(invocation -> {
      resultListener.get().complete(invocation.getArgument(0));
      return null;
    });
    // the channel to which the result should be sent
    var resultChannel = Mockito.mock(INetworkChannel.class);
    Mockito.when(resultChannel.queryPacketManager()).thenReturn(manager);
    // sender
    var channel = Mockito.mock(INetworkChannel.class);
    Mockito
      .doAnswer(invocation -> {
        // the packet has no unique id yet, set one
        IPacket packet = invocation.getArgument(0);
        packet.uniqueId(UUID.randomUUID());
        // post the packet to the listener
        listener.handle(resultChannel, packet);
        return resultListener.get();
      })
      .when(channel)
      .sendQueryAsync(Mockito.any(IPacket.class));
    // network component
    var component = Mockito.mock(INetworkComponent.class);
    Mockito.when(component.firstChannel()).thenReturn(channel);
    // RPC sender
    var sender = factory.providerForClass(component, TestApiClass.class);
    var nestedSender = factory.providerForClass(component, TestApiClassNested.class);
    var veryNestedSender = factory.providerForClass(component, TestApiClassVeryNested.class);
    // pre-save the arguments we are using
    var snapshot = ProcessSnapshot.self();
    var integers = Arrays.asList(185, 186, 188);
    // send an invoke request of the method to the handler
    Map<Long, Map<String, String>> result = sender
      .invokeMethod("handleProcessSnapshot", snapshot, integers, 187)
      .fireSync();
    // ensure that the handler received the request
    var key = TestApiClass.calculateResult(snapshot, integers, 187);
    Assertions.assertEquals(key, backingHandler.get());
    // ensure that the result is there
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.get(key));
    Assertions.assertTrue(Maps
      .difference(ImmutableMap.of("test1", "test2", "test3", "test4"), result.get(key))
      .areEqual());
    // nested call test
    resultListener.set(new CompletableTask<>());
    // with correct argument
    result = sender
      .invokeMethod("nestedClass", "Test1234")
      .join(nestedSender.invokeMethod("handleProcessSnapshot1", snapshot, integers, 187))
      .fireSync();
    Assertions.assertNull(result);
    // with a correct call to get a result
    resultListener.set(new CompletableTask<>());
    result = sender
      .invokeMethod("nestedClass", "Test123")
      .join(nestedSender.invokeMethod("handleProcessSnapshot1", snapshot, integers, 187))
      .fireSync();
    // ensure that the result is there
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.get(key));
    Assertions.assertTrue(Maps
      .difference(ImmutableMap.of("test1", "test2", "test3", "test4"), result.get(key))
      .areEqual());
    // with a correct call to get a result & but with an invalid to handleProcessSnapshot1 (should result in an exception)
    resultListener.set(new CompletableTask<>());
    try {
      sender
        .invokeMethod("nestedClass", "Test123")
        .join(nestedSender.invokeMethod("handleProcessSnapshot1", snapshot, integers, 185))
        .fireSync();
      Assertions.fail("A call to handleProcessSnapshot1 with the invalid argument '185' should result in an exception");
    } catch (Exception exception) {
      Assertions.assertTrue(exception instanceof RPCExecutionException);
      Assertions.assertTrue(exception.getMessage().startsWith(String.format(
        "IllegalArgumentException(Come on dude!! IS IT THAT BAD? @ %s.handleProcessSnapshot1(%s.java:",
        DefaultRPCHandlerTest.TestApiClassNested.class.getName(),
        this.getClass().getSimpleName()
      )));
    }
    // triple join :)
    resultListener.set(new CompletableTask<>());
    result = sender
      .invokeMethod("nestedClass", "Test123")
      .join(nestedSender.invokeMethod("toVeryNested", 1234))
      .join(veryNestedSender.invokeMethod("handleProcessSnapshot2", snapshot, integers, 187))
      .fireSync();
    // ensure that the result is there
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.get(key));
    Assertions.assertTrue(Maps
      .difference(ImmutableMap.of("test1", "test2", "test3", "test4"), result.get(key))
      .areEqual());
  }

  public interface TestApiClassVeryNested {

    Map<Long, Map<String, String>> handleProcessSnapshot2(ProcessSnapshot s, List<Integer> i, int primaryId);
  }

  public record TestApiClass(AtomicLong eventCounter) {

    static long calculateResult(ProcessSnapshot snapshot, List<Integer> integers, int primary) {
      // some values from the snapshot and the fixed 187 argument
      long result = primary + snapshot.currentLoadedClassCount() + snapshot.pid();
      // the integers submitted summed up
      result += integers.stream().mapToInt(Integer::intValue).sum();
      // the thread ids summed up
      return result + snapshot.threads().stream().mapToLong(ThreadSnapshot::id).sum();
    }

    public Map<Long, Map<String, String>> handleProcessSnapshot(ProcessSnapshot s, List<Integer> i, int primaryId) {
      return ImmutableMap.of(
        eventCounter.addAndGet(calculateResult(s, i, primaryId)),
        ImmutableMap.of("test1", "test2", "test3", "test4"));
    }

    public TestApiClassNested nestedClass(String arg) {
      return arg.equals("Test123") ? new TestApiClassNested() : null;
    }
  }

  public static final class TestApiClassNested {

    public Map<Long, Map<String, String>> handleProcessSnapshot1(ProcessSnapshot s, List<Integer> i, int primaryId) {
      if (primaryId != 187) {
        throw new IllegalArgumentException("Come on dude!! IS IT THAT BAD?");
      } else {
        return ImmutableMap.of(
          TestApiClass.calculateResult(s, i, primaryId),
          ImmutableMap.of("test1", "test2", "test3", "test4"));
      }
    }

    public TestApiClassVeryNested toVeryNested(int abc) {
      return abc == 1234 ? new TestApiClassVeryNestedImpl() : null;
    }
  }

  public static final class TestApiClassVeryNestedImpl implements TestApiClassVeryNested {

    public Map<Long, Map<String, String>> handleProcessSnapshot2(ProcessSnapshot s, List<Integer> i, int primaryId) {
      return ImmutableMap.of(
        TestApiClass.calculateResult(s, i, primaryId),
        ImmutableMap.of("test1", "test2", "test3", "test4"));
    }
  }
}
