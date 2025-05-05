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

package eu.cloudnetservice.modules.bridge.impl.platform;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;

abstract class PlatformPlayerManager implements PlayerManager {

  private final RPCSender sender;
  private final RPCImplementationBuilder.InstanceAllocator<PlayerProvider> playerProviderAllocator;
  private final RPCImplementationBuilder.InstanceAllocator<? extends PlayerExecutor> playerExecutorAllocator;

  private final PlayerProvider allPlayers;
  private final PlayerExecutor globalPlayerExecutor;

  @RPCInvocationTarget
  public PlatformPlayerManager(@NonNull RPCSender sender, @NonNull Supplier<NetworkChannel> channelSupplier) {
    this.sender = sender;

    // init rpc allocators
    var rpcFactory = sender.sourceFactory();
    this.playerProviderAllocator = rpcFactory.newRPCBasedImplementationBuilder(PlayerProvider.class)
      .implementConcreteMethods()
      .targetChannel(channelSupplier)
      .generateImplementation();
    this.playerExecutorAllocator = rpcFactory.newRPCBasedImplementationBuilder(PlatformPlayerExecutor.class)
      .superclass(PlayerExecutor.class)
      .targetChannel(channelSupplier)
      .generateImplementation();

    // init the static player utils
    var allPlayerBaseRPC = this.sender.invokeMethod("onlinePlayers");
    this.allPlayers = this.playerProviderAllocator.withBaseRPC(allPlayerBaseRPC).allocate();
    this.globalPlayerExecutor = this.playerExecutor(PlayerExecutor.GLOBAL_UNIQUE_ID);
  }

  @Override
  public @NonNull PlayerProvider onlinePlayers() {
    return this.allPlayers;
  }

  @Override
  public @NonNull PlayerProvider taskOnlinePlayers(@NonNull String task) {
    var baseRPC = this.sender.invokeCaller(task);
    return this.playerProviderAllocator.withBaseRPC(baseRPC).allocate();
  }

  @Override
  public @NonNull PlayerProvider groupOnlinePlayers(@NonNull String group) {
    var baseRPC = this.sender.invokeCaller(group);
    return this.playerProviderAllocator.withBaseRPC(baseRPC).allocate();
  }

  @Override
  public @NonNull PlayerExecutor globalPlayerExecutor() {
    return this.globalPlayerExecutor;
  }

  @Override
  public @NonNull PlayerExecutor playerExecutor(@NonNull UUID uniqueId) {
    var baseRPC = this.sender.invokeCaller(uniqueId);
    return this.playerExecutorAllocator
      .withBaseRPC(baseRPC)
      .withAdditionalConstructorParameters(uniqueId)
      .allocate();
  }
}
