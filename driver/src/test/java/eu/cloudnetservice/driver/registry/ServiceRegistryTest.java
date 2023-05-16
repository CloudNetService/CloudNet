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

package eu.cloudnetservice.driver.registry;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.injection.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ServiceRegistryTest {

  @Test
  public void testDefaultRegistry() {
    var registry = new DefaultServiceRegistry(InjectionLayer.boot());

    registry
      .registerProvider(A.class, "b", new B())
      .registerProvider(A.class, "c", new C());

    Assertions.assertEquals(2, registry.providers(A.class).size());
    Assertions.assertEquals(10, registry.provider(A.class, "b").value());
    Assertions.assertEquals(21, registry.provider(A.class, "c").value());

    registry.unregisterProvider(A.class, "b");

    Assertions.assertEquals(1, registry.providers(A.class).size());
    Assertions.assertTrue(registry.hasProvider(A.class, "c"));
    Assertions.assertFalse(registry.hasProvider(A.class, "b"));

    registry.unregisterAll();

    Assertions.assertEquals(0, registry.providedServices().size());

    var b = new B();
    registry.registerProvider(A.class, "b", b);

    Assertions.assertEquals(1, registry.providers(A.class).size());
    registry.unregisterAll(ServiceRegistryTest.class.getClassLoader());

    Assertions.assertEquals(0, registry.providers(A.class).size());
    registry.unregisterAll();
  }

  @Test
  public void testRegistryInjection() {
    var registry = new DefaultServiceRegistry(InjectionLayer.boot());
    var b = new B();

    registry.registerProvider(A.class, "b", b);
    registry.registerProvider(A.class, "c", new B());

    var d = InjectionLayer.boot().instance(D.class);
    Assertions.assertNotNull(d.a());

    Assertions.assertNotNull(d.b());
    Assertions.assertSame(b, d.b());

    Assertions.assertNotSame(b, d.c());

    Assertions.assertNull(d.nonExistent());
  }

  private record D(
    @Service A a,
    @Service(name = "b") A b,
    @Service(name = "c") A c,
    @Service(name = "non-existing") A nonExistent) {

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
