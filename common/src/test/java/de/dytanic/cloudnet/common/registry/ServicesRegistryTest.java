/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ServicesRegistryTest {

  @Test
  public void testDefaultRegistry() {
    ServicesRegistry registry = new DefaultServicesRegistry();

    registry
      .registerService(A.class, "b", new B())
      .registerService(A.class, "c", new C());

    Assertions.assertEquals(2, registry.services(A.class).size());
    Assertions.assertEquals(10, registry.service(A.class, "b").value());
    Assertions.assertEquals(21, registry.service(A.class, "c").value());

    registry.unregisterService(A.class, "b");

    Assertions.assertEquals(1, registry.services(A.class).size());
    Assertions.assertTrue(registry.containsService(A.class, "c"));
    Assertions.assertFalse(registry.containsService(A.class, "b"));

    registry.unregisterAll();

    Assertions.assertEquals(0, registry.providedServices().size());

    var b = new B();
    registry.registerService(A.class, "b", b);

    Assertions.assertEquals(1, registry.services(A.class).size());
    registry.unregisterAll(ServicesRegistryTest.class.getClassLoader());

    Assertions.assertEquals(0, registry.services(A.class).size());
    registry.unregisterAll();
  }

  private interface A {

    int value();
  }

  private static class B implements A {

    @Override
    public int value() {
      return 10;
    }
  }

  private static class C implements A {

    @Override
    public int value() {
      return 21;
    }
  }
}
