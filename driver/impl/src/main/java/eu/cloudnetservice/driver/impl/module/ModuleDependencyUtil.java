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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleDependencyNotFoundException;
import eu.cloudnetservice.driver.module.ModuleDependencyOutdatedException;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * Utility to find dependencies of a module, ensure that they are loaded and that there is no circular dependency.
 *
 * @see ModuleDependency
 * @see ModuleWrapper
 * @since 4.0
 */
public final class ModuleDependencyUtil {

  /**
   * A regex to match semver versions. See <a href="https://regex101.com/r/Gqy0sh/1">here</a> for testing.
   */
  private static final Pattern SEMVER_PATTERN = Pattern.compile(
    "(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*))?)?");

  /**
   * Creating an instance of this helper class is not allowed, results in {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException on invocation
   */
  private ModuleDependencyUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Collects all first-level module dependencies of a module.
   *
   * @param caller         the module that needs its dependencies collected.
   * @param moduleProvider the provider which loaded the calling module.
   * @return a set of all dependencies which need to be enabled before the caller can be enabled.
   * @throws ModuleDependencyNotFoundException if a module dependency is missing.
   * @throws ModuleDependencyOutdatedException if the running module can not provide the minimum required version.
   * @throws NullPointerException              if caller or moduleProvider is null.
   */
  public static @NonNull Set<ModuleWrapper> collectDependencies(
    @NonNull ModuleWrapper caller,
    @NonNull ModuleProvider moduleProvider
  ) {
    // we create a queue of all wrappers we already visited and use the calling module as the head of it
    Deque<ModuleWrapper> visitedNodes = new ArrayDeque<>();
    Set<ModuleWrapper> rootDependencyNodes = new HashSet<>();
    visitedNodes.add(caller);
    // we iterate over the root layer here to collect the first layer of dependencies of the module
    for (var dependingModule : caller.dependingModules()) {
      var wrapper = associatedModuleWrapper(dependingModule, moduleProvider, caller);
      // register the module as a root dependency of the calling module
      rootDependencyNodes.add(wrapper);
      // now we visit every dependency of the module giving in a new tree to build
      visitedNodes.add(wrapper);
      visitDependencies(visitedNodes, wrapper.dependingModules(), caller, wrapper, moduleProvider);
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
   * @throws ModuleDependencyOutdatedException if the running module can not provide the minimum required version.
   * @throws NullPointerException              if visitedNodes, dependencies, originalSource, dependencyHolder or
   *                                           moduleProvider is null.
   */
  private static void visitDependencies(
    @NonNull Deque<ModuleWrapper> visitedNodes,
    @NonNull Collection<ModuleDependency> dependencies,
    @NonNull ModuleWrapper originalSource,
    @NonNull ModuleWrapper dependencyHolder,
    @NonNull ModuleProvider moduleProvider
  ) {
    for (var dependency : dependencies) {
      var wrapper = associatedModuleWrapper(dependency, moduleProvider, dependencyHolder);
      // now verify that there is no circular dependency to the original caller
      Preconditions.checkArgument(
        !wrapper.module().name().equals(originalSource.module().name()),
        "Circular dependency detected: %s depends on caller module %s defined by %s",
        wrapper.module().name(), originalSource.module().name(), dependencyHolder.module().name());
      // push the wrapper to the visited modules stack and visit all dependencies of that module if we haven't seen it yet
      // if this module contains a circular dependency, we will find that out when the module gets loaded
      if (visitedNodes.add(wrapper)) {
        visitDependencies(visitedNodes, wrapper.dependingModules(), originalSource, wrapper, moduleProvider);
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
   * @throws ModuleDependencyOutdatedException if the running module can not provide the minimum required version.
   * @throws NullPointerException              if dependency, provider or dependencyHolder is null.
   */
  private static @NonNull ModuleWrapper associatedModuleWrapper(
    @NonNull ModuleDependency dependency,
    @NonNull ModuleProvider provider,
    @NonNull ModuleWrapper dependencyHolder
  ) {
    var wrapper = provider.module(dependency.name());
    // ensure that the wrapper is present
    if (wrapper == null) {
      throw new ModuleDependencyNotFoundException(dependency.name(), dependencyHolder.module().name());
    }
    // try to make a semver check
    var dependencyVersion = SEMVER_PATTERN.matcher(dependency.version());
    var moduleVersion = SEMVER_PATTERN.matcher(wrapper.module().version());
    // check if both of the matchers had at least one match
    if (dependencyVersion.matches() && moduleVersion.matches()) {
      // assert that the versions are compatible
      checkDependencyVersion(dependencyHolder, dependency, dependencyVersion, moduleVersion);
    }
    return wrapper;
  }

  /**
   * Validates that the dependency of the module is compatible with the actual running module. This only checks for the
   * major and minor version of the module as a patch version should only patch bugs not add new features nor contain
   * breaking changes.
   *
   * @param requiringModule   the module which holds the dependency.
   * @param dependency        the dependency which gets checked.
   * @param dependencyVersion a matcher for the semver versioning of the dependency.
   * @param moduleVersion     a matcher for the semver versioning of the running module.
   * @throws ModuleDependencyOutdatedException if the running module can not provide the minimum required version.
   * @throws NullPointerException              if requiringModule, dependency, dependencyVersion or moduleVersion is
   *                                           null.
   */
  private static void checkDependencyVersion(
    @NonNull ModuleWrapper requiringModule,
    @NonNull ModuleDependency dependency,
    @NonNull Matcher dependencyVersion,
    @NonNull Matcher moduleVersion
  ) {
    // extract both major versions
    var moduleMajor = Integer.parseInt(moduleVersion.group(1));
    var dependencyMajor = Integer.parseInt(dependencyVersion.group(1));
    // fail if the dependency major is not the actual major as breaking changes may be introduced
    if (dependencyMajor != moduleMajor) {
      throw new ModuleDependencyOutdatedException(requiringModule, dependency, "major", dependencyMajor, moduleMajor);
    }
    // check if a minor version is required
    if (dependencyVersion.groupCount() > 1) {
      // extract both minor versions
      var dependencyMinor = Integer.parseInt(dependencyVersion.group(2));
      var moduleMinor = moduleVersion.groupCount() == 1 ? 0 : Integer.parseInt(moduleVersion.group(2));
      // fail if the dependency minor is higher than the actual major
      if (dependencyMinor > moduleMinor) {
        throw new ModuleDependencyOutdatedException(requiringModule, dependency, "minor", dependencyMinor, moduleMinor);
      }
    }
  }
}
