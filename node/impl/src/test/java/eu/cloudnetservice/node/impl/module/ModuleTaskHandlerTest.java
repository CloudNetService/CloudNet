/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.module.ModuleState;
import eu.cloudnetservice.node.module.ModuleTask;
import jakarta.inject.Named;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class ModuleTaskHandlerTest {

  @AfterEach
  void resetStaticCallCounter() {
    ModuleMainSub.staticCallCounter = 0;
  }

  @Test
  void testDoesNotFindModuleTasksInClassWithoutModuleTasks() {
    var taskHandler = new ModuleTaskHandler();
    taskHandler.discoverModuleTasks(String.class);
    Assertions.assertNotNull(taskHandler.tasksByState);
    Assertions.assertTrue(taskHandler.tasksByState.isEmpty());
  }

  @Test
  void testModuleTasksCannotBeResolvedTwice() {
    var taskHandler = new ModuleTaskHandler();
    taskHandler.discoverModuleTasks(String.class);
    Assertions.assertThrows(IllegalStateException.class, () -> taskHandler.discoverModuleTasks(String.class));
  }

  @Test
  void testModuleTasksCanBeDiscoveredAgainAfterReset() {
    var taskHandler = new ModuleTaskHandler();
    taskHandler.discoverModuleTasks(String.class);
    Assertions.assertNotNull(taskHandler.tasksByState);
    Assertions.assertThrows(IllegalStateException.class, () -> taskHandler.discoverModuleTasks(String.class));

    Assertions.assertDoesNotThrow(taskHandler::reset);
    Assertions.assertNull(taskHandler.tasksByState);
    Assertions.assertDoesNotThrow(() -> taskHandler.discoverModuleTasks(String.class));
    Assertions.assertNotNull(taskHandler.tasksByState);
    Assertions.assertThrows(IllegalStateException.class, () -> taskHandler.discoverModuleTasks(String.class));

    Assertions.assertDoesNotThrow(taskHandler::reset);
    Assertions.assertDoesNotThrow(taskHandler::reset);
    Assertions.assertDoesNotThrow(taskHandler::reset);
    Assertions.assertDoesNotThrow(() -> taskHandler.discoverModuleTasks(String.class));
  }

  @Test
  void testDiscoverModuleTasksOnlyInConcreteClass() {
    var taskHandler = new ModuleTaskHandler();
    Assertions.assertThrows(IllegalArgumentException.class, () -> taskHandler.discoverModuleTasks(String[].class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> taskHandler.discoverModuleTasks(String[][].class));
    Assertions.assertThrows(IllegalArgumentException.class, () -> taskHandler.discoverModuleTasks(Collection.class));
    Assertions.assertNull(taskHandler.tasksByState);
  }

  @Test
  void testDiscoverModuleTasksInSingleClass() {
    var taskHandler = new ModuleTaskHandler();
    Assertions.assertDoesNotThrow(() -> taskHandler.discoverModuleTasks(ModuleMainSub.class));
    Assertions.assertNotNull(taskHandler.tasksByState);

    var loadingTasks = taskHandler.tasksByState.get(ModuleState.LOADING);
    Assertions.assertEquals(3, loadingTasks.length);
    Assertions.assertEquals("loadingOverridden", loadingTasks[0].method().getName());
    Assertions.assertEquals("onLoading1", loadingTasks[1].method().getName());
    Assertions.assertEquals("onLoading3", loadingTasks[2].method().getName());

    var reloadingTasks = taskHandler.tasksByState.get(ModuleState.RELOADING);
    Assertions.assertEquals(2, reloadingTasks.length);
    Assertions.assertEquals("onReloading1", reloadingTasks[0].method().getName());
    Assertions.assertEquals("onReloading2", reloadingTasks[1].method().getName());
  }

  @Test
  void testDiscoverModuleTasksInClassTree() {
    var taskHandler = new ModuleTaskHandler();
    Assertions.assertDoesNotThrow(() -> taskHandler.discoverModuleTasks(ModuleMain.class));
    Assertions.assertNotNull(taskHandler.tasksByState);

    var loadingTasks = taskHandler.tasksByState.get(ModuleState.LOADING);
    Assertions.assertEquals(4, loadingTasks.length);
    Assertions.assertEquals("onLoading1", loadingTasks[0].method().getName());
    Assertions.assertEquals("onLoading2", loadingTasks[1].method().getName());
    Assertions.assertEquals("onLoading3", loadingTasks[2].method().getName());
    Assertions.assertEquals("loadingLast", loadingTasks[3].method().getName());

    var reloadingTasks = taskHandler.tasksByState.get(ModuleState.RELOADING);
    Assertions.assertEquals(4, reloadingTasks.length);
    Assertions.assertEquals("unloadingOrReloading", reloadingTasks[0].method().getName());
    Assertions.assertEquals("onReloading1", reloadingTasks[1].method().getName());
    Assertions.assertEquals(ModuleMainSub.class, reloadingTasks[1].method().getDeclaringClass());
    Assertions.assertEquals("onReloading1", reloadingTasks[2].method().getName());
    Assertions.assertEquals(ModuleMain.class, reloadingTasks[2].method().getDeclaringClass());
    Assertions.assertEquals("onReloading2", reloadingTasks[3].method().getName());
    Assertions.assertEquals(ModuleMainSub.class, reloadingTasks[3].method().getDeclaringClass());

    var unloadingTasks = taskHandler.tasksByState.get(ModuleState.UNLOADING);
    Assertions.assertEquals(1, unloadingTasks.length);
    Assertions.assertEquals("unloadingOrReloading", unloadingTasks[0].method().getName());
  }

  @Test
  void testModuleTasksAreExecutedSuccessfully() {
    var injectionLayer = InjectionLayer.boot();
    var rootBindingBuilder = injectionLayer.injector().createBindingBuilder();
    injectionLayer.install(rootBindingBuilder.bind(int.class).qualifiedWithName("magic_number").toInstance(1337));
    injectionLayer.install(rootBindingBuilder.bind(String.class).qualifiedWithName("test").toInstance("hello world"));

    var taskHandler = new ModuleTaskHandler();
    taskHandler.discoverModuleTasks(ModuleMain.class);

    var instance = new ModuleMain();
    var thrown = taskHandler.invokeModuleTasks(ModuleState.LOADING, instance, injectionLayer);
    Assertions.assertNull(thrown);
    Assertions.assertEquals("loading_end", instance.state);

    instance.state = null;
    thrown = taskHandler.invokeModuleTasks(ModuleState.RELOADING, instance, injectionLayer);
    Assertions.assertNull(thrown);
    Assertions.assertEquals(4, ModuleMainSub.staticCallCounter);
    Assertions.assertEquals("unloading_reloading", instance.state);
  }

  @Test
  void testModuleTaskExceptionsAreReturnedToCaller() {
    var injectionLayer = InjectionLayer.boot();
    var rootBindingBuilder = injectionLayer.injector().createBindingBuilder();
    injectionLayer.install(rootBindingBuilder.bind(int.class).qualifiedWithName("magic_number").toInstance(1337));
    injectionLayer.install(rootBindingBuilder.bind(String.class).qualifiedWithName("test").toInstance("hello world"));

    var taskHandler = new ModuleTaskHandler();
    taskHandler.discoverModuleTasks(ModuleMain.class);

    var instance = new ModuleMain();
    instance.state = "test"; // trigger assertion fail
    var thrown = taskHandler.invokeModuleTasks(ModuleState.LOADING, instance, injectionLayer);
    Assertions.assertNotNull(thrown);
    Assertions.assertInstanceOf(RuntimeException.class, thrown);
    Assertions.assertTrue(thrown.getMessage().contains("ModuleMainSub.onLoading1()"));

    var cause = thrown.getCause();
    Assertions.assertNotNull(cause);
    Assertions.assertInstanceOf(AssertionFailedError.class, cause);
    Assertions.assertEquals("expected: <null> but was: <\"test\">", cause.getMessage());
  }

  public static class ModuleMain extends ModuleMainSub {

    @ModuleTask(states = ModuleState.RELOADING, priority = 110)
    public static void onReloading1() { // "overridden", but method is static so both will be called
      Assertions.assertEquals(1, staticCallCounter);
      staticCallCounter++;
    }

    public static void onReloading2() { // override, but a static method, so the super method should be called anyway
      Assertions.fail("ModuleMain.onReloading2() called");
    }

    @ModuleTask(states = ModuleState.LOADING, priority = 115)
    public void onLoading2() {
      Assertions.assertEquals("loading1", this.state);
      this.state = "loading2";
    }

    @ModuleTask(states = ModuleState.LOADING, priority = -128)
    public void loadingLast(@Named("test") String test, @Named("magic_number") int magicNumber) {
      Assertions.assertEquals("loading3", this.state);
      Assertions.assertEquals("hello world", test);
      Assertions.assertEquals(1337, magicNumber);
      this.state = "loading_end";
    }

    @Override
    public void loadingOverridden() { // no longer annotated with @ModuleTask
      super.loadingOverridden();
    }

    @ModuleTask(states = {ModuleState.UNLOADING, ModuleState.RELOADING}, priority = 120)
    public void unloadingOrReloading() {
      Assertions.assertNull(this.state);
      Assertions.assertEquals(0, staticCallCounter);
      this.state = "unloading_reloading";
    }
  }

  public static class ModuleMainSub {

    protected static int staticCallCounter;
    protected String state;

    @ModuleTask(states = ModuleState.RELOADING, priority = 115)
    public static void onReloading1() {
      Assertions.assertEquals(0, staticCallCounter);
      staticCallCounter++;
    }

    @ModuleTask(states = ModuleState.RELOADING, priority = 105)
    public static void onReloading2() {
      Assertions.assertEquals(2, staticCallCounter);
      staticCallCounter++;
    }

    @ModuleTask(states = ModuleState.LOADING, priority = 120)
    public void onLoading1() {
      Assertions.assertNull(this.state);
      this.state = "loading1";
    }

    @ModuleTask(states = ModuleState.LOADING, priority = 110)
    public void loading3(@Named("test") String test) {
      Assertions.assertEquals("loading2", this.state);
      Assertions.assertEquals("hello world", test);
      this.state = "loading3";
    }

    @ModuleTask(states = ModuleState.LOADING, priority = 127)
    public void loadingOverridden() {
      Assertions.fail("loadingOverridden() called");
    }
  }
}
