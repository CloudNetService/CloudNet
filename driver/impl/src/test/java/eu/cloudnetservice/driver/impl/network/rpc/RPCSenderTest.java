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

package eu.cloudnetservice.driver.impl.network.rpc;

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.IntSummaryStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@EnableServicesInject
public class RPCSenderTest {

  @Test
  void testRPCSenderMethodRetrieval() {
    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var sender = factory.newRPCSenderBuilder(IntSummaryStatistics.class).targetChannel(() -> null).build();

    // method calls without method descriptor
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("help"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("help", 12));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("accept"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("accept", 23L, "world"));
    Assertions.assertDoesNotThrow(() -> sender.invokeMethod("accept", 12));

    // method calls with method descriptor
    var wrongMethodDescriptor = MethodTypeDesc.of(ConstantDescs.CD_void);
    var correctMethodDescriptor = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int);
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("help", wrongMethodDescriptor));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("accept", wrongMethodDescriptor));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sender.invokeMethod("help", correctMethodDescriptor, 12));
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sender.invokeMethod("accept", correctMethodDescriptor, 12, "world"));
    Assertions.assertDoesNotThrow(() -> sender.invokeMethod("accept", correctMethodDescriptor, 12));

    // @formatter:off
    // CHECKSTYLE.OFF: good enough for a test
    var testInstance = new Object() {
      void add(int i) { sender.invokeCaller(i); }
      void accept(String s) { sender.invokeCaller(s); }
      void accept(int i) { acceptImpl(i); }
      double getAverage() { sender.invokeCaller(); return 0D; }
      private void acceptImpl(int i) { acceptImplImpl(i); }
      private void acceptImplImpl(int i) { sender.invokeCallerWithOffset(2, i); }
    };
    // CHECKSTYLE.ON
    // @formatter:on

    Assertions.assertThrows(IllegalArgumentException.class, () -> testInstance.add(12));
    Assertions.assertThrows(IllegalArgumentException.class, () -> testInstance.accept("world"));
    Assertions.assertDoesNotThrow(() -> testInstance.accept(12));
    Assertions.assertDoesNotThrow(testInstance::getAverage);
  }

  @Test
  void testRPCSenderMethodExclusion() {
    var mtSum = MethodTypeDesc.of(ConstantDescs.CD_long);
    var mtCount = MethodTypeDesc.of(ConstantDescs.CD_long);
    var mtAverage = MethodTypeDesc.of(ConstantDescs.CD_double);
    var mtAccept = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int);

    var factory = new DefaultRPCFactory(DefaultObjectMapper.DEFAULT_MAPPER, DataBufFactory.defaultFactory());
    var sender = factory.newRPCSenderBuilder(IntSummaryStatistics.class)
      .targetChannel(() -> null)
      .excludeMethod("getSum", mtSum)
      .excludeMethod("accept", mtAccept)
      .build();

    Assertions.assertDoesNotThrow(() -> sender.invokeMethod("getCount", mtCount));
    Assertions.assertDoesNotThrow(() -> sender.invokeMethod("getAverage", mtAverage));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("getSum", mtSum));
    Assertions.assertThrows(IllegalArgumentException.class, () -> sender.invokeMethod("accept", mtAccept, 12));
  }
}
