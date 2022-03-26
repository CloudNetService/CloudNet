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

package eu.cloudnetservice.modules.bridge.platform;

import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

final class PlatformPlayerManager implements PlayerManager {

  private final RPCSender sender;
  private final PlayerProvider allPlayers;
  private final PlayerExecutor globalPlayerExecutor;

  public PlatformPlayerManager(@NonNull Wrapper wrapper) {
    this.sender = wrapper.rpcFactory().providerForClass(wrapper.networkClient(), PlayerManager.class);
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
  public @Nullable CloudPlayer onlinePlayer(@NonNull UUID uniqueId) {
    return this.sender.invokeMethod("onlinePlayer", uniqueId).fireSync();
  }

  @Override
  public @Nullable CloudPlayer firstOnlinePlayer(@NonNull String name) {
    return this.sender.invokeMethod("firstOnlinePlayer", name).fireSync();
  }

  @Override
  public @NonNull List<? extends CloudPlayer> onlinePlayers(@NonNull String name) {
    return this.sender.invokeMethod("onlinePlayers", name).fireSync();
  }

  @Override
  public @NonNull List<? extends CloudPlayer> environmentOnlinePlayers(@NonNull ServiceEnvironmentType environment) {
    return this.sender.invokeMethod("environmentOnlinePlayers", environment).fireSync();
  }

  @Override
  public @NonNull PlayerProvider onlinePlayers() {
    return this.allPlayers;
  }

  @Override
  public @NonNull PlayerProvider taskOnlinePlayers(@NonNull String task) {
    return new PlatformPlayerProvider(this.sender.invokeMethod("taskOnlinePlayers", task));
  }

  @Override
  public @NonNull PlayerProvider groupOnlinePlayers(@NonNull String group) {
    return new PlatformPlayerProvider(this.sender.invokeMethod("groupOnlinePlayers", group));
  }

  @Override
  public @Nullable CloudOfflinePlayer offlinePlayer(@NonNull UUID uniqueId) {
    return this.sender.invokeMethod("offlinePlayer", uniqueId).fireSync();
  }

  @Override
  public @Nullable CloudOfflinePlayer firstOfflinePlayer(@NonNull String name) {
    return this.sender.invokeMethod("firstOfflinePlayer", name).fireSync();
  }

  @Override
  public @NonNull List<? extends CloudOfflinePlayer> offlinePlayers(@NonNull String name) {
    return this.sender.invokeMethod("offlinePlayers", name).fireSync();
  }

  @Override
  public @NonNull List<? extends CloudOfflinePlayer> registeredPlayers() {
    return this.sender.invokeMethod("registeredPlayers").fireSync();
  }

  @Override
  public void updateOfflinePlayer(@NonNull CloudOfflinePlayer cloudOfflinePlayer) {
    this.sender.invokeMethod("updateOfflinePlayer", cloudOfflinePlayer).fireSync();
  }

  @Override
  public void updateOnlinePlayer(@NonNull CloudPlayer cloudPlayer) {
    this.sender.invokeMethod("updateOnlinePlayer", cloudPlayer).fireSync();
  }

  @Override
  public void deleteCloudOfflinePlayer(@NonNull CloudOfflinePlayer cloudOfflinePlayer) {
    this.sender.invokeMethod("deleteCloudOfflinePlayer", cloudOfflinePlayer).fireSync();
  }

  @Override
  public @NonNull PlayerExecutor globalPlayerExecutor() {
    return this.globalPlayerExecutor;
  }

  @Override
  public @NonNull PlayerExecutor playerExecutor(@NonNull UUID uniqueId) {
    return new PlatformPlayerExecutor(this.sender.invokeMethod("playerExecutor", uniqueId), uniqueId);
  }
}
