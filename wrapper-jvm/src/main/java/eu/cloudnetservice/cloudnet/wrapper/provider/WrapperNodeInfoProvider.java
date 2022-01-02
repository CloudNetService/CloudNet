/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.wrapper.provider;

import eu.cloudnetservice.cloudnet.driver.command.CommandInfo;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.provider.NodeInfoProvider;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class WrapperNodeInfoProvider implements NodeInfoProvider {

  private final RPCSender rpcSender;

  public WrapperNodeInfoProvider(@NonNull Wrapper wrapper) {
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      NodeInfoProvider.class);
  }

  @Override
  public @NonNull Collection<CommandInfo> consoleCommands() {
    return this.rpcSender.invokeMethod("consoleCommands").fireSync();
  }

  @Override
  public @Nullable CommandInfo consoleCommand(@NonNull String commandLine) {
    return this.rpcSender.invokeMethod("consoleCommand", commandLine).fireSync();
  }

  @Override
  public @NonNull Collection<String> consoleTabCompleteResults(@NonNull String commandLine) {
    return this.rpcSender.invokeMethod("consoleTabCompleteResults", commandLine).fireSync();
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    return this.rpcSender.invokeMethod("sendCommandLine", commandLine).fireSync();
  }

  @Override
  public @NonNull Collection<String> sendCommandLineToNode(@NonNull String nodeUniqueId, @NonNull String commandLine) {
    return this.rpcSender.invokeMethod("sendCommandLineToNode", nodeUniqueId, commandLine).fireSync();
  }

  @Override
  public NetworkClusterNode[] nodes() {
    return this.rpcSender.invokeMethod("nodes").fireSync();
  }

  @Override
  public @Nullable NetworkClusterNode node(@NonNull String uniqueId) {
    return this.rpcSender.invokeMethod("node", uniqueId).fireSync();
  }

  @Override
  public NetworkClusterNodeInfoSnapshot[] nodeInfoSnapshots() {
    return this.rpcSender.invokeMethod("nodeInfoSnapshots").fireSync();
  }

  @Override
  public @Nullable NetworkClusterNodeInfoSnapshot nodeInfoSnapshot(@NonNull String uniqueId) {
    return this.rpcSender.invokeMethod("nodeInfoSnapshot", uniqueId).fireSync();
  }
}
