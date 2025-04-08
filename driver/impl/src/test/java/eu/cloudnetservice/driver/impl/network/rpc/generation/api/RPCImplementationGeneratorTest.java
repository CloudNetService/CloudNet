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

package eu.cloudnetservice.driver.impl.network.rpc.generation.api;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCFactory;
import eu.cloudnetservice.driver.impl.network.rpc.generation.RPCInternalInstanceFactory;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public class RPCImplementationGeneratorTest {

  @Test
  void testFullGeneration() {
    var mockedChannel = Mockito.mock(NetworkChannel.class);
    Mockito
      .doAnswer(_ -> {
        var rpcResponse = DataBuf.empty()
          .writeByte(RPCInvocationResult.STATUS_OK)
          .writeObject(true);
        return TaskUtil.finishedFuture(new BasePacket(-1, rpcResponse));
      })
      .when(mockedChannel)
      .sendQueryAsync(Mockito.any(Packet.class));

    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var databaseImplementationFactory = rpcFactory.newRPCBasedImplementationBuilder(Database.class)
      .targetChannel(mockedChannel)
      .implementConcreteMethods()
      .generateImplementation();

    var wrapperDatabase = databaseImplementationFactory.allocate();
    Assertions.assertTrue(wrapperDatabase.contains("world"));
    Assertions.assertTrue(wrapperDatabase.containsAsync("world").join());

    Mockito.verify(mockedChannel, Mockito.times(2)).sendQueryAsync(Mockito.any(Packet.class));
  }

  @Test
  void testExpectationOfExistingMethods() {
    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var databaseImplementationFactory = rpcFactory.newRPCBasedImplementationBuilder(BaseDatabase.class)
      .targetChannel(() -> null)
      .generateImplementation();
    var database = databaseImplementationFactory.allocate();

    var document = database.get("world");
    Assertions.assertSame(BaseDatabase.TEST_DOCUMENT, document);

    var future = database.getAsync("hello");
    Assertions.assertTrue(future.isDone());
    Assertions.assertSame(BaseDatabase.TEST_DOCUMENT, TaskUtil.getOrDefault(future, null));
  }

  @Test
  void testDownPassingRPCSenderToExtendingClass() {
    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var databaseImplementationFactory = rpcFactory.newRPCBasedImplementationBuilder(SenderNeedingDatabase.class)
      .targetChannel(() -> null)
      .generateImplementation();
    var database = databaseImplementationFactory
      .withAdditionalConstructorParameters(RPCInternalInstanceFactory.SpecialArg.RPC_SENDER)
      .allocate();

    Assertions.assertNotNull(database.rpcSender);
  }

  @Test
  void testImplementationOfMethodsWithDifferentStackSize() {
    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var testFactory = rpcFactory.newRPCBasedImplementationBuilder(TestRPCParameters.class)
      .targetChannel(() -> {
        throw new IllegalStateException("expected illegal state ;^)");
      })
      .implementConcreteMethods()
      .generateImplementation();

    var instance = testFactory
      .withAdditionalConstructorParameters(12, 45L, "789", new int[]{1, 5}, new long[]{89, 1290}, new String[]{"test"})
      .allocate();

    Assertions.assertNotNull(instance);
    Assertions.assertThrows(
      IllegalStateException.class, // due to the channel supplier above, at this point args were already loaded
      () -> instance.testLongIntStringArray(123L, 123, "world", new String[]{"testing"}));
  }
}
