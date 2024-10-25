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

package eu.cloudnetservice.driver.impl.network.rpc.handler;

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCFactory;
import eu.cloudnetservice.driver.impl.network.rpc.listener.RPCPacketListener;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public class RPCNetworkHandlingTest {

  @Test
  void testRPCHandling() {
    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());

    // construct the network handler that would usually process the requests
    var rpcHandlerRegistry = new DefaultRPCHandlerRegistry();
    var rpcNetworkHandler = new RPCPacketListener(rpcHandlerRegistry);

    // mock the channel handling
    var mockedChannel = Mockito.mock(NetworkChannel.class);
    var lastRPCRequest = new AtomicReference<Packet>();
    var responseQueue = new LinkedBlockingQueue<Packet>(1);
    Mockito
      .doAnswer(invocation -> {
        Packet rpcRequest = invocation.getArgument(0);
        rpcRequest.uniqueId(UUID.randomUUID()); // usually QueryManager would take over this job
        Assertions.assertEquals(NetworkConstants.INTERNAL_RPC_COM_CHANNEL, rpcRequest.channel());
        lastRPCRequest.set(rpcRequest);
        rpcNetworkHandler.handle(mockedChannel, rpcRequest);
        return TaskUtil.supplyAsync(responseQueue::take);
      })
      .when(mockedChannel)
      .sendQueryAsync(Mockito.any(Packet.class));
    Mockito
      .doAnswer(invocation -> {
        var lastRequest = lastRPCRequest.getAndSet(null);
        if (lastRequest != null) {
          Packet rpcResponse = invocation.getArgument(0);
          Assertions.assertEquals(-1, rpcResponse.channel());
          Assertions.assertEquals(lastRequest.uniqueId(), rpcResponse.uniqueId());
          responseQueue.offer(rpcResponse);
        }
        return null;
      })
      .when(mockedChannel)
      .sendPacket(Mockito.any(Packet.class));

    // build an RPC handler for RPCHandlingTest and register it into the rpc handling registry
    var handlingTestImpl = new RPCHandlingTestImpl();
    var handlingTestHandler = rpcFactory.newRPCHandlerBuilder(RPCHandlingTest.class)
      .targetInstance(handlingTestImpl)
      .build();
    rpcHandlerRegistry.registerHandler(handlingTestHandler);

    // build & register an RPC handler for RPCHandlingTestChained, this is required even if we don't want
    // to bind a specific instance to it. doing this prevents attacks on trying to access methods using chains
    // that are not designed to be called using chains (for example to exploit a security issue)
    var handlingTestChainedHandler = rpcFactory.newRPCHandlerBuilder(RPCHandlingTestChained.class).build();
    rpcHandlerRegistry.registerHandler(handlingTestChainedHandler);

    // build the sender which sends all RPC requests into the mocked network channel
    var handlingTestSender = rpcFactory.newRPCSenderBuilder(RPCHandlingTest.class)
      .targetChannel(mockedChannel)
      .build();
    var handlingTestChainSender = rpcFactory.newRPCSenderBuilder(RPCHandlingTestChained.class)
      .targetChannel(mockedChannel)
      .build();

    // call the RPCHandlingTest.helloWorld method & validate the response
    var helloWorldDesc = MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_long);
    var helloWorldRPC = handlingTestSender.invokeMethod("helloWorld", helloWorldDesc, 123L);
    var helloWorldResponse = Assertions.assertDoesNotThrow(() -> (String) helloWorldRPC.fireSync());
    Assertions.assertEquals("Hello World, for the 123. time!", helloWorldResponse);

    // call the round method
    var roundDesc = MethodTypeDesc.of(ConstantDescs.CD_long, ConstantDescs.CD_double);
    var roundRPC = handlingTestSender.invokeMethod("round", roundDesc, 189.793491);
    var roundResponse = Assertions.assertDoesNotThrow(() -> (long) roundRPC.fire().join());
    Assertions.assertEquals(190L, roundResponse);

    // call a method that causes an exception
    var sumDesc = MethodTypeDesc.of(ConstantDescs.CD_long, ConstantDescs.CD_long, ConstantDescs.CD_int);
    var sumRPC = handlingTestSender.invokeMethod("sum", sumDesc, Long.MAX_VALUE, 1024);
    var sumResponse = Assertions.assertThrows(RPCExecutionException.class, sumRPC::fireSync);
    Assertions.assertEquals("ArithmeticException: long overflow", sumResponse.getMessage());
    var stackTrace = sumResponse.getStackTrace();
    Assertions.assertTrue(stackTrace.length > 0);
    Assertions.assertEquals("java.lang.Math", stackTrace[0].getClassName());
    Assertions.assertEquals("addExact", stackTrace[0].getMethodName());

    // get the base rpc to build a chained rpc - note that this is not called here
    var cdHandlingTestChained = ClassDesc.of(RPCHandlingTestChained.class.getName());
    var chainedTestDesc = MethodTypeDesc.of(cdHandlingTestChained, ConstantDescs.CD_short);
    var chainTestRPC = handlingTestSender.invokeMethod("chainedTest", chainedTestDesc, (short) 235);

    // obtain a rpc to call the addToCounter method as a chained call. note that we obtained a RPCHandlingTestChained
    // with a count of 235 and adding 9.5 to that, but yet we get 249.5D as a response. This is due to the fact that
    // the remote implementation adds 5 to the provided counter when creating the instance
    var addToCounterDesc = MethodTypeDesc.of(ConstantDescs.CD_double, ConstantDescs.CD_float);
    var addToCounterRPC = handlingTestChainSender.invokeMethod("addToCounter", addToCounterDesc, 9.5F);
    var addToCounterChain = chainTestRPC.join(addToCounterRPC);
    var addToCounterResult = Assertions.assertDoesNotThrow(() -> (double) addToCounterChain.fireSync());
    Assertions.assertEquals(249.5D, addToCounterResult);

    // obtain another chain to call the printCounter method in the chain class
    var printCounterDesc = MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_char, ConstantDescs.CD_String);
    var printCounterRPC = handlingTestChainSender.invokeMethod("printCounter", printCounterDesc, 'X', null);
    var printCounterChain = chainTestRPC.join(printCounterRPC);
    var printCounterResult = Assertions.assertDoesNotThrow(() -> (String) printCounterChain.fire().join());
    Assertions.assertEquals("X240null", printCounterResult);

    // if a method call returns null in the middle of a chain, then an exceptional result with a null pointer
    // exception as the cause is sent back to the calling component
    var chainTestNegativeRPC = handlingTestSender.invokeMethod("chainedTest", chainedTestDesc, (short) -55);
    var printCounterChain2 = chainTestNegativeRPC.join(printCounterRPC);
    var printCounterResult2 = Assertions.assertThrows(RPCExecutionException.class, printCounterChain2::fireSync);
    Assertions.assertTrue(printCounterResult2.getMessage().startsWith("NullPointerException:"));
  }

  public interface RPCHandlingTest {

    String helloWorld(long clickCounter);

    long round(double input);

    long sum(long a, int b);

    default RPCHandlingTestChained chainedTest(short someCounter) {
      return new HandlingTestChainedImpl(someCounter);
    }
  }

  public interface RPCHandlingTestChained {

    double addToCounter(float count);

    String printCounter(char prefix, String suffix);
  }

  public static final class RPCHandlingTestImpl implements RPCHandlingTest {

    @Override
    public String helloWorld(long clickCounter) {
      return String.format("Hello World, for the %d. time!", clickCounter);
    }

    @Override
    public long round(double input) {
      return Math.round(input);
    }

    @Override
    public long sum(long a, int b) {
      return Math.addExact(a, b);
    }

    @Override
    public RPCHandlingTestChained chainedTest(short someCounter) {
      return someCounter <= 0 ? null : new HandlingTestChainedImpl(someCounter + 5);
    }
  }

  public record HandlingTestChainedImpl(int someCounter) implements RPCHandlingTestChained {

    @Override
    public double addToCounter(float count) {
      return this.someCounter + count;
    }

    @Override
    public String printCounter(char prefix, String suffix) {
      return String.format("%c%d%s", prefix, this.someCounter, suffix);
    }
  }
}
