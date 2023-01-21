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
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.ApiImplementationGenerator;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RPCImplementationGeneratorTest {

  @Test
  void testFullGeneration() {
    var uuid = UUID.randomUUID();

    var immediateRpc = Mockito.mock(RPC.class);
    Mockito.when(immediateRpc.fireSync()).thenReturn(true);
    Mockito.when(immediateRpc.fire()).thenReturn(Task.completedTask(true));

    var sender = Mockito.mock(RPCSender.class);
    Mockito.when(sender.invokeMethod(Mockito.anyString(), Mockito.any())).then(invocation -> {
      String method = invocation.getArgument(0);
      var arg = invocation.getArgument(1);

      Assertions.assertTrue(method.startsWith("containsUser"));
      Assertions.assertNotNull(arg);
      Assertions.assertEquals(uuid, arg);

      return immediateRpc;
    });

    // we're using direct generation for easier testing with the sender
    var management = ApiImplementationGenerator.generateApiImplementation(
      PermissionManagement.class,
      GenerationContext.forClass(PermissionManagement.class).implementAllMethods(true).build(),
      sender
    ).newInstance();

    var result = management.containsUser(uuid);
    Assertions.assertTrue(result);

    var future = management.containsUserAsync(uuid);
    Assertions.assertTrue(future.isDone());
    Assertions.assertTrue(future.getOrNull());
  }

  @Test
  void testExpectationOfExistingMethods() {
    var management = (PermissionManagement) ApiImplementationGenerator.generateApiImplementation(
      PermissionManagement.class,
      GenerationContext.forClass(BasePermissionManagement.class).build(),
      Mockito.mock(RPCSender.class)
    ).newInstance();

    var user = management.user(UUID.randomUUID());
    Assertions.assertSame(BasePermissionManagement.VAL, user);

    var future = management.userAsync(UUID.randomUUID());
    Assertions.assertTrue(future.isDone());
    Assertions.assertSame(BasePermissionManagement.VAL, future.getOrNull());
  }

  @Test
  void testDownPassingRPCSenderToExtendingClass() {
    var sender = Mockito.mock(RPCSender.class);
    var management = ApiImplementationGenerator.generateApiImplementation(
      PermissionManagement.class,
      GenerationContext.forClass(SenderNeedingManagement.class).build(),
      sender
    ).newInstance();

    Assertions.assertInstanceOf(SenderNeedingManagement.class, management);
    Assertions.assertSame(((SenderNeedingManagement) management).sender, sender);
  }
}
