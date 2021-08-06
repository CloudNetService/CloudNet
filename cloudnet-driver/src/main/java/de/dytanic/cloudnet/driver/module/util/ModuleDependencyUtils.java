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

package de.dytanic.cloudnet.driver.module.util;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import de.dytanic.cloudnet.driver.module.ModuleDependencyNotFoundException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Utility to find dependencies of a module, ensure that they are loaded and that there is no circular dependency.
 */
public final class ModuleDependencyUtils {

  private ModuleDependencyUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Collects all first-level module dependencies of a module.
   *
   * @param caller         the module that needs its dependencies collected.
   * @param moduleProvider the provider which loaded the calling module.
   * @return a set of all dependencies which need to be enabled before the caller can be enabled.
   * @throws ModuleDependencyNotFoundException if a module dependency is missing.
   */
  public static @NotNull Set<IModuleWrapper> collectDependencies(
    @NotNull IModuleWrapper caller,
    @NotNull IModuleProvider moduleProvider
  ) {
    // we create a queue of all wrappers we already visited and use the calling module as the head of it
    Deque<IModuleWrapper> visitedNodes = new ArrayDeque<>();
    Set<IModuleWrapper> rootDependencyNodes = new HashSet<>();
    visitedNodes.add(caller);
    // we iterate over the root layer here to collect the first layer of dependencies of the module
    for (ModuleDependency dependingModule : caller.getDependingModules()) {
      IModuleWrapper wrapper = getAssociatedModuleWrapper(dependingModule, moduleProvider, caller);
      // now we visit every dependency of the module giving in a new tree to build
      visitedNodes.add(wrapper);
      visitDependencies(visitedNodes, wrapper.getDependingModules(), caller, wrapper, moduleProvider);
    }
    // now we have all dependencies collected and can return - no module depends on the caller module
    return rootDependencyNodes;
  }

  /**
   * Visits all dependencies of a module and ensures that there is no circular dependency to the root calling module.
   *
   * @param visitedNodes     all yet visited modules.
   * @param dependencies     all dependencies to check.
   * @param originalSource   the original caller which requested the check-
   * @param dependencyHolder the module of which the dependencies are checked currently.
   * @param moduleProvider   the module provider which originally loaded the dependencies.
   * @throws ModuleDependencyNotFoundException if a module dependency is missing.
   */
  private static void visitDependencies(
    @NotNull Deque<IModuleWrapper> visitedNodes,
    @NotNull Collection<ModuleDependency> dependencies,
    @NotNull IModuleWrapper originalSource,
    @NotNull IModuleWrapper dependencyHolder,
    @NotNull IModuleProvider moduleProvider
  ) {
    for (ModuleDependency dependency : dependencies) {
      IModuleWrapper wrapper = getAssociatedModuleWrapper(dependency, moduleProvider, dependencyHolder);
      // now verify that there is no circular dependency to the original caller
      Preconditions.checkArgument(
        !wrapper.getModule().getName().equals(originalSource.getModule().getName()),
        "Circular dependency detected: %s depends on caller module %s defined by %s",
        wrapper.getModule().getName(), originalSource.getModule().getName(), dependencyHolder.getModule().getName());
      // push the wrapper to the visited modules stack and visit all dependencies of that module if we haven't seen it yet
      // if this module contains a circular dependency, we will find that out when the module gets loaded
      if (visitedNodes.add(wrapper)) {
        visitDependencies(visitedNodes, wrapper.getDependingModules(), originalSource, wrapper, moduleProvider);
      }
    }
  }

  /**
   * Gets the associated module wrapper to a module dependency or throws an exception if the wrapper is not present.
   *
   * @param dependency       the dependency to get the associated wrapper with.
   * @param provider         the module provider from which the requesting module came.
   * @param dependencyHolder the holder which needs the dependency to be present.
   * @return the associated module wrapper with the given module dependency.
   * @throws ModuleDependencyNotFoundException if a module dependency is missing.
   */
  private static @NotNull IModuleWrapper getAssociatedModuleWrapper(
    @NotNull ModuleDependency dependency,
    @NotNull IModuleProvider provider,
    @NotNull IModuleWrapper dependencyHolder
  ) {
    IModuleWrapper wrapper = provider.getModule(dependency.getName());
    // ensure that the wrapper is present
    if (wrapper == null) {
      throw new ModuleDependencyNotFoundException(String.format("Missing module dependency %s from %s",
        dependency.getName(), dependencyHolder.getModule().getName()));
    }
    // todo: ensure that the version of the module is compatible by doing the following
    // if major != dependency.major = exception
    // if minor < dependency.minor = exception
    // for patch do nothing - these versions shouldn't contain new methods nor removed ones
    return wrapper;
  }
}
