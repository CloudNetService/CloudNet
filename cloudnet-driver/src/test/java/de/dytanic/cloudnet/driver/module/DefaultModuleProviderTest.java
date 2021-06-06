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

import org.junit.Assert;
import org.junit.Test;

public final class DefaultModuleProviderTest {

  @Test
  public void testModule() throws Throwable {
    IModuleProvider moduleProvider = new DefaultModuleProvider();

    IModuleWrapper moduleWrapper = moduleProvider
      .loadModule(DefaultModuleProviderTest.class.getClassLoader().getResource("module.jar"));
    Assert.assertNull(
      moduleProvider.loadModule(DefaultModuleProviderTest.class.getClassLoader().getResource("module.jar")));

    Assert.assertNotNull(moduleWrapper);
    Assert.assertNotNull(System.getProperty("module_test_state"));
    Assert.assertEquals("loaded", System.getProperty("module_test_state"));

    Assert.assertNotNull(moduleWrapper.startModule());
    Assert.assertEquals("started", System.getProperty("module_test_state"));

    Assert.assertNotNull(moduleWrapper.stopModule());
    Assert.assertEquals("stopped", System.getProperty("module_test_state"));

    Assert.assertNotNull(moduleWrapper.unloadModule());
    Assert.assertEquals("unloaded", System.getProperty("module_test_state"));

    Assert.assertEquals(0, moduleProvider.getModules().size());
  }
}
