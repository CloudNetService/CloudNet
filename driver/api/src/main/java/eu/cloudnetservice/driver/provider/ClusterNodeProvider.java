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

package eu.cloudnetservice.driver.provider;

import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.command.CommandInfo;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The main api point for accessing node information of all nodes which are currently connected to the current node. As
 * per the CloudNet cluster contract, all nodes need to know all information about each other node without a node in the
 * middle. This means that, for example, Node-1 must be connected to Node-3 to know all information, this will not work
 * if Node-1 is only connected to Node-2, and Node-2 is then connected to Node-3.
 * <p>
 * Note: Sometimes the documentation of methods in this class refers to the current node - that is either the current
 * component or the node component to which the current wrapped is connected (if running remotely).
 *
 * @since 4.0
 */
public interface ClusterNodeProvider {

  /**
   * Get all commands which are registered on the current node. Additions and removals to the returned collection are
   * not possible and will not have any effect. There is no way to register a command from this provider because remote
   * commands are not supported. You need to use the node command provider to register a command on the current node.
   *
   * @return all registered commands.
   */
  @UnmodifiableView
  @NonNull
  Collection<CommandInfo> consoleCommands();

  /**
   * Get an information about a specific registered command on the current node. This method returns null if no command
   * with the given name is present on the current node.
   *
   * @param name the name of the command to get the information of.
   * @return the command info of the command with the given name or null if no such command exists.
   * @throws NullPointerException if the given command name is null.
   */
  @Nullable CommandInfo consoleCommand(@NonNull String name);

  /**
   * Gets all tab complete results for the next argument in the provided command line. An empty command line will list
   * all root command names. This method resolves all tab completion results which are allowed to be executed via a
   * console command source. This does not mean that the api is actually able to execute the command. For example, if
   * the input is {@code tasks} one suggestion might be {@code setup} (which will start the task console setup
   * animation), the api is not allowed to execute that command but the result will be listed anyway.
   *
   * @param commandLine the current command line input to start the tab completion based on.
   * @return the suggestions for further input based on the current command line.
   * @throws NullPointerException if the given command line is null.
   */
  @NonNull
  Collection<String> consoleTabCompleteResults(@NonNull String commandLine);

  /**
   * Sends the given command line to the current node and returns the output of the command from the execution. Only
   * lines which were sent to the command sender are returned, lines send to the console directly are not caught by this
   * method.
   *
   * @param commandLine the command line to execute.
   * @return all lines send to the command sender associated with this method call.
   * @throws NullPointerException if the given command line is null.
   */
  @NonNull
  Collection<String> sendCommandLine(@NonNull String commandLine);

  /**
   * Gets all nodes which are currently registered on the current node. As per the CloudNet cluster contract, each node
   * must know all other nodes in the cluster. Therefore, the returned collection is not synced with the cluster.
   * Additions and removals to the returned collection are not possible and will not have a result, use the add/remove
   * node methods for that purpose.
   *
   * @return all nodes which are registered on the current node.
   */
  @UnmodifiableView
  @NonNull
  Collection<NetworkClusterNode> nodes();

  /**
   * Get the network cluster node object association from the given node unique id registered on the current node. This
   * method returns null if no node with the given unique is registered on the local node.
   *
   * @param uniqueId the unique id of the node to get the associated object of.
   * @return the cluster node object registered on the current node or null if no node with the given name is present.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable NetworkClusterNode node(@NonNull String uniqueId);

  /**
   * Adds a new node on the current node and synchronizes the change into the cluster. This method has no effect if a
   * node with the same unique id is already present. This will also update the ip whitelist of all nodes and adds all
   * network listeners of the given cluster node to it.
   * <p>
   * After registering the node it can directly connect to the node the action was executed on. There is no guarantee
   * that the method has an immediate effect on all nodes.
   *
   * @param node the node to register.
   * @return true if the node was registered successfully, false otherwise.
   * @throws NullPointerException if the given node to register is null.
   */
  boolean addNode(@NonNull NetworkClusterNode node);

  /**
   * Removes the given node on the current node and synchronizes the change into the cluster. This method has no effect
   * if no node with the given unique id is registered on the current node. This will also update the ip whitelist of
   * all nodes and removes all network listeners of the given cluster node from it.
   * <p>
   * This method has an immediate effect. All nodes will disconnect the node and mark all services started on it as
   * removed. The node has no chance after the method call to reconnect to the cluster until it was added again.
   *
   * @param uniqueId the unique id of the node to remove.
   * @return true if the node was removed successfully, false otherwise.
   * @throws NullPointerException if the given node unique id is null.
   */
  boolean removeNode(@NonNull String uniqueId);

  /**
   * Get the network cluster node snapshots of all nodes which are currently connected to the current node. A node might
   * be registered and able to connect to the cluster, but will not appear in the list when it is not connected (or
   * didn't send a snapshot after the connection yet).
   * <p>
   * The returned snapshot collection is not modifiable as additions or removals from it will have no effect.
   *
   * @return the snapshot information of all nodes which are currently connected to the current node.
   */
  @NonNull
  Collection<NodeInfoSnapshot> nodeInfoSnapshots();

  /**
   * Get the network cluster node snapshot of the node with the given unique id. This method returns null either if no
   * node with the given unique id exists or the node is currently not connected. If the existence of a node should get
   * checked, use {@link #node(String)} instead.
   *
   * @param uniqueId the unique id of the node to get the snapshot of.
   * @return the snapshot of the node or null if no node with the given id is registered or the node is not connected.
   * @throws NullPointerException if the given node unique id is null.
   */
  @Nullable NodeInfoSnapshot nodeInfoSnapshot(@NonNull String uniqueId);

  /**
   * Get all commands which are registered on the current node. Additions and removals to the returned collection are
   * not possible and will not have any effect. There is no way to register a command from this provider because remote
   * commands are not supported. You need to use the node command provider to register a command on the current node.
   *
   * @return a task completed with all registered commands.
   */
  @NonNull
  CompletableFuture<Collection<CommandInfo>> consoleCommandsAsync();

  /**
   * Get an information about a specific registered command on the current node. This method returns null if no command
   * with the given name is present on the current node.
   *
   * @param name the name of the command to get the information of.
   * @return a task completed with the info of the command with the given name or null if no such command exists.
   * @throws NullPointerException if the given command name is null.
   */
  @NonNull
  CompletableFuture<CommandInfo> consoleCommandAsync(@NonNull String name);

  /**
   * Gets all tab complete results for the next argument in the provided command line. An empty command line will list
   * all root command names. This method resolves all tab completion results which are allowed to be executed via a
   * console command source. This does not mean that the api is actually able to execute the command. For example, if
   * the input is {@code task} one suggestion might be {@code setup} (which will start the task console setup
   * animation), the api is not allowed to execute that command but the result will be listed anyway.
   *
   * @param commandLine the current command line input to start the tab completion based on.
   * @return a task completed with the suggestions for further input based on the current command line.
   * @throws NullPointerException if the given command line is null.
   */
  @NonNull
  CompletableFuture<Collection<String>> consoleTabCompleteResultsAsync(@NonNull String commandLine);

  /**
   * Sends the given command line to the current node and returns the output of the command from the execution. Only
   * lines which were sent to the command sender are returned, lines send to the console directly are not caught by this
   * method.
   *
   * @param commandLine the command line to execute.
   * @return a task completed with all lines send to the command sender associated with this method call.
   * @throws NullPointerException if the given command line is null.
   */
  @NonNull
  CompletableFuture<Collection<String>> sendCommandLineAsync(@NonNull String commandLine);

  /**
   * Gets all nodes which are currently registered on the current node. As per the CloudNet cluster contract, each node
   * must know all other nodes in the cluster. Therefore, the returned collection is not synced with the cluster.
   * Additions and removals to the returned collection are not possible will not have a result, use the add/remove node
   * methods for that purpose.
   *
   * @return a task completed with all nodes which are registered on the current node.
   */
  @NonNull
  CompletableFuture<Collection<NetworkClusterNode>> nodesAsync();

  /**
   * Get the network cluster node object association from the given node unique id registered on the current node. This
   * method returns null if no node with the given unique is registered on the local node.
   *
   * @param uniqueId the unique id of the node to get the associated object of.
   * @return a task completed with the cluster node object associated with the given unique id or null if unknown.
   * @throws NullPointerException if the given unique id is null.
   */
  @NonNull
  CompletableFuture<NetworkClusterNode> nodeAsync(@NonNull String uniqueId);

  /**
   * Adds a new node on the current node and synchronizes the change into the cluster. This method has no effect if a
   * node with the same unique id is already present. This will also update the ip whitelist of all nodes and adds all
   * network listeners of the given cluster node to it.
   * <p>
   * After registering the node it can directly connect to the node the action was executed on. There is no guarantee
   * that the method has an immediate effect on all nodes.
   *
   * @param node the node to register.
   * @return a task completed with true if the node was registered successfully, false otherwise.
   * @throws NullPointerException if the given node to register is null.
   */
  @NonNull
  CompletableFuture<Boolean> addNodeAsync(@NonNull NetworkClusterNode node);

  /**
   * Removes the given node on the current node and synchronizes the change into the cluster. This method has no effect
   * if no node with the given unique id is registered on the current node. This will also update the ip whitelist of
   * all nodes and removes all network listeners of the given cluster node from it.
   * <p>
   * This method has an immediate effect. All nodes will disconnect the node and mark all services started on it as
   * removed. The node has no chance after the method call to reconnect to the cluster until it was added again.
   *
   * @param uniqueId the unique id of the node to remove.
   * @return a task completed with true if the node was removed successfully, false otherwise.
   * @throws NullPointerException if the given node unique id is null.
   */
  @NonNull
  CompletableFuture<Boolean> removeNodeAsync(@NonNull String uniqueId);

  /**
   * Get the network cluster node snapshots of all nodes which are currently connected to the current node. A node might
   * be registered and able to connect to the cluster, but will not appear in the list when it is not connected (or
   * didn't send a snapshot after the connection yet).
   * <p>
   * The returned snapshot collection is not modifiable as additions or removals from it will have no effect.
   *
   * @return a task completed with the snapshot of all nodes which are currently connected to the current node.
   */
  @NonNull
  CompletableFuture<Collection<NodeInfoSnapshot>> nodeInfoSnapshotsAsync();

  /**
   * Get the network cluster node snapshot of the node with the given unique id. This method returns null either if no
   * node with the given unique id exists or the node is currently not connected. If the existence of a node should get
   * checked, use {@link #node(String)} instead.
   *
   * @param uniqueId the unique id of the node to get the snapshot of.
   * @return a task completed with the snapshot of the node or null if the node is not registered or connected.
   * @throws NullPointerException if the given node unique id is null.
   */
  @NonNull
  CompletableFuture<NodeInfoSnapshot> nodeInfoSnapshotAsync(@NonNull String uniqueId);
}
