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

import org.junit.Assert;
import org.junit.Test;

public final class ServicesRegistryTest {

  @Test
  public void testDefaultRegistry() {
    DefaultServicesRegistry registry = new DefaultServicesRegistry();

    registry
      .registerService(A.class, "b", new B())
      .registerService(A.class, "c", new C());

    Assert.assertEquals(2, registry.getServices(A.class).size());
    Assert.assertEquals(10, registry.getService(A.class, "b").getValue());
    Assert.assertEquals(21, registry.getService(A.class, "c").getValue());

    registry.unregisterService(A.class, "b");
    Assert.assertEquals(1, registry.getServices(A.class).size());
    Assert.assertTrue(registry.containsService(A.class, "c"));
    Assert.assertFalse(registry.containsService(A.class, "b"));

    registry.unregisterAll();
    Assert.assertEquals(0, registry.getProvidedServices().size());

    B b = new B();
    registry.registerService(A.class, "b", b);

    Assert.assertEquals(1, registry.getServices(A.class).size());
    registry.unregisterAll(ServicesRegistryTest.class.getClassLoader());
    Assert.assertEquals(0, registry.getServices(A.class).size());
    registry.unregisterAll();
  }

  private interface A {

    int getValue();

  }

  private static class B implements A {

    @Override
    public int getValue() {
      return 10;
    }
  }

  private static class C implements A {

    @Override
    public int getValue() {
      return 21;
    }
  }
}
