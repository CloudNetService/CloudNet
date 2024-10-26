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

package eu.cloudnetservice.driver.impl.registry;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ServiceRegistryTest {

  @Test
  void testServiceRegisterPreconditions() {
    var registry = new DefaultServiceRegistry();
    var nameNotBlank = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> registry.registerProvider(ServiceA.class, "", new ServiceAImpl1()));
    Assertions.assertEquals("service name cannot be blank", nameNotBlank.getMessage());

    var notAnInterface = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> registry.registerProvider(String.class, "test", ""));
    Assertions.assertEquals("service type must be an interface", notAnInterface.getMessage());

    var noNoArgsConstructor = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> registry.registerConstructingProvider(ServiceA.class, "test", ServiceAImpl2.class));
    Assertions.assertEquals(
      "Service implementation must have a public no-args constructor",
      noNoArgsConstructor.getMessage());
  }

  @Test
  void testServiceUnregisterByClassLoader() {
    var registry = new DefaultServiceRegistry();
    var someClassLoader = new URLClassLoader(new URL[0]);
    var serviceAFakeInstance = (ServiceA) Proxy.newProxyInstance(
      someClassLoader,
      new Class[]{ServiceA.class},
      (proxy, method, args) -> switch (method.getName()) {
        case "hashCode" -> 123456789;
        case "equals" -> args[0] == proxy;
        case "toString" -> "FakeInstanceServiceA";
        default -> throw new IllegalArgumentException(method.getName());
      });

    var realARegistration = registry.registerProvider(ServiceA.class, "real", new ServiceAImpl1());
    var realBRegistration = registry.registerProvider(ServiceB.class, "real", new ServiceBImpl1());
    var fakeARegistration = registry.registerProvider(ServiceA.class, "fake", serviceAFakeInstance);

    var serviceTypes = registry.registeredServiceTypes();
    Assertions.assertTrue(serviceTypes.contains(ServiceA.class));
    Assertions.assertTrue(serviceTypes.contains(ServiceB.class));
    Assertions.assertTrue(realARegistration.valid());
    Assertions.assertTrue(realBRegistration.valid());
    Assertions.assertTrue(fakeARegistration.valid());

    registry.unregisterAll(someClassLoader); // only our fake instance is loaded by this class loader
    Assertions.assertNull(registry.registration(ServiceA.class, "fake"));
    Assertions.assertTrue(serviceTypes.contains(ServiceA.class));
    Assertions.assertTrue(serviceTypes.contains(ServiceB.class));
    Assertions.assertTrue(realARegistration.valid());
    Assertions.assertTrue(realBRegistration.valid());
    Assertions.assertFalse(fakeARegistration.valid());

    registry.unregisterAll(ServiceA.class.getClassLoader());
    Assertions.assertFalse(serviceTypes.contains(ServiceA.class));
    Assertions.assertFalse(serviceTypes.contains(ServiceB.class));
    Assertions.assertFalse(realARegistration.valid());
    Assertions.assertFalse(realBRegistration.valid());
    Assertions.assertFalse(fakeARegistration.valid());
  }

  @Test
  void testNonSingletonProviderReturnsNewInstance() {
    var registry = new DefaultServiceRegistry();
    registry.registerConstructingProvider(ServiceA.class, "ns", ServiceAImpl1.class);
    registry.registerProvider(ServiceA.class, "s", new ServiceAImpl1());

    var nonSingletonInstance1 = registry.instance(ServiceA.class, "ns");
    var nonSingletonInstance2 = registry.instance(ServiceA.class, "ns");
    Assertions.assertNotSame(nonSingletonInstance1, nonSingletonInstance2);
    Assertions.assertFalse(Proxy.isProxyClass(nonSingletonInstance1.getClass()));
    Assertions.assertFalse(Proxy.isProxyClass(nonSingletonInstance2.getClass()));

    var singletonInstance1 = registry.instance(ServiceA.class, "s");
    var singletonInstance2 = registry.instance(ServiceA.class, "s");
    Assertions.assertSame(singletonInstance1, singletonInstance2);
    Assertions.assertFalse(Proxy.isProxyClass(singletonInstance1.getClass()));
    Assertions.assertFalse(Proxy.isProxyClass(singletonInstance2.getClass()));
  }

  @Test
  void testDefaultRegistrationIsProxiedForNonSingletonProvider() {
    var registry = new DefaultServiceRegistry();
    var registration1 = registry.registerProvider(ServiceB.class, "1", new ServiceBImpl1());
    Assertions.assertTrue(registration1.valid());
    Assertions.assertTrue(registration1.defaultService());

    var registration2 = registry.registerProvider(ServiceB.class, "2", new ServiceBImpl2());
    Assertions.assertTrue(registration2.valid());
    Assertions.assertFalse(registration2.defaultService());

    var defaultProxy = registry.defaultInstance(ServiceB.class);
    Assertions.assertTrue(Proxy.isProxyClass(defaultProxy.getClass()));
    Assertions.assertEquals("hello", defaultProxy.world());

    registration2.markAsDefaultService();
    Assertions.assertFalse(registration1.defaultService());
    Assertions.assertTrue(registration2.defaultService());
    Assertions.assertEquals("world", defaultProxy.world());
  }

  @Test
  void testDefaultServiceAutomaticallyChangedWhenCurrentDefaultIsUnregistered() {
    var registry = new DefaultServiceRegistry();
    var registration1 = registry.registerProvider(ServiceB.class, "1", new ServiceBImpl1());
    Assertions.assertTrue(registration1.valid());
    Assertions.assertTrue(registration1.defaultService());

    var registration2 = registry.registerProvider(ServiceB.class, "2", new ServiceBImpl2());
    Assertions.assertTrue(registration2.valid());
    Assertions.assertFalse(registration2.defaultService());

    var defaultRegistration = registry.defaultRegistration(ServiceB.class);
    Assertions.assertTrue(defaultRegistration.valid());
    Assertions.assertTrue(defaultRegistration.defaultService());
    Assertions.assertEquals("1", defaultRegistration.name());

    var defaultInstance = defaultRegistration.serviceInstance();
    Assertions.assertTrue(Proxy.isProxyClass(defaultInstance.getClass()));
    Assertions.assertEquals("hello", defaultInstance.world());

    Assertions.assertTrue(registration1.unregister());
    Assertions.assertFalse(registration1.unregister());
    Assertions.assertFalse(registration1.valid());
    Assertions.assertFalse(registration1.defaultService());

    Assertions.assertTrue(registration2.valid());
    Assertions.assertTrue(registration2.defaultService());
    Assertions.assertEquals("2", defaultRegistration.name());
    Assertions.assertEquals("world", defaultInstance.world());

    Assertions.assertTrue(registration2.unregister());
    Assertions.assertFalse(registration2.unregister());
    Assertions.assertFalse(registration2.valid());
    Assertions.assertFalse(registration2.defaultService());
    Assertions.assertFalse(defaultRegistration.valid());
    Assertions.assertEquals("world", defaultInstance.world());
  }

  @Test
  void testAllValidRegistrationsCanBeRetrieved() {
    var registry = new DefaultServiceRegistry();
    var registration1 = registry.registerProvider(ServiceA.class, "1", new ServiceAImpl1());
    var registration2 = registry.registerProvider(ServiceA.class, "2", new ServiceAImpl1());
    var registration3 = registry.registerProvider(ServiceA.class, "3", new ServiceAImpl2("world"));

    var registrations = registry.registrations(ServiceA.class);
    Assertions.assertEquals(3, registrations.size());
    Assertions.assertTrue(registrations.contains(registration1));
    Assertions.assertTrue(registrations.contains(registration2));
    Assertions.assertTrue(registrations.contains(registration3));

    Assertions.assertTrue(registration2.unregister());
    Assertions.assertEquals(2, registrations.size());
    Assertions.assertTrue(registrations.contains(registration1));
    Assertions.assertFalse(registrations.contains(registration2));
    Assertions.assertTrue(registrations.contains(registration3));

    Assertions.assertTrue(registration3.unregister());
    Assertions.assertEquals(1, registrations.size());
    Assertions.assertTrue(registrations.contains(registration1));
    Assertions.assertFalse(registrations.contains(registration2));
    Assertions.assertFalse(registrations.contains(registration3));
  }

  @Test
  void testAutoServiceDiscoveryAndRegistration() {
    var registry = new DefaultServiceRegistry();
    registry.discoverServices(DefaultServiceRegistry.class);

    var registeredServices = registry.registeredServiceTypes();
    Assertions.assertTrue(registeredServices.contains(DataBufFactory.class));
    Assertions.assertTrue(registeredServices.contains(DocumentFactory.class));
  }

  @Test
  void testInjectionOfServices() {
    var bootLayer = InjectionLayer.boot(); // configures the @Service annotation
    bootLayer.installAutoConfigureBindings(ServiceRegistryTest.class.getClassLoader(), "driver");

    var registry = bootLayer.instance(ServiceRegistry.class);
    registry.registerProvider(ServiceA.class, "1", new ServiceAImpl1());
    registry.registerProvider(ServiceB.class, "1", new ServiceBImpl1());
    registry.registerProvider(ServiceB.class, "test", new ServiceBImpl2());

    var instance = bootLayer.instance(SomeClassThatNeedServices.class);
    Assertions.assertEquals("hello", instance.serBDef().world());
    Assertions.assertEquals("world", instance.serBTest().world());
  }

  public interface ServiceA {

  }

  public interface ServiceB {

    String world();
  }

  public static class ServiceAImpl1 implements ServiceA {

  }

  public static class ServiceAImpl2 implements ServiceA {

    public ServiceAImpl2(String world) {
    }
  }

  public static class ServiceBImpl1 implements ServiceB {

    @Override
    public String world() {
      return "hello";
    }
  }

  public static class ServiceBImpl2 implements ServiceB {

    @Override
    public String world() {
      return "world";
    }
  }

  public record SomeClassThatNeedServices(@Service ServiceB serBDef, @Service(name = "test") ServiceB serBTest) {

  }
}
