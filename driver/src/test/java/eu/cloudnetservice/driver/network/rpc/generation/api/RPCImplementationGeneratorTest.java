/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.generation.api;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RPCImplementationGeneratorTest {

  @Test
  void testFullGeneration() {
    var key = UUID.randomUUID().toString();

    var immediateRpc = Mockito.mock(RPC.class);
    Mockito.when(immediateRpc.fireSync()).thenReturn(true);
    Mockito.when(immediateRpc.fire()).thenReturn(Task.completedTask(true));

    var sender = Mockito.mock(RPCSender.class);
    Mockito.when(sender.invokeMethod(Mockito.anyString(), Mockito.any())).then(invocation -> {
      String method = invocation.getArgument(0);
      var arg = invocation.getArgument(1);

      Assertions.assertTrue(method.startsWith("contains"));
      Assertions.assertNotNull(arg);
      Assertions.assertEquals(key, arg);

      return immediateRpc;
    });

    // we're using direct generation for easier testing with the sender
    var wrapperDatabase = ApiImplementationGenerator.generateApiImplementation(
      Database.class,
      GenerationContext.forClass(Database.class).implementAllMethods(true).build(),
      sender
    ).newInstance();

    var result = wrapperDatabase.contains(key);
    Assertions.assertTrue(result);

    var future = wrapperDatabase.containsAsync(key);
    Assertions.assertTrue(future.isDone());
    Assertions.assertTrue(future.getOrNull());
  }

  @Test
  void testExpectationOfExistingMethods() {
    var database = ApiImplementationGenerator.generateApiImplementation(
      Database.class,
      GenerationContext.forClass(BaseDatabase.class).build(),
      Mockito.mock(RPCSender.class)
    ).newInstance();

    var document = database.get(UUID.randomUUID().toString());
    Assertions.assertSame(BaseDatabase.TEST_DOCUMENT, document);

    var future = database.getAsync(UUID.randomUUID().toString());
    Assertions.assertTrue(future.isDone());
    Assertions.assertSame(BaseDatabase.TEST_DOCUMENT, future.getOrNull());
  }

  @Test
  void testDownPassingRPCSenderToExtendingClass() {
    var sender = Mockito.mock(RPCSender.class);
    var database = ApiImplementationGenerator.generateApiImplementation(
      Database.class,
      GenerationContext.forClass(SenderNeedingDatabase.class).build(),
      sender
    ).newInstance();

    Assertions.assertInstanceOf(SenderNeedingDatabase.class, database);
    Assertions.assertSame(((SenderNeedingDatabase) database).rpcSender, sender);
  }

  @Test
  void testGenerationOfMethods() {
    var testInstance = ApiImplementationGenerator.generateApiImplementation(
      TestRPCParameters.class,
      GenerationContext.forClass(TestRPCParameters.class).implementAllMethods(true).build(),
      Mockito.mock(RPCSender.class)
    ).newInstance(1, 2L, "World", new int[0], new long[]{1L}, new String[]{"Hello"});
    Assertions.assertNotNull(testInstance);
  }
}
