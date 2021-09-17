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

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.module.util.ModuleDependencyUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
public class ModuleDependencyUtilsTest {

  @Test
  @Order(0)
  void testDependencyResolving() {
    Collection<IModuleWrapper> moduleWrappers = new ArrayList<>();

    IModuleProvider provider = this.mockModuleProvider(moduleWrappers);

    IModuleWrapper rootModule = this.mockRootModule(provider);
    moduleWrappers.add(rootModule);

    moduleWrappers.add(this.mockModule(provider, "sub", "1.5.0"));
    moduleWrappers.add(this.mockModule(provider, "sub2", "1.7.4"));

    Assertions.assertEquals(3, provider.getModules().size());

    Collection<IModuleWrapper> dependencies = ModuleDependencyUtils.collectDependencies(rootModule, provider);
    Assertions.assertEquals(1, dependencies.size());
    Assertions.assertEquals("sub", Iterables.get(dependencies, 0).getModule().getName());
  }

  @Test
  @Order(10)
  void testMissingDependency() {
    Collection<IModuleWrapper> moduleWrappers = new ArrayList<>();
    IModuleProvider provider = this.mockModuleProvider(moduleWrappers);

    Assertions.assertThrows(
      ModuleDependencyNotFoundException.class,
      () -> ModuleDependencyUtils.collectDependencies(this.mockRootModule(provider), provider));
  }

  @Test
  @Order(20)
  void testDependencyOutdatedDependency() {
    Collection<IModuleWrapper> moduleWrappers = new ArrayList<>();
    IModuleProvider provider = this.mockModuleProvider(moduleWrappers);

    moduleWrappers.add(this.mockModule(provider, "sub", "1.4.7"));

    Assertions.assertThrows(
      ModuleDependencyOutdatedException.class,
      () -> ModuleDependencyUtils.collectDependencies(this.mockRootModule(provider), provider));
  }

  private IModuleProvider mockModuleProvider(Collection<IModuleWrapper> wrappers) {
    IModuleProvider provider = Mockito.mock(IModuleProvider.class);
    Mockito.when(provider.getModules()).thenReturn(wrappers);
    Mockito.when(provider.getModule(Mockito.anyString())).then(invocation -> wrappers.stream()
      .filter(wrapper -> wrapper.getModule().getName().equals(invocation.getArgument(0)))
      .findFirst()
      .orElse(null));

    return provider;
  }

  private IModuleWrapper mockRootModule(IModuleProvider provider) {
    return this.mockModule(
      provider,
      "root",
      "1.0",
      wrapper -> Mockito
        .when(wrapper.getDependingModules())
        .thenReturn(Collections.singleton(new ModuleDependency("eu.cloudnet", "sub", "1.5.0"))));
  }

  private IModuleWrapper mockModule(IModuleProvider pro, String name, String version) {
    return this.mockModule(pro, name, version, $ -> {
    });
  }

  private IModuleWrapper mockModule(IModuleProvider pro, String name, String version, Consumer<IModuleWrapper> mod) {
    IModule mockedModule = Mockito.mock(IModule.class);
    Mockito.when(mockedModule.getName()).thenReturn(name);
    Mockito.when(mockedModule.getVersion()).thenReturn(version);

    IModuleWrapper moduleWrapper = Mockito.mock(IModuleWrapper.class);
    Mockito.when(moduleWrapper.getModuleProvider()).thenReturn(pro);
    Mockito.when(moduleWrapper.getModule()).thenReturn(mockedModule);

    mod.accept(moduleWrapper);

    return moduleWrapper;
  }
}
