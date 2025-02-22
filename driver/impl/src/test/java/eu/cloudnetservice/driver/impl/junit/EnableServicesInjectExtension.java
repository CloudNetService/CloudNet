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

package eu.cloudnetservice.driver.impl.junit;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.impl.registry.DefaultServiceRegistry;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

public final class EnableServicesInjectExtension
  implements BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {

  private static final Field EXT_LAYER_FIELD;
  private static final Field BOOT_LAYER_FIELD;
  private static final Field SERVICE_REGISTRY_FIELD;

  private static final Logger LOGGER = LoggerFactory.getLogger(EnableServicesInjectExtension.class);

  static {
    try {
      // resolve holder fields of injection
      var injectProviderClass = Class.forName("eu.cloudnetservice.driver.inject.InjectionLayerProvider");
      EXT_LAYER_FIELD = injectProviderClass.getDeclaredField("ext");
      EXT_LAYER_FIELD.setAccessible(true);
      BOOT_LAYER_FIELD = injectProviderClass.getDeclaredField("boot");
      BOOT_LAYER_FIELD.setAccessible(true);

      // resolve holder field of service registry
      var serviceHolderClass = Class.forName("eu.cloudnetservice.driver.registry.ServiceRegistryHolder");
      SERVICE_REGISTRY_FIELD = serviceHolderClass.getDeclaredField("instance");
      SERVICE_REGISTRY_FIELD.setAccessible(true);
    } catch (Throwable exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  // small hack to set up the service/injection state before all tests once as parameterized tests will
  // resolve the parameters before any other callback is invoked. this means that parameter resolvers
  // are unable to use services or inject if we would set up the state in a different callback
  @Override
  public void beforeAll(ExtensionContext context) {
    this.setupInjectionAndServicesState();
    LOGGER.debug(() -> "Setup initial test injection and services state");
  }

  // technically this cleanup is also not needed, but it prevents other tests running in the same jvm
  // to accidentally forget to enable inject/services, then depend on an old state from another test
  // and possibly fail at some point due to a different test execution order
  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    this.resetInjectionAndServicesState();
    LOGGER.debug(() -> "Cleaned up test injection and services state after final test");
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    this.resetInjectionAndServicesState();
    this.setupInjectionAndServicesState();
    LOGGER.debug(() -> "Reset test injection and services state");
  }

  private void resetInjectionAndServicesState() throws Exception {
    EXT_LAYER_FIELD.set(null, null);
    BOOT_LAYER_FIELD.set(null, null);
    SERVICE_REGISTRY_FIELD.set(null, null);
  }

  private void setupInjectionAndServicesState() {
    // setup boot injection layer
    var bootInjectionLayer = InjectionLayer.boot();
    bootInjectionLayer.installAutoConfigureBindings(EnableServicesInjectExtension.class.getClassLoader(), "driver");

    // setup default services for testing
    var serviceRegistry = bootInjectionLayer.instance(ServiceRegistry.class);
    serviceRegistry.discoverServices(DefaultServiceRegistry.class);

    // setup injection stuff that depends on the services being initialized
    bootInjectionLayer.install(BindingBuilder.create()
      .bind(DriverEnvironment.class)
      .toInstance(DriverEnvironment.NODE));
  }
}
