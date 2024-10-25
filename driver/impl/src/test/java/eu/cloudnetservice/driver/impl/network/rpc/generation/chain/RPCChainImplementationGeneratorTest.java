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

package eu.cloudnetservice.driver.impl.network.rpc.generation.chain;

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCFactory;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCChained;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public class RPCChainImplementationGeneratorTest {

  @Test
  public void testChainedImplementationGeneration() {
    var mockedChannel = Mockito.mock(NetworkChannel.class);
    Mockito
      .doAnswer(_ -> {
        var rpcResponse = DataBuf.empty()
          .writeByte(RPCInvocationResult.STATUS_OK)
          .writeObject("hello world!");
        return TaskUtil.finishedFuture(new BasePacket(-1, rpcResponse));
      })
      .when(mockedChannel)
      .sendQueryAsync(Mockito.any(Packet.class));

    var rpcFactory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var databaseImplementationFactory = rpcFactory.newRPCBasedImplementationBuilder(RootDatabase.class)
      .targetChannel(mockedChannel)
      .generateImplementation();

    var rootDatabase = databaseImplementationFactory.allocate();
    Assertions.assertEquals("hello world!", rootDatabase.bar("test", 123));
    Assertions.assertEquals("hello world!", rootDatabase.foo(123456L).join());

    var subDatabase = rootDatabase.getSubDatabase("test", 2048L);
    Assertions.assertEquals("test", subDatabase.name);
    Assertions.assertEquals(2048L, subDatabase.documentCount);
    Assertions.assertEquals("hello world!", subDatabase.getName());

    var subSubDatabase = subDatabase.subSubDatabase();
    Assertions.assertEquals("<hidden in the depths>", subSubDatabase.getName());

    Mockito.verify(mockedChannel, Mockito.times(3)).sendQueryAsync(Mockito.any(Packet.class));
  }

  // ============ Testing Class Definitions

  public interface SubSubDatabase {

    String getName();

    String subDocumentCount();
  }

  public abstract static class RootDatabase {

    public abstract String bar(String a, int b);

    public abstract CompletableFuture<String> foo(long xyz);

    @RPCChained(parameterMapping = {
      0, 1, // map name parameter to seconds constructor arg
      1, 0, // map document count param to first constructor arg
    })
    public abstract SubDatabase getSubDatabase(String name, long documentCount);
  }

  public abstract static class SubDatabase {

    private final String name;
    private final long documentCount;

    @RPCInvocationTarget
    public SubDatabase(long documentCount, String name) {
      this.name = name;
      this.documentCount = documentCount;
    }

    public String getName() {
      return "testing";
    }

    @RPCChained(
      generationFlags = 0x00, // instruction to not implement all methods
      baseImplementation = SubSubDatabaseImplBase.class
    )
    public abstract SubSubDatabase subSubDatabase();
  }

  public abstract static class SubSubDatabaseImplBase implements SubSubDatabase {

    @Override
    public String getName() {
      return "<hidden in the depths>";
    }
  }
}
