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

package de.dytanic.cloudnet.ext.bridge.node.player;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.ext.bridge.node.NodePlayerProvider;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.DefaultPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class NodePlayerManager extends DefaultPlayerManager implements IPlayerManager {

  private final String databaseName;

  private final Cache<UUID, ICloudOfflinePlayer> offlinePlayerCache = CacheBuilder.newBuilder()
    .concurrencyLevel(4)
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build();
  private final Map<UUID, CloudPlayer> onlineCloudPlayers = new ConcurrentHashMap<>();

  private final Striped<Lock> managementLocks = Striped.lazyWeakLock(1);
  private final PlayerProvider allPlayersProvider = new NodePlayerProvider(this,
    () -> this.onlineCloudPlayers.values().stream());

  public NodePlayerManager(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * @deprecated IPlayerManager should be accessed through the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
   */
  @Deprecated
  public static NodePlayerManager getInstance() {
    return (NodePlayerManager) CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
  }

  public Database getDatabase() {
    return CloudNet.getInstance().getDatabaseProvider().getDatabase(this.databaseName);
  }

  @Override
  public int getOnlineCount() {
    return this.onlineCloudPlayers.size();
  }

  @Override
  public long getRegisteredCount() {
    return this.getDatabase().getDocumentsCount();
  }

  @Nullable
  @Override
  public CloudPlayer getOnlinePlayer(@NotNull UUID uniqueId) {
    return this.onlineCloudPlayers.get(uniqueId);
  }

  @Override
  public @Nullable CloudPlayer getFirstOnlinePlayer(@NotNull String name) {
    for (CloudPlayer player : this.onlineCloudPlayers.values()) {
      if (player.getName().equalsIgnoreCase(name)) {
        return player;
      }
    }
    return null;
  }

  @Override
  public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.onlineCloudPlayers.values().stream().filter(cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name))
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
    Preconditions.checkNotNull(environment);

    return this.onlineCloudPlayers.values()
      .stream()
      .filter(cloudPlayer ->
        (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment)
          || (cloudPlayer.getConnectedService() != null
          && cloudPlayer.getConnectedService().getEnvironment() == environment))
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<CloudPlayer> getOnlinePlayers() {
    return new ArrayList<>(this.onlineCloudPlayers.values());
  }

  @Override
  public @NotNull PlayerProvider onlinePlayers() {
    return this.allPlayersProvider;
  }

  @Override
  public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
    return new NodePlayerProvider(
      this,
      () -> this.onlineCloudPlayers.values().stream()
        .filter(cloudPlayer -> cloudPlayer.getConnectedService().getTaskName().equalsIgnoreCase(task)
          || cloudPlayer.getLoginService().getTaskName().equalsIgnoreCase(task))
    );
  }

  @Override
  public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
    return new NodePlayerProvider(
      this,
      () -> this.onlineCloudPlayers.values().stream()
        .filter(cloudPlayer ->
          Arrays.binarySearch(cloudPlayer.getConnectedService().getGroups(), group) >= 0
            || Arrays.binarySearch(cloudPlayer.getLoginService().getGroups(), group) >= 0
        )
    );
  }

  @Override
  public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    ICloudOfflinePlayer offlinePlayer = this.offlinePlayerCache.getIfPresent(uniqueId);
    if (offlinePlayer == null) {
      JsonDocument jsonDocument = this.getDatabase().get(uniqueId.toString());
      if (jsonDocument != null) {
        offlinePlayer = this.parseOfflinePlayer(jsonDocument);
        this.offlinePlayerCache.put(uniqueId, offlinePlayer);
      }
    }
    return offlinePlayer;
  }

  @Override
  public @Nullable ICloudOfflinePlayer getFirstOfflinePlayer(@NotNull String name) {
    for (ICloudOfflinePlayer offlinePlayer : this.offlinePlayerCache.asMap().values()) {
      if (offlinePlayer.getName().equalsIgnoreCase(name)) {
        return offlinePlayer;
      }
    }
    return super.getFirstOfflinePlayer(name);
  }

  @Override
  public @NotNull List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.getDatabase().get(new JsonDocument("name", name)).stream()
      .map(this::parseOfflinePlayer)
      .collect(Collectors.toList());
  }

  @Override
  public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
    List<CloudOfflinePlayer> cloudOfflinePlayers = new ArrayList<>();

    this.getDatabase().iterate((s, jsonDocument) -> cloudOfflinePlayers.add(this.parseOfflinePlayer(jsonDocument)));

    return cloudOfflinePlayers;
  }

  private CloudOfflinePlayer parseOfflinePlayer(JsonDocument jsonDocument) {
    CloudOfflinePlayer cloudOfflinePlayer = jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE);

    NetworkServiceInfo networkServiceInfo = cloudOfflinePlayer.getLastNetworkConnectionInfo().getNetworkService();

    if (networkServiceInfo.getServiceId() == null || networkServiceInfo.getGroups() == null) {
      // CloudNet 3.3 and lower CloudOfflinePlayer database entries don't have a serviceId and groups, migrating them
      JsonDocument lastNetworkConnectionInfoDocument = jsonDocument
        .getDocument("lastNetworkConnectionInfo", new JsonDocument());
      JsonDocument networkServiceDocument = lastNetworkConnectionInfoDocument
        .getDocument("networkService", new JsonDocument());

      String[] serverNameSplit = networkServiceDocument.getString("serverName", "").split("-");

      ServiceId serviceId = new ServiceId(
        networkServiceDocument.get("uniqueId", UUID.class, UUID.randomUUID()),
        CloudNetDriver.getInstance().getComponentName(),
        serverNameSplit.length > 0 ? serverNameSplit[0] : "",
        serverNameSplit.length > 1 ? Integer.parseInt(serverNameSplit[1]) : -1,
        networkServiceDocument.get("environment", ServiceEnvironmentType.class, ServiceEnvironmentType.MINECRAFT_SERVER)
      );

      networkServiceInfo.setServiceId(serviceId);
      networkServiceInfo.setGroups(new String[0]);
      this.updateOfflinePlayer(cloudOfflinePlayer);
    }

    return cloudOfflinePlayer;
  }

  @Override
  @NotNull
  public ITask<Integer> getOnlineCountAsync() {
    return CompletedTask.create(this.onlineCloudPlayers.size());
  }

  @Override
  @NotNull
  public ITask<Long> getRegisteredCountAsync() {
    return this.getDatabase().getDocumentsCountAsync();
  }

  @Override
  @NotNull
  public ITask<ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
    return this.schedule(() -> this.getOnlinePlayer(uniqueId));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
    return this.schedule(() -> this.getOnlinePlayers(name));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
    return this.schedule(() -> this.getOnlinePlayers(environment));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
    return this.schedule(this::getOnlinePlayers);
  }

  @Override
  @NotNull
  public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
    return this.schedule(() -> this.getOfflinePlayer(uniqueId));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
    return this.schedule(() -> this.getOfflinePlayers(name));
  }

  @Override
  @NotNull
  public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
    return this.schedule(this::getRegisteredPlayers);
  }

  @Override
  public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    Preconditions.checkNotNull(cloudOfflinePlayer);

    this.updateOfflinePlayer0(cloudOfflinePlayer);
    this.messageBuilder()
      .message("update_offline_cloud_player")
      .buffer(ProtocolBuffer.create().writeObject(cloudOfflinePlayer))
      .targetAll()
      .build()
      .send();
  }

  public void updateOfflinePlayer0(ICloudOfflinePlayer cloudOfflinePlayer) {
    this.offlinePlayerCache.put(cloudOfflinePlayer.getUniqueId(), cloudOfflinePlayer);
    this.getDatabase()
      .update(cloudOfflinePlayer.getUniqueId().toString(), JsonDocument.newDocument(cloudOfflinePlayer));
  }

  public void handleOfflinePlayerUpdate(ICloudOfflinePlayer player) {
    this.offlinePlayerCache.put(player.getUniqueId(), player);

    Database database = this.getDatabase();
    if (!database.isSynced()) {
      database.updateAsync(player.getUniqueId().toString(), JsonDocument.newDocument(player));
    }
  }

  @Override
  public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
    Preconditions.checkNotNull(cloudPlayer);

    this.updateOnlinePlayer0(cloudPlayer);
    this.messageBuilder()
      .message("update_online_cloud_player")
      .buffer(ProtocolBuffer.create().writeObject(cloudPlayer))
      .targetAll()
      .build()
      .send();
  }

  @Override
  public void deleteCloudOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    Preconditions.checkNotNull(cloudOfflinePlayer);

    this.deleteCloudOfflinePlayer0(cloudOfflinePlayer);
    this.messageBuilder()
      .message("delete_offline_player")
      .targetAll()
      .buffer(ProtocolBuffer.create().writeObject(cloudOfflinePlayer))
      .build()
      .send();
  }

  public void deleteCloudOfflinePlayer0(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    this.getDatabase().delete(cloudOfflinePlayer.getUniqueId().toString());
    this.offlinePlayerCache.invalidate(cloudOfflinePlayer.getUniqueId());
  }

  @Override
  public ITask<Void> deleteCloudOfflinePlayerAsync(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
    return this.schedule(() -> {
      this.deleteCloudOfflinePlayer(cloudOfflinePlayer);
      return null;
    });
  }

  public void updateOnlinePlayer0(ICloudPlayer cloudPlayer) {
    this.updateOnlinePlayerInCache(cloudPlayer);
    this.updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
  }

  public void handleOnlinePlayerUpdate(ICloudPlayer cloudPlayer) {
    this.updateOnlinePlayerInCache(cloudPlayer);
    this.handleOfflinePlayerUpdate(CloudOfflinePlayer.of(cloudPlayer));
  }

  protected void updateOnlinePlayerInCache(ICloudPlayer cloudPlayer) {
    Lock handlingLock = this.managementLocks.get(cloudPlayer.getUniqueId());
    try {
      // lock the management lock of the player to ensure only one update at a time
      handlingLock.lock();
      // actually update the player if needed
      this.onlineCloudPlayers.replace(cloudPlayer.getUniqueId(), (CloudPlayer) cloudPlayer);
    } finally {
      handlingLock.unlock();
    }
  }

  @NotNull
  public <T> ITask<T> schedule(Callable<T> callable) {
    ITask<T> task = new ListenableTask<>(callable);
    CloudNetDriver.getInstance().getTaskExecutor().submit(task);
    return task;
  }

  public Map<UUID, CloudPlayer> getOnlineCloudPlayers() {
    return this.onlineCloudPlayers;
  }

  public String getDatabaseName() {
    return this.databaseName;
  }

  public void loginPlayer(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    Lock loginLock = this.managementLocks.get(networkConnectionInfo.getUniqueId());
    try {
      // ensure that we handle only one login message at a time
      loginLock.lock();
      this.loginPlayer0(networkConnectionInfo, networkPlayerServerInfo);
    } finally {
      loginLock.unlock();
    }
  }

  private void loginPlayer0(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    NetworkServiceInfo networkService = networkConnectionInfo.getNetworkService();
    CloudPlayer cloudPlayer = this.selectPlayerForLogin(networkConnectionInfo, networkPlayerServerInfo);
    // we can always update the name to keep it synced
    cloudPlayer.setName(networkConnectionInfo.getName());
    // check if the login service is a proxy and set the proxy as the login service if so
    if (networkService.getServiceId().getEnvironment().isMinecraftProxy()) {
      // a proxy should be able to change the login service
      cloudPlayer.setLoginService(networkService);
      // set the unique id of the player to the (most likely) online unique id
      // as a server might give us a offline mode unique id
      cloudPlayer.setUniqueId(networkConnectionInfo.getUniqueId());
    }
    // Set more information according to the server information which the proxy can't provide
    if (networkPlayerServerInfo != null) {
      cloudPlayer.setNetworkPlayerServerInfo(networkPlayerServerInfo);
      cloudPlayer.setConnectedService(networkPlayerServerInfo.getNetworkService());

      if (networkPlayerServerInfo.getXBoxId() != null) {
        cloudPlayer.setXBoxId(networkPlayerServerInfo.getXBoxId());
      }

      if (cloudPlayer.getLoginService() == null) {
        cloudPlayer.setLoginService(networkPlayerServerInfo.getNetworkService());
      }
    }
    // update the player into the database and notify the other nodes
    this.processLogin(cloudPlayer);
  }

  protected CloudPlayer selectPlayerForLogin(NetworkConnectionInfo connectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    // check if the player is already loaded
    CloudPlayer cloudPlayer = this.getOnlinePlayer(connectionInfo.getUniqueId());
    if (cloudPlayer == null) {
      // try to load the player using the name and the login service
      for (CloudPlayer player : this.getOnlineCloudPlayers().values()) {
        if (player.getName().equals(connectionInfo.getName())
          && player.getLoginService() != null
          && player.getLoginService().getUniqueId().equals(connectionInfo.getNetworkService().getUniqueId())) {
          cloudPlayer = player;
          break;
        }
      }
      // there is no loaded player, so try to load it using the offline association
      if (cloudPlayer == null) {
        ICloudOfflinePlayer cloudOfflinePlayer = this.getOrRegisterOfflinePlayer(connectionInfo);

        cloudPlayer = new CloudPlayer(
          cloudOfflinePlayer,
          connectionInfo.getNetworkService(),
          connectionInfo.getNetworkService(),
          connectionInfo,
          networkPlayerServerInfo
        );
        cloudPlayer.setLastLoginTimeMillis(System.currentTimeMillis());

        this.getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
      }
    }
    return cloudPlayer;
  }

  protected void processLogin(@NotNull CloudPlayer cloudPlayer) {
    // update the player into the database
    this.updateOnlinePlayer0(cloudPlayer);
    // notify the other nodes that we received the login
    ChannelMessage.builder()
      .channel("process_cloud_player_login")
      .buffer(ProtocolBuffer.create().writeObject(cloudPlayer))
      .targetNodes()
      .build()
      .send();
  }

  public void processLoginMessage(@NotNull CloudPlayer cloudPlayer) {
    Lock loginLock = this.managementLocks.get(cloudPlayer.getUniqueId());
    try {
      // ensure we only handle one login at a time
      loginLock.lock();
      // check if the player is already loaded
      CloudPlayer registeredPlayer = this.onlineCloudPlayers.get(cloudPlayer.getUniqueId());
      if (registeredPlayer == null) {
        this.onlineCloudPlayers.put(cloudPlayer.getUniqueId(), cloudPlayer);
        this.offlinePlayerCache.put(cloudPlayer.getUniqueId(), cloudPlayer);
      } else {
        boolean needsUpdate = false;
        // check if the player has a known login service
        if (cloudPlayer.getLoginService() != null) {
          NetworkServiceInfo newLoginService = cloudPlayer.getLoginService();
          NetworkServiceInfo knownLoginService = registeredPlayer.getLoginService();
          // check if we already know the same service
          if (!Objects.equals(newLoginService, knownLoginService)
            && newLoginService.getEnvironment().isMinecraftProxy()
            && (knownLoginService == null || !knownLoginService.getEnvironment().isMinecraftProxy())) {
            cloudPlayer.setLoginService(newLoginService);
            needsUpdate = true;
          }
        }
        // check if the player has a known connected service which is not a proxy
        if (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment()
          .isMinecraftProxy()) {
          NetworkServiceInfo knownConnectedService = registeredPlayer.getConnectedService();
          if (knownConnectedService != null && knownConnectedService.getEnvironment().isMinecraftServer()) {
            cloudPlayer.setConnectedService(knownConnectedService);
            needsUpdate = true;
          }
        }
        // check if we need to update the player
        if (needsUpdate) {
          this.updateOnlinePlayer0(cloudPlayer);
        }
      }
    } finally {
      loginLock.unlock();
    }
  }

  public ICloudOfflinePlayer getOrRegisterOfflinePlayer(NetworkConnectionInfo networkConnectionInfo) {
    ICloudOfflinePlayer cloudOfflinePlayer = this.getOfflinePlayer(networkConnectionInfo.getUniqueId());

    if (cloudOfflinePlayer == null) {
      // create a new player and cache it, the insert into the database will be done later during the login
      cloudOfflinePlayer = new CloudOfflinePlayer(
        networkConnectionInfo.getUniqueId(),
        networkConnectionInfo.getName(),
        null,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        networkConnectionInfo
      );
      this.offlinePlayerCache.put(networkConnectionInfo.getUniqueId(), cloudOfflinePlayer);
    }

    return cloudOfflinePlayer;
  }

  public void logoutPlayer(CloudPlayer cloudPlayer) {
    Lock managementLock = this.managementLocks.get(cloudPlayer.getUniqueId());
    try {
      // ensure only one update operation at a time
      managementLock.lock();
      // actually process the logout
      this.logoutPlayer0(cloudPlayer);
    } finally {
      managementLock.unlock();
    }
  }

  private void logoutPlayer0(CloudPlayer cloudPlayer) {
    // remove the player from the cache
    this.onlineCloudPlayers.remove(cloudPlayer.getUniqueId());
    cloudPlayer.setLastNetworkConnectionInfo(cloudPlayer.getNetworkConnectionInfo());
    // update the offline version of the player into the database
    this.updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
  }

  private void logoutPlayer(UUID uniqueId, String name, Predicate<CloudPlayer> predicate) {
    CloudPlayer cloudPlayer;
    if (uniqueId != null) {
      // if we can logout by unique id we need to lock the processing lock
      Lock managementLock = this.managementLocks.get(uniqueId);
      try {
        // lock the management lock to prevent duplicate handling at the same time
        managementLock.lock();
        // try the associated player
        cloudPlayer = this.getOnlinePlayer(uniqueId);
      } finally {
        // unlock the lock to allow the logout if the player is present
        managementLock.unlock();
      }
    } else {
      cloudPlayer = this.getFirstOnlinePlayer(name);
    }

    if (cloudPlayer != null && (predicate == null || predicate.test(cloudPlayer))) {
      this.logoutPlayer(cloudPlayer);
    }
  }

  public void logoutPlayer(NetworkConnectionInfo networkConnectionInfo) {
    this.logoutPlayer(
      networkConnectionInfo.getUniqueId(),
      networkConnectionInfo.getName(),
      networkConnectionInfo.getNetworkService()
    );
  }

  public void logoutPlayer(UUID uniqueId, String name, NetworkServiceInfo proxy) {
    this.logoutPlayer(
      uniqueId,
      name,
      player -> player.getLoginService().getUniqueId().equals(proxy.getUniqueId())
    );
  }
}
