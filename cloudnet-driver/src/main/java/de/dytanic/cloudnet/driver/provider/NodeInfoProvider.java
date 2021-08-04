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

package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface provides access to the cluster
 */
public interface NodeInfoProvider {

  /**
   * @return the {@link CommandInfo} for each registered command
   */
  Collection<CommandInfo> getConsoleCommands();

  /**
   * @param commandLine the whole console input containing the command
   * @return the {@link CommandInfo} if there is a registered command - null otherwise
   */
  @Nullable
  CommandInfo getConsoleCommand(@NotNull String commandLine);

  /**
   * Gets all tab complete results for the specific command line. If the line contains at least one space, it will get
   * the command and then the tab complete results out of it. If the line doesn't contain any spaces, it will return the
   * names of all registered commands that begin with the {@code commandLine} (case-insensitive).
   *
   * @param commandLine the command with arguments to get the results from
   * @return a collection containing all unsorted results
   */
  Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine);

  /**
   * Sends the given commandLine to the node, executes the commandLine and returns the response
   *
   * @param commandLine the commandLine to be sent
   * @return the reponse of the node
   */
  String[] sendCommandLine(@NotNull String commandLine);

  /**
   * Sends the given commandLine to a specific node in the cluster, executes the commandLine and returns the response
   *
   * @param commandLine the commandLine to be sent
   * @return the response of the node
   */
  String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine);

  /**
   * @return all nodes from the config of the node where the method is called on
   */
  NetworkClusterNode[] getNodes();

  /**
   * @param uniqueId the uniqueId of the target node
   * @return {@link NetworkClusterNode} from the config of the node where the method is called on, null if there is no
   * entry in the config
   */
  @Nullable
  NetworkClusterNode getNode(@NotNull String uniqueId);

  /**
   * @return all {@link NetworkClusterNodeInfoSnapshot} of nodes that are still connected
   */
  NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots();

  /**
   * @param uniqueId the uniqueId of the target node
   * @return the {@link NetworkClusterNodeInfoSnapshot} for the given uniqueId, null if there is no snapshot
   */
  @Nullable
  NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId);

  /**
   * @return the {@link CommandInfo} for each registered command
   */
  @NotNull
  ITask<Collection<CommandInfo>> getConsoleCommandsAsync();

  /**
   * @param commandLine the whole console input containing the command
   * @return the {@link CommandInfo} if there is a registered command - null otherwise
   */
  @NotNull
  ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine);

  /**
   * Gets all tab complete results for the specific command line. If the line contains at least one space, it will get
   * the command and then the tab complete results out of it. If the line doesn't contain any spaces, it will return the
   * names of all registered commands that begin with the {@code commandLine} (case-insensitive).
   *
   * @param commandLine the command with arguments to get the results from
   * @return a collection containing all unsorted results
   */
  @NotNull
  ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine);

  /**
   * Sends the given commandLine to the node, executes the commandLine and returns the response
   *
   * @param commandLine the commandLine to be sent
   * @return the reponse of the node
   */
  @NotNull
  ITask<String[]> sendCommandLineAsync(@NotNull String commandLine);

  /**
   * Sends the given commandLine to a specific node in the cluster, executes the commandLine and returns the response
   *
   * @param commandLine the commandLine to be sent
   * @return the response of the node
   */
  @NotNull
  ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine);

  /**
   * @return all nodes from the config of the node where the method is called on
   */
  @NotNull
  ITask<NetworkClusterNode[]> getNodesAsync();

  /**
   * @param uniqueId the uniqueId of the target node
   * @return {@link NetworkClusterNode} from the config of the node where the method is called on, null if there is no
   * entry in the config
   */
  @NotNull
  ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId);

  /**
   * @return all {@link NetworkClusterNodeInfoSnapshot} of nodes that are still connected
   */
  @NotNull
  ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync();

  /**
   * @param uniqueId the uniqueId of the target node
   * @return the {@link NetworkClusterNodeInfoSnapshot} for the given uniqueId, null if there is no snapshot
   */
  @NotNull
  ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId);

}
