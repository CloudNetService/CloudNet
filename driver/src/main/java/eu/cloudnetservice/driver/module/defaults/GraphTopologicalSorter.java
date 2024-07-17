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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.graph.Graph;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A utility class to sort a graph in topological order.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class GraphTopologicalSorter {

  private GraphTopologicalSorter() {
    throw new UnsupportedOperationException();
  }

  /**
   * Sorts the given graph in topological order. That is a traversal of the graph in which each node is only visited
   * after all its predecessors and other ancestors have been visited. Note that the given graph could have multiple
   * valid topological orders and there is no guarantee made which order is returned from this method in that case.
   *
   * @param graph the graph to get the topological ordering for.
   * @return the topological order of the given graph.
   * @throws NullPointerException if the given graph is null.
   * @throws GraphCycleException  if there is a cycle in the given graph.
   */
  @Unmodifiable
  public static @NonNull List<String> sortGraph(@NonNull Graph<String> graph) {
    // resolve the initial root and non-root nodes of the given graph
    Queue<String> roots = new ArrayDeque<>();
    Multiset<String> nonRoots = HashMultiset.create();
    for (var node : graph.nodes()) {
      var incomingEdges = graph.inDegree(node);
      if (incomingEdges == 0) {
        roots.add(node);
      } else {
        nonRoots.add(node, incomingEdges);
      }
    }

    // * processes the graph in topological order by visiting root nodes and their successors
    // * nodes with no incoming edges (root nodes) are added to the result list first
    // * as each root node is processed, its successors have their incoming edge count decremented
    // * if a successor's incoming edge count reaches zero it becomes a root node
    // --> this ensures nodes are added to the result list only after all their dependencies are resolved
    String currentNode;
    ImmutableList.Builder<String> resultBuilder = ImmutableList.builder();
    while ((currentNode = roots.poll()) != null) {
      resultBuilder.add(currentNode);
      for (var successor : graph.successors(currentNode)) {
        var prevIncomingEdges = nonRoots.remove(successor, 1);
        if (prevIncomingEdges == 1) {
          // checking for 1 here as this is the previous value, which means it got
          // removed because the count went to 0 with the -1 operation we triggered
          roots.add(successor);
        }
      }
    }

    // if there are still non-roots present this means that one non-root couldn't
    // be sorted into the result list without having a dependency on another node.
    // this means there must be some cyclic node in the graph
    if (!nonRoots.isEmpty()) {
      throw GraphCycleException.INSTANCE;
    }

    return resultBuilder.build();
  }

  /**
   * An exception thrown when a cycle is detected in the graph while sorting it. This exception has no stacktrace and
   * does not contain any additional information about the exception cause.
   *
   * @since 4.0
   */
  public static final class GraphCycleException extends IllegalStateException {

    /**
     * The singleton instance of this dependency, use this when throwing instead of construction a new instance.
     */
    private static final GraphCycleException INSTANCE = new GraphCycleException();

    /**
     * Constructs a new instance of this exception, don't call directly - use the jvm-static instance instead.
     */
    private GraphCycleException() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Throwable fillInStackTrace() {
      return this;
    }
  }
}
