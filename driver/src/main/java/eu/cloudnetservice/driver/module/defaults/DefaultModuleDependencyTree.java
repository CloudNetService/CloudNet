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

package eu.cloudnetservice.driver.module.defaults;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import eu.cloudnetservice.driver.module.ModuleDependencyTree;
import eu.cloudnetservice.driver.module.metadata.ModuleMetadata;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of a module dependency tree.
 *
 * @since 4.0
 */
public final class DefaultModuleDependencyTree implements ModuleDependencyTree {

  private final MutableGraph<String> dependencyGraph = GraphBuilder.directed().build();

  /**
   * Adds the given module and its dependencies into this module tree.
   *
   * @param metadata the metadata of the module to register in this dependency tree.
   * @throws NullPointerException if the given module metadata is null.
   */
  public void registerModule(@NonNull ModuleMetadata metadata) {
    // add a node for the module itself & edges for all dependencies
    this.dependencyGraph.addNode(metadata.id());
    for (var moduleDependency : metadata.moduleDependencies()) {
      this.dependencyGraph.putEdge(metadata.id(), moduleDependency.id());
    }
  }

  /**
   * Removes the module from this dependency tree in case it would not cause any other module to have an unfulfilled
   * dependency. If there are still other modules depending on the module, this method returns the unfulfilled module
   * dependencies this action would cause.
   *
   * @param metadata the module metadata of the module to remove from this tree.
   * @return a set containing the unfulfilled module dependencies which would be caused by this action.
   * @throws NullPointerException if the given module metadata is null.
   */
  public @NonNull Set<String> unregisterModule(@NonNull ModuleMetadata metadata) {
    // before unregistering check if the module has other modules depending on it that would
    // become unfulfilled when just ripping out the module from the tree
    if (this.dependencyGraph.nodes().contains(metadata.id())) {
      var predecessors = this.dependencyGraph.predecessors(metadata.id());
      if (!predecessors.isEmpty()) {
        return predecessors;
      }
    }

    this.forceUnregisterModule(metadata);
    return Set.of();
  }

  /**
   * Forcibly removes the module from this dependency tree, ignoring the fact that there might be unfulfilled dependency
   * as the result of that action.
   *
   * @param metadata the module metadata of the module to remove from this tree.
   * @throws NullPointerException if the given module metadata is null.
   */
  public void forceUnregisterModule(@NonNull ModuleMetadata metadata) {
    // remove the module and it's dependency edges from the graph
    this.dependencyGraph.removeNode(metadata.id());
    for (var moduleDependency : metadata.moduleDependencies()) {
      this.dependencyGraph.removeEdge(metadata.id(), moduleDependency.id());
    }
  }

  /**
   * Tries to find the first cycle in the current dependency graph. This method returns an empty list if no cyclic
   * dependency is present in the graph. If there is cycle in the graph the returned list contains the nodes that cause
   * the cycle.
   *
   * @return an empty list if this dependency tree has no cycle, a list with the cycling dependencies otherwise.
   */
  @Unmodifiable
  public @NonNull List<String> detectFirstDependencyCycle() {
    return GraphCycleFinder.detectFirstCycle(this.dependencyGraph);
  }

  /**
   * Calculates the load order of modules based on this dependency tree. This ensures that dependency modules are in the
   * list before the actual module depending on them. Note: this method should only be called if
   * {@link #detectFirstDependencyCycle()} returned no result. If a cyclic dependency is detected while sorting this
   * tree an exception is thrown.
   *
   * @return this dependency tree in sorted order for module loading.
   * @throws IllegalStateException if this dependency tree contains a cycle.
   */
  @Unmodifiable
  public @NonNull List<String> calculateLoadOrder() {
    try {
      return GraphTopologicalSorter.sortGraph(this.dependencyGraph).reversed();
    } catch (GraphTopologicalSorter.GraphCycleException _) {
      throw new IllegalStateException("detected cycle in dependency tree, this must be checked before");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean directlyDependingOn(@NonNull ModuleMetadata owner, @NonNull ModuleMetadata other) {
    if (this.dependencyGraph.nodes().contains(owner.id())) {
      var successors = this.dependencyGraph.successors(owner.id());
      return successors.contains(other.id());
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean transitiveDependingOn(@NonNull ModuleMetadata owner, @NonNull ModuleMetadata other) {
    if (this.dependencyGraph.nodes().contains(owner.id())) {
      var reachableNodes = Graphs.reachableNodes(this.dependencyGraph, owner.id());
      return reachableNodes.contains(other.id());
    }

    return false;
  }
}
