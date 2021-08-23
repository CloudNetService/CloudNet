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

package de.dytanic.cloudnet.driver.module;

import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class DefaultModuleProviderTest {

  @Test
  void testModuleLifecycles() {
    IModuleProvider moduleProvider = new DefaultModuleProvider();
    URL testModuleResource = DefaultModuleProviderTest.class.getClassLoader().getResource("module.jar");

    Assertions.assertNotNull(testModuleResource);

    IModuleWrapper moduleWrapper = moduleProvider.loadModule(testModuleResource);
    Assertions.assertNull(moduleProvider.loadModule(testModuleResource));

    Assertions.assertNotNull(moduleWrapper);
    Assertions.assertNotNull(System.getProperty("module_test_state"));

    Assertions.assertEquals("loaded", System.getProperty("module_test_state"));

    Assertions.assertNotNull(moduleWrapper.startModule());
    Assertions.assertEquals("started", System.getProperty("module_test_state"));

    Assertions.assertNotNull(moduleWrapper.stopModule());
    Assertions.assertEquals("stopped", System.getProperty("module_test_state"));

    Assertions.assertNotNull(moduleWrapper.unloadModule());
    Assertions.assertEquals("unloaded", System.getProperty("module_test_state"));

    Assertions.assertEquals(0, moduleProvider.getModules().size());
  }
}
