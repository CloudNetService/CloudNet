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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.DefaultPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class BridgePlayerManager extends DefaultPlayerManager implements IPlayerManager {

  private final PlayerProvider allPlayersProvider;
  private final RPCSender rpcSender;

  public BridgePlayerManager(Wrapper wrapper) {
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), IPlayerManager.class);
    this.allPlayersProvider = new BridgePlayerProvider(this.rpcSender.invokeMethod("onlinePlayers"));
  }

  /**
   * @deprecated IPlayerManager should be accessed through the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
   */
  @Deprecated
  public static IPlayerManager getInstance() {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
  }

  @Override
  public int getOnlineCount() {
    return this.rpcSender.invokeMethod("getOnlineCount").fireSync();
  }

  @Override
  public long getRegisteredCount() {
    return this.rpcSender.invokeMethod("getRegisteredCount").fireSync();
  }

  @Nullable
  @Override
  public ICloudPlayer getOnlinePlayer(@NotNull UUID uniqueId) {
    return this.rpcSender.invokeMethod("getOnlinePlayer", uniqueId).fireSync();
  }

  @Override
  public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
    return this.rpcSender.invokeMethod("getOnlinePlayers", name).fireSync();
  }

  @Override
  public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
    return this.rpcSender.invokeMethod("getOnlinePlayers", environment).fireSync();
  }

  @Override
  public @NotNull List<? extends ICloudPlayer> getOnlinePlayers() {
    return new ArrayList<>(this.onlinePlayers().asPlayers());
  }

  @Override
  public @NotNull PlayerProvider onlinePlayers() {
    return this.allPlayersProvider;
  }

  @Override
  public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
    return new BridgePlayerProvider(this.rpcSender.invokeMethod("taskOnlinePlayers", task));
  }

  @Override
  public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
    return new BridgePlayerProvider(this.rpcSender.invokeMethod("groupOnlinePlayers", group));
  }

  @Override
  public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
    return this.rpcSender.invokeMethod("getOfflinePlayer", uniqueId).fireSync();
  }

  @Override
  public @NotNull List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
    return this.rpcSender.invokeMethod("getOnlinePlayers", name).fireSync();
  }

  @Override
  public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
    return this.rpcSender.invokeMethod("getRegisteredPlayers").fireSync();
  }

  @Override
  @NotNull
  public ITask<Integer> getOnlineCountAsync() {
    return CompletableTask.supplyAsync(this::getOnlineCount);
  }

  @Override
  @NotNull
  public ITask<Long> getRegisteredCountAsync() {
    return CompletableTask.supplyAsync(this::getRegisteredCount);
  }


  @Override
  @NotNull
  public ITask<? extends ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supplyAsync(() -> this.getOnlinePlayer(uniqueId));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getOnlinePlayers(name));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
    return CompletableTask.supplyAsync(() -> this.getOnlinePlayers(environment));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
    return this.onlinePlayers().asPlayersAsync().map(ArrayList::new);
  }

  @Override
  @NotNull
  public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supplyAsync(() -> this.getOfflinePlayer(uniqueId));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getOfflinePlayers(name));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
    return CompletableTask.supplyAsync(this::getRegisteredPlayers);
  }


  //TODO: look into this
  @Override
  public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    Preconditions.checkNotNull(cloudOfflinePlayer);

    this.messageBuilder()
      .message("update_offline_cloud_player")
      .buffer(ProtocolBuffer.create().writeObject(cloudOfflinePlayer))
      .targetAll()
      .build()
      .send();
  }

  @Override
  public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
    Preconditions.checkNotNull(cloudPlayer);

    this.messageBuilder()
      .message("update_online_cloud_player")
      .buffer(ProtocolBuffer.create().writeObject(cloudPlayer))
      .targetAll()
      .build()
      .send();
  }

  @Override
  public void deleteCloudOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    this.rpcSender.invokeMethod("deleteCloudOfflinePlayer", cloudOfflinePlayer).fireSync();
  }

  @Override
  public ITask<Void> deleteCloudOfflinePlayerAsync(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    return CompletableTask.supplyAsync(() -> this.deleteCloudOfflinePlayer(cloudOfflinePlayer));
  }
}
