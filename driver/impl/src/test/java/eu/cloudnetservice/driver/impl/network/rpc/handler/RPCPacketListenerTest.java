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
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import io.vavr.Tuple2;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.IntSummaryStatistics;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

@EnableServicesInject
public class RPCPacketListenerTest {

  static Packet craftRPCRequest(
    int chainLength,
    String className,
    String methodName,
    String methodDesc,
    Object[] args,
    boolean wantsResponse
  ) {
    var buffer = DataBuf.empty()
      .writeInt(chainLength)
      .writeString(className)
      .writeString(methodName)
      .writeString(methodDesc);
    for (var argument : args) {
      buffer.writeObject(argument);
    }

    var packet = new BasePacket(NetworkConstants.INTERNAL_RPC_COM_CHANNEL, buffer);
    packet.uniqueId(wantsResponse ? UUID.randomUUID() : null); // usually set by QueryManager
    return packet;
  }

  // first element -> mocked channel
  // second element -> queue for sent packets
  static Tuple2<NetworkChannel, BlockingQueue<Packet>> mockNetworkChannel() {
    var responseQueue = new LinkedBlockingQueue<Packet>();
    var mockedChannel = Mockito.mock(NetworkChannel.class);
    Mockito
      .doAnswer(invocation -> {
        Packet response = invocation.getArgument(0);
        Assertions.assertNotNull(response.uniqueId());
        Assertions.assertEquals(-1, response.channel());
        responseQueue.offer(response);
        return null;
      })
      .when(mockedChannel)
      .sendPacket(Mockito.any(Packet.class));
    return new Tuple2<>(mockedChannel, responseQueue);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -5, -10, -100, -5689, -44_444_444})
  void testNegativeChainLengthIsRejected(int chainLength) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var desc = MethodTypeDesc.of(ConstantDescs.CD_String).descriptorString();
    var invalidCraftedRPC = craftRPCRequest(chainLength, "Test", "test", desc, new Object[0], true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_BAD_REQUEST, response.content().readByte());
    Assertions.assertEquals("invalid chain length", response.content().readString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Test", "java.lang.Math", "java.util.ArrayList"})
  void testRequestsToClassesWithoutHandlerAreRejected(String className) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var desc = MethodTypeDesc.of(ConstantDescs.CD_int).descriptorString();
    var invalidCraftedRPC = craftRPCRequest(1, className, "test", desc, new Object[0], true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_BAD_REQUEST, response.content().readByte());
    Assertions.assertEquals("missing explicitly defined target handler to call", response.content().readString());
  }

  @ParameterizedTest
  @ValueSource(classes = {Math.class, IntSummaryStatistics.class})
  void testMethodCallIsRejectedIfNoInstanceIsBound(Class<?> target) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var handler = factory.newRPCHandlerBuilder(target).build();
    handlerRegistry.registerHandler(handler);

    var desc = MethodTypeDesc.of(ConstantDescs.CD_int).descriptorString();
    var invalidCraftedRPC = craftRPCRequest(1, target.getName(), "test", desc, new Object[0], true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_SERVER_ERROR, response.content().readByte());
    Assertions.assertEquals("no instance to invoke the method on", response.content().readString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"(ILjava/lang/String)I", "(ILjava/lang/String;)", "Ljava/lang/String", "II)J", "II)J"})
  void testInvalidMethodDescriptorIsRejected(String descriptor) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var handler = factory
      .newRPCHandlerBuilder(IntSummaryStatistics.class)
      .targetInstance(new IntSummaryStatistics())
      .build();
    handlerRegistry.registerHandler(handler);

    var invalidCraftedRPC = craftRPCRequest(
      1,
      IntSummaryStatistics.class.getName(),
      "test",
      descriptor,
      new Object[0],
      true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_BAD_REQUEST, response.content().readByte());
    Assertions.assertEquals("invalid target method descriptor", response.content().readString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"getAvg", "getAVerage", "get", "getAveragE", "getMin", "accept"})
  void testNonExistingOrNotMatchingMethodsAreRejected(String methodName) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var handler = factory
      .newRPCHandlerBuilder(IntSummaryStatistics.class)
      .targetInstance(new IntSummaryStatistics())
      .build();
    handlerRegistry.registerHandler(handler);

    var desc = MethodTypeDesc.of(ConstantDescs.CD_double).descriptorString();
    var invalidCraftedRPC = craftRPCRequest(
      1,
      IntSummaryStatistics.class.getName(),
      methodName,
      desc,
      new Object[0],
      true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_BAD_REQUEST, response.content().readByte());
    Assertions.assertEquals("target method not found", response.content().readString());
  }

  @Test
  void testArgumentMismatchIsRejected() throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var handler = factory
      .newRPCHandlerBuilder(IntSummaryStatistics.class)
      .targetInstance(new IntSummaryStatistics())
      .build();
    handlerRegistry.registerHandler(handler);

    var desc = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int).descriptorString();
    var invalidCraftedRPC = craftRPCRequest(
      1,
      IntSummaryStatistics.class.getName(),
      "accept",
      desc,
      new Object[0],
      true);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RPCInvocationResult.STATUS_BAD_REQUEST, response.content().readByte());
    Assertions.assertEquals("provided arguments do not satisfy (I)V", response.content().readString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"(L)V", "I)V"})
  void testNothingIsSentBackIfNoResultIsExpected(String methodDescriptor) throws Exception {
    var handlerRegistry = new DefaultRPCHandlerRegistry();
    var packetListener = new RPCPacketListener(handlerRegistry);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var handler = factory
      .newRPCHandlerBuilder(IntSummaryStatistics.class)
      .targetInstance(new IntSummaryStatistics())
      .build();
    handlerRegistry.registerHandler(handler);

    var invalidCraftedRPC = craftRPCRequest(
      1,
      IntSummaryStatistics.class.getName(),
      "accept",
      methodDescriptor,
      new Object[]{135},
      false);

    var mockResult = mockNetworkChannel();
    packetListener.handle(mockResult._1(), invalidCraftedRPC);

    var response = mockResult._2().poll();
    Assertions.assertNull(response);
  }
}
