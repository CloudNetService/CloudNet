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

package eu.cloudnetservice.driver.impl.network.rpc.registry;

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCFactory;
import eu.cloudnetservice.driver.impl.network.rpc.handler.DefaultRPCHandlerRegistry;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import java.util.IntSummaryStatistics;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@EnableServicesInject
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultRPCHandlerRegistryTest {

  @ParameterizedTest
  @ValueSource(classes = {IntSummaryStatistics.class, Math.class})
  void testBasicRegisterAndUnregister(Class<?> targetClass) {
    var registry = new DefaultRPCHandlerRegistry();
    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());

    Assertions.assertFalse(registry.hasHandler(targetClass));
    Assertions.assertFalse(registry.hasHandler(targetClass.getName()));
    Assertions.assertNull(registry.handler(targetClass));
    Assertions.assertNull(registry.handler(targetClass.getName()));

    var handler = factory.newRPCHandlerBuilder(targetClass).build();
    Assertions.assertTrue(registry.registerHandler(handler));
    Assertions.assertTrue(registry.hasHandler(targetClass));
    Assertions.assertTrue(registry.hasHandler(targetClass.getName()));
    Assertions.assertNotNull(registry.handler(targetClass));
    Assertions.assertNotNull(registry.handler(targetClass.getName()));

    Assertions.assertTrue(registry.unregisterHandler(targetClass));
    Assertions.assertFalse(registry.hasHandler(targetClass));
    Assertions.assertNull(registry.handler(targetClass));
  }

  @Test
  void testRegisterMultipleHandlers() {
    var registry = new DefaultRPCHandlerRegistry();
    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());

    var handlerMath = factory.newRPCHandlerBuilder(Math.class).build();
    var handlerSummary = factory.newRPCHandlerBuilder(IntSummaryStatistics.class).build();

    Assertions.assertTrue(registry.registerHandler(handlerMath));
    Assertions.assertTrue(registry.registerHandler(handlerSummary));
    Assertions.assertTrue(registry.hasHandler(Math.class));
    Assertions.assertTrue(registry.hasHandler(IntSummaryStatistics.class));
    Assertions.assertSame(handlerMath, registry.handler(Math.class));
    Assertions.assertSame(handlerSummary, registry.handler(IntSummaryStatistics.class));

    Assertions.assertTrue(registry.unregisterHandler(Math.class));
    Assertions.assertFalse(registry.hasHandler(Math.class));
    Assertions.assertTrue(registry.registerHandler(handlerMath));
    Assertions.assertFalse(registry.registerHandler(handlerMath));

    Assertions.assertTrue(registry.unregisterHandler(handlerMath));
    Assertions.assertFalse(registry.hasHandler(Math.class));
    Assertions.assertTrue(registry.registerHandler(handlerMath));

    Assertions.assertTrue(registry.unregisterHandler("java.lang.Math"));
    Assertions.assertFalse(registry.hasHandler(Math.class));
    Assertions.assertTrue(registry.registerHandler(handlerMath));

    var handlerPattern = factory.newRPCHandlerBuilder(Pattern.class).build();
    Assertions.assertFalse(registry.unregisterHandler(handlerPattern));
    Assertions.assertFalse(registry.unregisterHandler(Pattern.class));
    Assertions.assertFalse(registry.unregisterHandler("java.util.regex.Pattern"));

    registry.unregisterHandlers(handlerMath.getClass().getClassLoader());
    Assertions.assertFalse(registry.hasHandler(Math.class));
    Assertions.assertFalse(registry.hasHandler(IntSummaryStatistics.class));
  }
}
