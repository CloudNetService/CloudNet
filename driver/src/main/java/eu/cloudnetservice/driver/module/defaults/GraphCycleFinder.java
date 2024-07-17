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

import com.google.common.graph.Graph;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A very basic utility to find cycles in a graph.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class GraphCycleFinder {

  /**
   * Finds the first cycle in the given graph. If a cycle was found the returned list will be non-empty and contain the
   * cyclic nodes, in order. If the returned list is empty no cycle was detected in the given graph.
   *
   * @param graph the graph to scan for a cycle.
   * @return an empty list if the given graph has no cycle, a list with the cycling nodes otherwise.
   * @throws NullPointerException if the given graph is null.
   */
  @Unmodifiable
  public static @NonNull List<String> detectFirstCycle(@NonNull Graph<String> graph) {
    // without any edges there cannot be any cycles
    if (graph.edges().isEmpty()) {
      return List.of();
    }

    // try to find a node in the graph that has a cyclic
    // dependency to some other node in the graph
    var graphNodes = graph.nodes();
    LinkedList<String> stack = new LinkedList<>();
    Map<String, NodeVisitState> visitedNodes = HashMap.newHashMap(graphNodes.size());
    for (var node : graphNodes) {
      var cycle = visitNode(graph, node, visitedNodes, stack);
      if (!cycle.isEmpty()) {
        return cycle;
      }
    }

    return List.of();
  }

  /**
   * Visits a single node of the given graph and checks if there is any cycle. If that is the case a non-empty list will
   * be returned containing all nodes that are part of the cycle. If no cycle is found the returned list is empty.
   *
   * @param graph        the graph to search for cycles in.
   * @param node         the node that is currently being checked.
   * @param visitedNodes the state map of the nodes that were visited before.
   * @param stack        the stack of nodes that are being traversed during the current check.
   * @return an empty list if the given graph node has no cycle, a list with the cycling nodes otherwise.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private static @NonNull List<String> visitNode(
    @NonNull Graph<String> graph,
    @NonNull String node,
    @NonNull Map<String, NodeVisitState> visitedNodes,
    @NonNull LinkedList<String> stack
  ) {
    var visitState = visitedNodes.get(node);
    return switch (visitState) {
      case COMPLETE -> List.of(); // node was already visited
      case PENDING -> {
        // visiting node again while visiting the node, there must be some
        // cyclic reference on the stack. add the current node again to the
        // stack to make it clear what is the cause and return
        stack.addLast(node);
        yield stack;
      }
      case null -> {
        // node was not visited yet, mark the node as being visited and scan the edges
        // return if one scan returns a non-empty list as some cycle was detected in that case
        stack.addLast(node);
        visitedNodes.put(node, NodeVisitState.PENDING);
        for (var successor : graph.successors(node)) {
          var cycle = visitNode(graph, successor, visitedNodes, stack);
          if (!cycle.isEmpty()) {
            yield cycle;
          }
        }

        // all connected nodes were visited without finding a cycle, mark
        // the node as visited and return an empty list to indicate the result
        stack.removeLast();
        visitedNodes.put(node, NodeVisitState.COMPLETE);
        yield List.of();
      }
    };
  }

  /**
   * Represents the state of a node during the cycle detection. If a node has no state it indicates that it hasn't been
   * explored yet.
   *
   * @since 4.0
   */
  private enum NodeVisitState {
    /**
     * The node is currently being explored.
     */
    PENDING,
    /**
     * The node was fully explored already.
     */
    COMPLETE,
  }
}
