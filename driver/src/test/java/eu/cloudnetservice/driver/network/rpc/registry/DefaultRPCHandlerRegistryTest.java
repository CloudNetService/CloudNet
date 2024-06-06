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

package eu.cloudnetservice.driver.network.rpc.registry;

import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCFactory;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.network.rpc.handler.DefaultRPCHandlerTest;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultRPCHandlerRegistryTest {

  @Test
  @Order(0)
  void testRegisterHandler() {
    var factory = this.provideFactory();
    RPCHandlerRegistry registry = new DefaultRPCHandlerRegistry();

    registry.registerHandler(factory.newHandler(
      DefaultRPCHandlerTest.TestApiClass.class,
      new DefaultRPCHandlerTest.TestApiClass(new AtomicLong())));

    Assertions.assertEquals(1, registry.registeredHandlers().size());
    Assertions.assertNotNull(
      registry.registeredHandlers().get(DefaultRPCHandlerTest.TestApiClass.class.getCanonicalName()));

    Assertions.assertTrue(registry.hasHandler(DefaultRPCHandlerTest.TestApiClass.class));
    Assertions.assertTrue(registry.hasHandler(DefaultRPCHandlerTest.TestApiClass.class.getCanonicalName()));

    Assertions.assertNotNull(registry.handler(DefaultRPCHandlerTest.TestApiClass.class));
    Assertions.assertNotNull(registry.handler(DefaultRPCHandlerTest.TestApiClass.class.getCanonicalName()));
  }

  @Test
  @Order(10)
  void testUnregisterHandler() {
    var factory = this.provideFactory();
    RPCHandlerRegistry registry = new DefaultRPCHandlerRegistry();

    registry.registerHandler(factory.newHandler(
      DefaultRPCHandlerTest.TestApiClass.class,
      new DefaultRPCHandlerTest.TestApiClass(new AtomicLong())));

    Assertions.assertEquals(1, registry.registeredHandlers().size());
    Assertions.assertNotNull(
      registry.registeredHandlers().get(DefaultRPCHandlerTest.TestApiClass.class.getCanonicalName()));

    registry.unregisterHandler(DefaultRPCHandlerTest.TestApiClass.class);

    Assertions.assertEquals(0, registry.registeredHandlers().size());
    Assertions.assertNull(
      registry.registeredHandlers().get(DefaultRPCHandlerTest.TestApiClass.class.getCanonicalName()));
  }

  private RPCFactory provideFactory() {
    return new DefaultRPCFactory(new DefaultObjectMapper(), DataBufFactory.defaultFactory());
  }
}
