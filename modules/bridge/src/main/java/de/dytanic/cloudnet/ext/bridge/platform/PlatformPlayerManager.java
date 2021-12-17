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

package de.dytanic.cloudnet.ext.bridge.platform;

import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

final class PlatformPlayerManager implements IPlayerManager {

  private final RPCSender sender;
  private final PlayerProvider allPlayers;
  private final PlayerExecutor globalPlayerExecutor;

  public PlatformPlayerManager(@NotNull Wrapper wrapper) {
    this.sender = wrapper.rpcProviderFactory().providerForClass(wrapper.networkClient(), IPlayerManager.class);
    // init the static player utils
    this.globalPlayerExecutor = this.playerExecutor(PlayerExecutor.GLOBAL_UNIQUE_ID);
    this.allPlayers = new PlatformPlayerProvider(this.sender.invokeMethod("onlinePlayers"));
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int onlineCount() {
    return this.sender.invokeMethod("onlineCount").fireSync();
  }

  @Override
  public @Range(from = 0, to = Long.MAX_VALUE) long registeredCount() {
    return this.sender.invokeMethod("registeredCount").fireSync();
  }

  @Override
  public @Nullable CloudPlayer onlinePlayer(@NotNull UUID uniqueId) {
    return this.sender.invokeMethod("onlinePlayer", uniqueId).fireSync();
  }

  @Override
  public @Nullable CloudPlayer firstOnlinePlayer(@NotNull String name) {
    return this.sender.invokeMethod("firstOnlinePlayer", name).fireSync();
  }

  @Override
  public @NotNull List<? extends CloudPlayer> onlinePlayers(@NotNull String name) {
    return this.sender.invokeMethod("onlinePlayers", name).fireSync();
  }

  @Override
  public @NotNull List<? extends CloudPlayer> environmentOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
    return this.sender.invokeMethod("environmentOnlinePlayers", environment).fireSync();
  }

  @Override
  public @NotNull PlayerProvider onlinePlayers() {
    return this.allPlayers;
  }

  @Override
  public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
    return new PlatformPlayerProvider(this.sender.invokeMethod("taskOnlinePlayers", task));
  }

  @Override
  public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
    return new PlatformPlayerProvider(this.sender.invokeMethod("groupOnlinePlayers", group));
  }

  @Override
  public @Nullable CloudOfflinePlayer offlinePlayer(@NotNull UUID uniqueId) {
    return this.sender.invokeMethod("offlinePlayer", uniqueId).fireSync();
  }

  @Override
  public @Nullable CloudOfflinePlayer firstOfflinePlayer(@NotNull String name) {
    return this.sender.invokeMethod("firstOfflinePlayer", name).fireSync();
  }

  @Override
  public @NotNull List<? extends CloudOfflinePlayer> offlinePlayers(@NotNull String name) {
    return this.sender.invokeMethod("offlinePlayers", name).fireSync();
  }

  @Override
  public @NotNull List<? extends CloudOfflinePlayer> registeredPlayers() {
    return this.sender.invokeMethod("registeredPlayers").fireSync();
  }

  @Override
  public void updateOfflinePlayer(@NotNull CloudOfflinePlayer cloudOfflinePlayer) {
    this.sender.invokeMethod("updateOfflinePlayer", cloudOfflinePlayer).fireSync();
  }

  @Override
  public void updateOnlinePlayer(@NotNull CloudPlayer cloudPlayer) {
    this.sender.invokeMethod("updateOnlinePlayer", cloudPlayer).fireSync();
  }

  @Override
  public void deleteCloudOfflinePlayer(@NotNull CloudOfflinePlayer cloudOfflinePlayer) {
    this.sender.invokeMethod("deleteCloudOfflinePlayer", cloudOfflinePlayer).fireSync();
  }

  @Override
  public @NotNull PlayerExecutor globalPlayerExecutor() {
    return this.globalPlayerExecutor;
  }

  @Override
  public @NotNull PlayerExecutor playerExecutor(@NotNull UUID uniqueId) {
    return new PlatformPlayerExecutor(this.sender.invokeMethod("playerExecutor", uniqueId), uniqueId);
  }
}
