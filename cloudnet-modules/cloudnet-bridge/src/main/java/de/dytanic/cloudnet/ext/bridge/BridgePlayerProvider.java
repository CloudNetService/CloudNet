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

package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class BridgePlayerProvider implements PlayerProvider {

  private final RPC rpc;
  private final RPCSender rpcSender;

  public BridgePlayerProvider(RPC rpc) {
    this.rpc = rpc;

    RPCProviderFactory factory = this.rpc.getSender().getFactory();
    this.rpcSender = factory.providerForClass(this.rpc.getSender().getAssociatedComponent(), PlayerProvider.class);
  }

  @Override
  public @NotNull Collection<? extends ICloudPlayer> asPlayers() {
    return this.rpc.join(this.rpcSender.invokeMethod("asPlayers")).fireSync();
  }

  @Override
  public @NotNull Collection<UUID> asUUIDs() {
    return this.rpc.join(this.rpcSender.invokeMethod("asUUIDs")).fireSync();
  }

  @Override
  public @NotNull Collection<String> asNames() {
    return this.rpc.join(this.rpcSender.invokeMethod("asNames")).fireSync();
  }

  @Override
  public int count() {
    return this.rpc.join(this.rpcSender.invokeMethod("count")).fireSync();
  }

  @Override
  public @NotNull ITask<Collection<? extends ICloudPlayer>> asPlayersAsync() {
    return CompletableTask.supplyAsync(this::asPlayers);
  }

  @Override
  public @NotNull ITask<Collection<UUID>> asUUIDsAsync() {
    return CompletableTask.supplyAsync(this::asUUIDs);
  }

  @Override
  public @NotNull ITask<Collection<String>> asNamesAsync() {
    return CompletableTask.supplyAsync(this::asNames);
  }

  @Override
  public @NotNull ITask<Integer> countAsync() {
    return CompletableTask.supplyAsync(this::count);
  }

}
