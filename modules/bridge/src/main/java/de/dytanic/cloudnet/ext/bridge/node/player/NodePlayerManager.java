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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Striped;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.cluster.sync.DataSyncRegistry;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandPlayers;
import de.dytanic.cloudnet.ext.bridge.node.listener.BridgeLocalProxyPlayerDisconnectListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.BridgePluginIncludeListener;
import de.dytanic.cloudnet.ext.bridge.node.network.NodePlayerChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodePlayerManager implements IPlayerManager {

  protected final String databaseName;
  protected final IEventManager eventManager;

  protected final Map<UUID, CloudPlayer> onlinePlayers = new ConcurrentHashMap<>();
  protected final PlayerProvider allPlayersProvider = new NodePlayerProvider(
    () -> this.onlinePlayers.values().stream());

  protected final Striped<Lock> playerReadWriteLocks = Striped.lazyWeakLock(1);
  protected final LoadingCache<UUID, Optional<CloudOfflinePlayer>> offlinePlayerCache = CacheBuilder.newBuilder()
    .concurrencyLevel(4)
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build(new CacheLoader<UUID, Optional<CloudOfflinePlayer>>() {
      @Override
      public Optional<CloudOfflinePlayer> load(@NotNull UUID uniqueId) {
        var document = NodePlayerManager.this.getDatabase().get(uniqueId.toString());
        if (document == null) {
          return Optional.empty();
        } else {
          return Optional.of(document.toInstanceOf(CloudOfflinePlayer.class));
        }
      }
    });

  public NodePlayerManager(
    @NotNull String databaseName,
    @NotNull IEventManager eventManager,
    @NotNull DataSyncRegistry dataSyncRegistry,
    @NotNull RPCProviderFactory providerFactory,
    @NotNull BridgeManagement bridgeManagement
  ) {
    this.databaseName = databaseName;
    this.eventManager = eventManager;
    // register the listeners which are required to run
    eventManager.registerListener(new BridgePluginIncludeListener(bridgeManagement));
    eventManager.registerListener(new BridgeLocalProxyPlayerDisconnectListener(this));
    eventManager.registerListener(new NodePlayerChannelMessageListener(eventManager, this, bridgeManagement));
    // register the players command
    CloudNet.getInstance().getCommandProvider().register(new CommandPlayers(this));
    // register the rpc listeners
    providerFactory.newHandler(IPlayerManager.class, this).registerToDefaultRegistry();
    providerFactory.newHandler(PlayerExecutor.class, null).registerToDefaultRegistry();
    providerFactory.newHandler(PlayerProvider.class, null).registerToDefaultRegistry();
    // register the data sync handler
    dataSyncRegistry.registerHandler(DataSyncHandler.<CloudPlayer>builder()
      .alwaysForce()
      .key("cloud_player")
      .convertObject(CloudPlayer.class)
      .nameExtractor(CloudPlayer::getName)
      .dataCollector(this.onlinePlayers::values)
      .currentGetter(player -> this.onlinePlayers.get(player.getUniqueId()))
      .writer(player -> this.onlinePlayers.put(player.getUniqueId(), player))
      .build());
  }

  @Override
  public int getOnlineCount() {
    return this.onlinePlayers.size();
  }

  @Override
  public long getRegisteredCount() {
    return this.getDatabase().getDocumentsCount();
  }

  @Override
  public @Nullable CloudPlayer getOnlinePlayer(@NotNull UUID uniqueId) {
    return this.onlinePlayers.get(uniqueId);
  }

  @Override
  public @Nullable CloudPlayer getFirstOnlinePlayer(@NotNull String name) {
    for (var player : this.onlinePlayers.values()) {
      if (player.getName().equalsIgnoreCase(name)) {
        return player;
      }
    }
    return null;
  }

  @Override
  public @NotNull List<? extends CloudPlayer> getOnlinePlayers(@NotNull String name) {
    return this.onlinePlayers.values().stream()
      .filter(cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name))
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<? extends CloudPlayer> getEnvironmentOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
    return this.onlinePlayers.values()
      .stream()
      .filter(cloudPlayer ->
        (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment)
          || (cloudPlayer.getConnectedService() != null
          && cloudPlayer.getConnectedService().getEnvironment() == environment))
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull PlayerProvider onlinePlayers() {
    return this.allPlayersProvider;
  }

  @Override
  public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
    return new NodePlayerProvider(() -> this.onlinePlayers.values()
      .stream()
      .filter(cloudPlayer -> cloudPlayer.getConnectedService().getTaskName().equalsIgnoreCase(task)
        || cloudPlayer.getLoginService().getTaskName().equalsIgnoreCase(task)));
  }

  @Override
  public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
    return new NodePlayerProvider(() -> this.onlinePlayers.values()
      .stream()
      .filter(cloudPlayer -> cloudPlayer.getConnectedService().getGroups().contains(group)
        || cloudPlayer.getLoginService().getGroups().contains(group)));
  }

  @Override
  public CloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
    return this.offlinePlayerCache.getUnchecked(uniqueId).orElse(null);
  }

  @Override
  public @Nullable CloudOfflinePlayer getFirstOfflinePlayer(@NotNull String name) {
    return this.offlinePlayerCache.asMap().values().stream()
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(player -> player.getName().equalsIgnoreCase(name))
      .findFirst()
      .orElseGet(() -> IPlayerManager.super.getFirstOfflinePlayer(name));
  }

  @Override
  public @NotNull List<? extends CloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
    return this.getDatabase().get(JsonDocument.newDocument("name", name)).stream()
      .map(document -> document.toInstanceOf(CloudOfflinePlayer.class))
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<? extends CloudOfflinePlayer> getRegisteredPlayers() {
    return this.getDatabase().entries().values().stream()
      .map(doc -> doc.toInstanceOf(CloudOfflinePlayer.class))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public void updateOfflinePlayer(@NotNull CloudOfflinePlayer player) {
    // push the change to the cache
    this.pushOfflinePlayerCache(player.getUniqueId(), player);
    // update the database
    this.getDatabase().update(player.getUniqueId().toString(), JsonDocument.newDocument(player));
    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .message("update_offline_cloud_player")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(player))
      .build()
      .send();
    // call the update event locally
    this.eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
  }

  @Override
  public void updateOnlinePlayer(@NotNull CloudPlayer cloudPlayer) {
    // push the change to the cache
    this.pushOnlinePlayerCache(cloudPlayer);
    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .message("update_online_cloud_player")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(cloudPlayer))
      .build()
      .send();
    // call the update event locally
    this.eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(cloudPlayer));
  }

  @Override
  public void deleteCloudOfflinePlayer(@NotNull CloudOfflinePlayer cloudOfflinePlayer) {
    // push the change to the cache
    this.pushOfflinePlayerCache(cloudOfflinePlayer.getUniqueId(), null);
    // delete from the database
    this.getDatabase().delete(cloudOfflinePlayer.getUniqueId().toString());
    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .message("delete_offline_cloud_player")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(cloudOfflinePlayer))
      .build()
      .send();
    // call the update event locally
    this.eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(cloudOfflinePlayer));
  }

  @Override
  public @NotNull PlayerExecutor getGlobalPlayerExecutor() {
    return NodePlayerExecutor.GLOBAL;
  }

  @Override
  public @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId) {
    return new NodePlayerExecutor(uniqueId, this);
  }

  public void pushOfflinePlayerCache(@NotNull UUID uniqueId, @Nullable CloudOfflinePlayer cloudOfflinePlayer) {
    this.offlinePlayerCache.put(uniqueId, Optional.ofNullable(cloudOfflinePlayer));
  }

  public void pushOnlinePlayerCache(@NotNull CloudPlayer cloudPlayer) {
    this.onlinePlayers.replace(cloudPlayer.getUniqueId(), cloudPlayer);
    this.pushOfflinePlayerCache(cloudPlayer.getUniqueId(), CloudOfflinePlayer.offlineCopy(cloudPlayer));
  }

  protected @NotNull LocalDatabase getDatabase() {
    return CloudNet.getInstance().getDatabaseProvider().getDatabase(this.databaseName);
  }

  public @NotNull Map<UUID, CloudPlayer> getOnlinePlayers() {
    return this.onlinePlayers;
  }

  public void loginPlayer(
    @NotNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkPlayerServerInfo networkPlayerServerInfo
  ) {
    var loginLock = this.playerReadWriteLocks.get(networkPlayerProxyInfo.uniqueId());
    try {
      // ensure that we handle only one login message at a time
      loginLock.lock();
      this.loginPlayer0(networkPlayerProxyInfo, networkPlayerServerInfo);
    } finally {
      loginLock.unlock();
    }
  }

  protected void loginPlayer0(
    @NotNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkPlayerServerInfo networkPlayerServerInfo
  ) {
    var networkService = networkPlayerProxyInfo.networkService();
    var cloudPlayer = this.selectPlayerForLogin(networkPlayerProxyInfo, networkPlayerServerInfo);
    // check if the login service is a proxy and set the proxy as the login service if so
    if (ServiceEnvironmentType.isMinecraftProxy(networkService.getServiceId().getEnvironment())) {
      // a proxy should be able to change the login service
      cloudPlayer.setLoginService(networkService);
    }
    // Set more information according to the server information which the proxy can't provide
    if (networkPlayerServerInfo != null) {
      cloudPlayer.setNetworkPlayerServerInfo(networkPlayerServerInfo);
      cloudPlayer.setConnectedService(networkPlayerServerInfo.networkService());

      if (cloudPlayer.getLoginService() == null) {
        cloudPlayer.setLoginService(networkPlayerServerInfo.networkService());
      }
    }
    // update the player into the database and notify the other nodes
    if (networkPlayerServerInfo == null) {
      this.processLogin(cloudPlayer);
    }
  }

  protected @NotNull CloudPlayer selectPlayerForLogin(
    @NotNull NetworkPlayerProxyInfo connectionInfo,
    @Nullable NetworkPlayerServerInfo serverInfo
  ) {
    // check if the player is already loaded
    var cloudPlayer = this.getOnlinePlayer(connectionInfo.uniqueId());
    if (cloudPlayer == null) {
      // try to load the player using the name and the login service
      for (var player : this.getOnlinePlayers().values()) {
        if (player.getName().equals(connectionInfo.name())
          && player.getLoginService() != null
          && player.getLoginService().getUniqueId().equals(connectionInfo.networkService().getUniqueId())) {
          cloudPlayer = player;
          break;
        }
      }
      // there is no loaded player, so try to load it using the offline association
      if (cloudPlayer == null) {
        // get the offline player or create a new one
        var cloudOfflinePlayer = this.getOrRegisterOfflinePlayer(connectionInfo);
        // convert the offline player to an online version using all provided information
        cloudPlayer = new CloudPlayer(
          connectionInfo.networkService(),
          serverInfo == null ? connectionInfo.networkService() : serverInfo.networkService(),
          connectionInfo,
          serverInfo,
          JsonDocument.newDocument(),
          cloudOfflinePlayer.getFirstLoginTimeMillis(),
          System.currentTimeMillis(),
          cloudOfflinePlayer.getLastNetworkPlayerProxyInfo(),
          cloudOfflinePlayer.getProperties());
        // cache the online player for later use
        this.onlinePlayers.put(cloudPlayer.getUniqueId(), cloudPlayer);
      }
    }
    // cannot never be null at this point
    return cloudPlayer;
  }

  protected void processLogin(@NotNull CloudPlayer cloudPlayer) {
    // push the player into the cache
    this.pushOnlinePlayerCache(cloudPlayer);
    // update the database
    this.getDatabase().insert(
      cloudPlayer.getUniqueId().toString(),
      JsonDocument.newDocument(CloudOfflinePlayer.offlineCopy(cloudPlayer)));
    // notify the other nodes that we received the login
    ChannelMessage.builder()
      .targetAll()
      .message("process_cloud_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(cloudPlayer))
      .build()
      .send();
    // call the update event locally
    this.eventManager.callEvent(new BridgeProxyPlayerLoginEvent(cloudPlayer));
  }

  public void processLoginMessage(@NotNull CloudPlayer cloudPlayer) {
    var loginLock = this.playerReadWriteLocks.get(cloudPlayer.getUniqueId());
    try {
      // ensure we only handle one login at a time
      loginLock.lock();
      // check if the player is already loaded
      var registeredPlayer = this.onlinePlayers.get(cloudPlayer.getUniqueId());
      if (registeredPlayer == null) {
        this.onlinePlayers.put(cloudPlayer.getUniqueId(), cloudPlayer);
        this.offlinePlayerCache.put(cloudPlayer.getUniqueId(), Optional.of(cloudPlayer));
      } else {
        var needsUpdate = false;
        // check if the player has a known login service
        if (cloudPlayer.getLoginService() != null) {
          var newLoginService = cloudPlayer.getLoginService();
          var loginService = registeredPlayer.getLoginService();
          // check if we already know the same service
          if (!Objects.equals(newLoginService, loginService)
            && ServiceEnvironmentType.isMinecraftProxy(newLoginService.getEnvironment())
            && (loginService == null || !ServiceEnvironmentType.isMinecraftProxy(loginService.getEnvironment()))) {
            cloudPlayer.setLoginService(newLoginService);
            needsUpdate = true;
          }
        }
        // check if the player has a known connected service which is not a proxy
        if (cloudPlayer.getConnectedService() != null
          && ServiceEnvironmentType.isMinecraftProxy(cloudPlayer.getConnectedService().getEnvironment())) {
          var connectedService = registeredPlayer.getConnectedService();
          if (connectedService != null && ServiceEnvironmentType.isMinecraftServer(connectedService.getEnvironment())) {
            cloudPlayer.setConnectedService(connectedService);
            needsUpdate = true;
          }
        }
        // check if we need to update the player
        if (needsUpdate) {
          this.onlinePlayers.replace(cloudPlayer.getUniqueId(), cloudPlayer);
        }
      }
    } finally {
      loginLock.unlock();
    }
  }

  public @NotNull CloudOfflinePlayer getOrRegisterOfflinePlayer(@NotNull NetworkPlayerProxyInfo proxyInfo) {
    var cloudOfflinePlayer = this.getOfflinePlayer(proxyInfo.uniqueId());
    // check if the player is already present
    if (cloudOfflinePlayer == null) {
      // create a new player and cache it, the insert into the database will be done later during the login
      cloudOfflinePlayer = new CloudOfflinePlayer(
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        proxyInfo.name(),
        proxyInfo);
      this.offlinePlayerCache.put(proxyInfo.uniqueId(), Optional.of(cloudOfflinePlayer));
    }
    // the selected player
    return cloudOfflinePlayer;
  }

  public void logoutPlayer(@NotNull CloudPlayer cloudPlayer) {
    var managementLock = this.playerReadWriteLocks.get(cloudPlayer.getUniqueId());
    try {
      // ensure only one update operation at a time
      managementLock.lock();
      // actually process the logout
      this.logoutPlayer0(cloudPlayer);
    } finally {
      managementLock.unlock();
    }
  }

  private void logoutPlayer0(@NotNull CloudPlayer cloudPlayer) {
    // remove the player from the cache
    this.onlinePlayers.remove(cloudPlayer.getUniqueId());
    cloudPlayer.setLastNetworkPlayerProxyInfo(cloudPlayer.getNetworkPlayerProxyInfo());
    // copy to an offline version
    var offlinePlayer = CloudOfflinePlayer.offlineCopy(cloudPlayer);
    // update the offline version of the player into the cache
    this.pushOfflinePlayerCache(cloudPlayer.getUniqueId(), offlinePlayer);
    // push the change to the database
    this.getDatabase().insert(offlinePlayer.getUniqueId().toString(), JsonDocument.newDocument(offlinePlayer));
    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .message("process_cloud_player_logout")
      .buffer(DataBuf.empty().writeObject(cloudPlayer))
      .build()
      .send();
    // call the update event locally
    this.eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(cloudPlayer));
  }

  @Contract("!null, !null, _ -> _; !null, null, _ -> _; null, !null, _ -> _; null, null, _ -> fail")
  public void logoutPlayer(@Nullable UUID uniqueId, @Nullable String name, @Nullable Predicate<CloudPlayer> tester) {
    // either the name or unique id must be given
    Preconditions.checkArgument(uniqueId != null || name != null);
    // get the cloud player matching the arguments
    CloudPlayer cloudPlayer;
    if (uniqueId != null) {
      // if we can log out by unique id we need to lock the processing lock
      var managementLock = this.playerReadWriteLocks.get(uniqueId);
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
    // check if we should log out the player
    if (cloudPlayer != null && (tester == null || tester.test(cloudPlayer))) {
      this.logoutPlayer(cloudPlayer);
    }
  }
}
