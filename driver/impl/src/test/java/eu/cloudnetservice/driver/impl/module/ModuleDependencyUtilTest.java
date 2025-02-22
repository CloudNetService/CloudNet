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

package eu.cloudnetservice.driver.impl.module;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleDependencyNotFoundException;
import eu.cloudnetservice.driver.module.ModuleDependencyOutdatedException;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleDependencyUtilTest {

  @Test
  @Order(0)
  void testDependencyResolving() {
    Collection<ModuleWrapper> moduleWrappers = new ArrayList<>();

    var provider = this.mockModuleProvider(moduleWrappers);

    var rootModule = this.mockRootModule(provider);
    moduleWrappers.add(rootModule);

    moduleWrappers.add(this.mockModule(provider, "sub", "1.5.0"));
    moduleWrappers.add(this.mockModule(provider, "sub2", "1.7.4"));

    Assertions.assertEquals(3, provider.modules().size());

    Collection<ModuleWrapper> dependencies = ModuleDependencyUtil.collectDependencies(rootModule, provider);
    Assertions.assertEquals(1, dependencies.size());
    Assertions.assertEquals("sub", Iterables.get(dependencies, 0).module().name());
  }

  @Test
  @Order(10)
  void testMissingDependency() {
    Collection<ModuleWrapper> moduleWrappers = new ArrayList<>();
    var provider = this.mockModuleProvider(moduleWrappers);

    Assertions.assertThrows(
      ModuleDependencyNotFoundException.class,
      () -> ModuleDependencyUtil.collectDependencies(this.mockRootModule(provider), provider));
  }

  @Test
  @Order(20)
  void testDependencyOutdatedDependency() {
    Collection<ModuleWrapper> moduleWrappers = new ArrayList<>();
    var provider = this.mockModuleProvider(moduleWrappers);

    moduleWrappers.add(this.mockModule(provider, "sub", "1.4.7"));

    Assertions.assertThrows(
      ModuleDependencyOutdatedException.class,
      () -> ModuleDependencyUtil.collectDependencies(this.mockRootModule(provider), provider));
  }

  private ModuleProvider mockModuleProvider(Collection<ModuleWrapper> wrappers) {
    var provider = Mockito.mock(ModuleProvider.class);
    Mockito.when(provider.modules()).thenReturn(wrappers);
    Mockito.when(provider.module(Mockito.anyString())).then(invocation -> wrappers.stream()
      .filter(wrapper -> wrapper.module().name().equals(invocation.getArgument(0)))
      .findFirst()
      .orElse(null));

    return provider;
  }

  private ModuleWrapper mockRootModule(ModuleProvider provider) {
    return this.mockModule(
      provider,
      "root",
      "1.0",
      wrapper -> Mockito
        .when(wrapper.dependingModules())
        .thenReturn(Collections.singleton(new ModuleDependency("eu.cloudnet", "sub", "1.5.0"))));
  }

  private ModuleWrapper mockModule(ModuleProvider pro, String name, String version) {
    return this.mockModule(pro, name, version, $ -> {
    });
  }

  private ModuleWrapper mockModule(ModuleProvider pro, String name, String version, Consumer<ModuleWrapper> mod) {
    var mockedModule = Mockito.mock(Module.class);
    Mockito.when(mockedModule.name()).thenReturn(name);
    Mockito.when(mockedModule.version()).thenReturn(version);

    var moduleWrapper = Mockito.mock(ModuleWrapper.class);
    Mockito.when(moduleWrapper.moduleProvider()).thenReturn(pro);
    Mockito.when(moduleWrapper.module()).thenReturn(mockedModule);

    mod.accept(moduleWrapper);

    return moduleWrapper;
  }
}
