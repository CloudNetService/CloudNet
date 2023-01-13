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

package eu.cloudnetservice.driver.network.rpc.generation.chain;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCFactory;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RPCChainImplementationGeneratorTest {

  @Test
  public void testGeneration() {
    var rpcChain = Mockito.mock(RPCChain.class);
    Mockito.when(rpcChain.fireSync(Mockito.any())).thenAnswer(invocation -> {
      Assertions.assertNull(invocation.getArgument(0));
      return "Hello World!";
    });

    var rpc = Mockito.mock(RPC.class);
    Mockito.when(rpc.join(Mockito.any())).thenAnswer(invocation -> {
      RPC argument = invocation.getArgument(0);

      Assertions.assertEquals("bar", argument.methodName());
      Assertions.assertEquals(String.class, argument.expectedResultType());

      Assertions.assertEquals(2, argument.arguments().length);
      Assertions.assertEquals("123", argument.arguments()[0]);
      Assertions.assertEquals(456, argument.arguments()[1]);

      return rpcChain;
    });

    var sender = Mockito.mock(RPCSender.class);
    Mockito.when(sender.invokeMethod(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
      String method = invocation.getArgument(0);
      String arg = invocation.getArgument(1);

      Assertions.assertEquals("chrome", method);
      Assertions.assertEquals("world!", arg);

      return rpc;
    });

    var factory = new DefaultRPCFactory(new DefaultObjectMapper(), DataBufFactory.defaultFactory());
    var generatedImpl = factory.generateRPCChainBasedApi(
      sender,
      "chrome",
      Test123.class,
      GenerationContext.forClass(Test123.class).channelSupplier(() -> null).build()
    ).newInstance(new Object[]{"test", 123, "xdd"}, new Object[]{"world!"});

    Assertions.assertEquals("test", generatedImpl.name);
    Assertions.assertEquals(123, generatedImpl.google);
    Assertions.assertEquals("xdd", generatedImpl.test);

    Assertions.assertEquals("Hello World!", generatedImpl.bar("123", 456));
  }

  public abstract static class Test123 {

    private final String name;
    private final int google;
    private final String test;

    public Test123(String name, int google, String test) {
      this.name = name;
      this.google = google;
      this.test = test;
    }

    public abstract String bar(String a, int b);

    public abstract Task<String> foo();
  }
}
