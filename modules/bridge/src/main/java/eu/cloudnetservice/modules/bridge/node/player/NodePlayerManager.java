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

package eu.cloudnetservice.modules.bridge.node.player;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Striped;
import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerDisconnectEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerLoginEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudPlayerEvent;
import eu.cloudnetservice.modules.bridge.node.command.PlayersCommand;
import eu.cloudnetservice.modules.bridge.node.listener.BridgeLocalProxyPlayerDisconnectListener;
import eu.cloudnetservice.modules.bridge.node.network.NodePlayerChannelMessageListener;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(PlayerManager.class)
public class NodePlayerManager implements PlayerManager {

  protected final String databaseName;
  protected final EventManager eventManager;
  protected final CommandProvider commandProvider;
  protected final NodeDatabaseProvider nodeDatabaseProvider;

  protected final Map<UUID, CloudPlayer> onlinePlayers = new ConcurrentHashMap<>();
  protected final PlayerProvider allPlayerProvider = new NodePlayerProvider(() -> this.onlinePlayers.values().stream());

  protected final Striped<Lock> playerReadWriteLocks = Striped.lazyWeakLock(1);
  protected final LoadingCache<UUID, Optional<CloudOfflinePlayer>> offlinePlayerCache = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build(uniqueId -> {
      // load the player from the database sync to block the current thread
      var document = NodePlayerManager.this.database().get(uniqueId.toString());
      if (document == null) {
        return Optional.empty();
      } else {
        return Optional.of(document.toInstanceOf(CloudOfflinePlayer.class));
      }
    });

  @Inject
  public NodePlayerManager(
    @NonNull EventManager eventManager,
    @NonNull RPCFactory providerFactory,
    @NonNull CommandProvider commandProvider,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry,
    @NonNull NodeDatabaseProvider nodeDatabaseProvider
  ) {
    this.databaseName = BridgeManagement.BRIDGE_PLAYER_DB_NAME;
    this.eventManager = eventManager;
    this.commandProvider = commandProvider;
    this.nodeDatabaseProvider = nodeDatabaseProvider;
    // register the rpc listeners
    providerFactory.newHandler(PlayerManager.class, this).registerTo(handlerRegistry);
    providerFactory.newHandler(PlayerExecutor.class, null).registerTo(handlerRegistry);
    providerFactory.newHandler(PlayerProvider.class, null).registerTo(handlerRegistry);
    // register the data sync handler
    dataSyncRegistry.registerHandler(DataSyncHandler.<CloudPlayer>builder()
      .alwaysForce()
      .key("cloud_player")
      .convertObject(CloudPlayer.class)
      .nameExtractor(CloudPlayer::name)
      .dataCollector(this.onlinePlayers::values)
      .currentGetter(player -> this.onlinePlayers.get(player.uniqueId()))
      .writer(player -> this.onlinePlayers.put(player.uniqueId(), player))
      .build());
  }

  @PostConstruct
  private void registerPlayerCommand() {
    this.commandProvider.register(PlayersCommand.class);
  }

  @PostConstruct
  private void registerListeners() {
    this.eventManager.registerListener(BridgeLocalProxyPlayerDisconnectListener.class);
    this.eventManager.registerListener(NodePlayerChannelMessageListener.class);
  }

  @Override
  public int onlineCount() {
    return this.onlinePlayers.size();
  }

  @Override
  public long registeredCount() {
    return this.database().documentCount();
  }

  @Override
  public @Nullable CloudPlayer onlinePlayer(@NonNull UUID uniqueId) {
    return this.onlinePlayers.get(uniqueId);
  }

  @Override
  public @Nullable CloudPlayer firstOnlinePlayer(@NonNull String name) {
    for (var player : this.onlinePlayers.values()) {
      if (player.name().equalsIgnoreCase(name)) {
        return player;
      }
    }
    return null;
  }

  @Override
  public @NonNull List<CloudPlayer> onlinePlayers(@NonNull String name) {
    return this.onlinePlayers.values().stream()
      .filter(cloudPlayer -> cloudPlayer.name().equalsIgnoreCase(name))
      .toList();
  }

  @Override
  public @NonNull List<CloudPlayer> environmentOnlinePlayers(@NonNull ServiceEnvironmentType environment) {
    return this.onlinePlayers.values()
      .stream()
      .filter(cloudPlayer -> {
        var serviceInfo = Objects.requireNonNullElse(cloudPlayer.connectedService(), cloudPlayer.loginService());
        return serviceInfo.environment().equals(environment);
      })
      .toList();
  }

  @Override
  public @NonNull PlayerProvider onlinePlayers() {
    return this.allPlayerProvider;
  }

  @Override
  public @NonNull PlayerProvider taskOnlinePlayers(@NonNull String task) {
    return new NodePlayerProvider(() -> this.onlinePlayers.values()
      .stream()
      .filter(player -> {
        var serviceInfo = Objects.requireNonNullElse(player.connectedService(), player.loginService());
        return serviceInfo.taskName().equals(task);
      }));
  }

  @Override
  public @NonNull PlayerProvider groupOnlinePlayers(@NonNull String group) {
    return new NodePlayerProvider(() -> this.onlinePlayers.values()
      .stream()
      .filter(player -> {
        var serviceInfo = Objects.requireNonNullElse(player.connectedService(), player.loginService());
        return serviceInfo.groups().contains(group);
      }));
  }

  @Override
  public @Nullable CloudOfflinePlayer offlinePlayer(@NonNull UUID uniqueId) {
    return this.offlinePlayerCache.get(uniqueId).orElse(null);
  }

  @Override
  public @Nullable CloudOfflinePlayer firstOfflinePlayer(@NonNull String name) {
    return this.offlinePlayerCache.asMap().values().stream()
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(player -> player.name().equalsIgnoreCase(name))
      .findFirst()
      .orElseGet(() -> {
        var players = this.offlinePlayers(name);
        return players.isEmpty() ? null : players.get(0);
      });
  }

  @Override
  public @NonNull List<CloudOfflinePlayer> offlinePlayers(@NonNull String name) {
    return this.database().find("name", name).stream()
      .map(document -> document.toInstanceOf(CloudOfflinePlayer.class))
      .toList();
  }

  @Override
  public @NonNull List<CloudOfflinePlayer> registeredPlayers() {
    return this.database().entries().values().stream()
      .map(doc -> doc.toInstanceOf(CloudOfflinePlayer.class))
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public void updateOfflinePlayer(@NonNull CloudOfflinePlayer player) {
    // push the change to the cache
    this.pushOfflinePlayerCache(player.uniqueId(), player);
    // update the database
    this.database().insert(player.uniqueId().toString(), Document.newJsonDocument().appendTree(player));
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
  public void updateOnlinePlayer(@NonNull CloudPlayer cloudPlayer) {
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
  public void deleteCloudOfflinePlayer(@NonNull CloudOfflinePlayer cloudOfflinePlayer) {
    // push the change to the cache
    this.pushOfflinePlayerCache(cloudOfflinePlayer.uniqueId(), null);
    // delete from the database
    this.database().delete(cloudOfflinePlayer.uniqueId().toString());
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
  public @NonNull PlayerExecutor globalPlayerExecutor() {
    return NodePlayerExecutor.GLOBAL;
  }

  @Override
  public @NonNull PlayerExecutor playerExecutor(@NonNull UUID uniqueId) {
    return new NodePlayerExecutor(uniqueId, this);
  }

  public void pushOfflinePlayerCache(@NonNull UUID uniqueId, @Nullable CloudOfflinePlayer cloudOfflinePlayer) {
    this.offlinePlayerCache.put(uniqueId, Optional.ofNullable(cloudOfflinePlayer));
  }

  public void pushOnlinePlayerCache(@NonNull CloudPlayer cloudPlayer) {
    this.onlinePlayers.replace(cloudPlayer.uniqueId(), cloudPlayer);
    this.pushOfflinePlayerCache(cloudPlayer.uniqueId(), CloudOfflinePlayer.offlineCopy(cloudPlayer));
  }

  protected @NonNull LocalDatabase database() {
    return this.nodeDatabaseProvider.database(this.databaseName);
  }

  public @NonNull Map<UUID, CloudPlayer> players() {
    return this.onlinePlayers;
  }

  public void loginPlayer(
    @NonNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkServiceInfo joinedServiceInfo
  ) {
    var loginLock = this.playerReadWriteLocks.get(networkPlayerProxyInfo.uniqueId());
    try {
      // ensure that we handle only one login message at a time
      loginLock.lock();
      this.loginPlayer0(networkPlayerProxyInfo, joinedServiceInfo);
    } finally {
      loginLock.unlock();
    }
  }

  protected void loginPlayer0(
    @NonNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkServiceInfo joinedServiceInfo
  ) {
    var networkService = networkPlayerProxyInfo.networkService();
    var cloudPlayer = this.selectPlayerForLogin(networkPlayerProxyInfo, joinedServiceInfo);
    // set the service information of the services which requested the login
    cloudPlayer.loginService(networkService);
    cloudPlayer.connectedService(joinedServiceInfo);
    // update the player into the database and notify the other nodes
    this.processLogin(cloudPlayer);
  }

  protected @NonNull CloudPlayer selectPlayerForLogin(
    @NonNull NetworkPlayerProxyInfo connectionInfo,
    @Nullable NetworkServiceInfo joinedServiceInfo
  ) {
    // check if the player is already loaded
    var cloudPlayer = this.onlinePlayer(connectionInfo.uniqueId());
    if (cloudPlayer == null) {
      // try to load the player using the name and the login service
      for (var player : this.players().values()) {
        if (player.name().equals(connectionInfo.name())) {
          if (player.loginService().uniqueId().equals(connectionInfo.networkService().uniqueId())) {
            cloudPlayer = player;
            break;
          }
        }
      }
      // there is no loaded player, so try to load it using the offline association
      if (cloudPlayer == null) {
        // get the offline player or create a new one
        var cloudOfflinePlayer = this.getOrRegisterOfflinePlayer(connectionInfo);
        // convert the offline player to an online version using all provided information
        cloudPlayer = new CloudPlayer(
          connectionInfo,
          connectionInfo.networkService(),
          joinedServiceInfo == null ? connectionInfo.networkService() : joinedServiceInfo,
          null,
          Document.newJsonDocument(),
          connectionInfo.name(),
          cloudOfflinePlayer.firstLoginTimeMillis(),
          System.currentTimeMillis(),
          cloudOfflinePlayer.lastNetworkPlayerProxyInfo(),
          cloudOfflinePlayer.propertyHolder());
        // cache the online player for later use
        this.onlinePlayers.put(cloudPlayer.uniqueId(), cloudPlayer);
      }
    }
    // cannot never be null at this point
    return cloudPlayer;
  }

  protected void processLogin(@NonNull CloudPlayer cloudPlayer) {
    // push the player into the cache
    this.pushOnlinePlayerCache(cloudPlayer);
    // update the database
    this.database().insert(
      cloudPlayer.uniqueId().toString(),
      Document.newJsonDocument().appendTree(CloudOfflinePlayer.offlineCopy(cloudPlayer)));
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

  public void processLoginMessage(@NonNull CloudPlayer cloudPlayer) {
    var loginLock = this.playerReadWriteLocks.get(cloudPlayer.uniqueId());
    try {
      // ensure we only handle one login at a time
      loginLock.lock();
      // check if the player is already loaded
      var registeredPlayer = this.onlinePlayers.get(cloudPlayer.uniqueId());
      if (registeredPlayer == null) {
        this.onlinePlayers.put(cloudPlayer.uniqueId(), cloudPlayer);
        this.offlinePlayerCache.put(cloudPlayer.uniqueId(), Optional.of(cloudPlayer));
      } else {
        var needsUpdate = false;
        // check if the player has a known login service
        var newLoginService = cloudPlayer.loginService();
        var loginService = registeredPlayer.loginService();
        // check if we already know the same service
        if (!Objects.equals(newLoginService, loginService) && ServiceEnvironmentType.minecraftProxy(
          newLoginService.environment()) && !ServiceEnvironmentType.minecraftProxy(loginService.environment())) {
          cloudPlayer.loginService(newLoginService);
          needsUpdate = true;
        }
        // check if the player has a known connected service which is not a proxy
        if (cloudPlayer.connectedService() != null
          && ServiceEnvironmentType.minecraftProxy(cloudPlayer.connectedService().environment())) {
          var connectedService = registeredPlayer.connectedService();
          if (connectedService != null && ServiceEnvironmentType.minecraftServer(connectedService.environment())) {
            cloudPlayer.connectedService(connectedService);
            needsUpdate = true;
          }
        }
        // check if we need to update the player
        if (needsUpdate) {
          this.onlinePlayers.replace(cloudPlayer.uniqueId(), cloudPlayer);
        }
      }
    } finally {
      loginLock.unlock();
    }
  }

  public @NonNull CloudOfflinePlayer getOrRegisterOfflinePlayer(@NonNull NetworkPlayerProxyInfo proxyInfo) {
    var cloudOfflinePlayer = this.offlinePlayer(proxyInfo.uniqueId());
    // check if the player is already present
    if (cloudOfflinePlayer == null) {
      // create a new player and cache it, the insert into the database will be done later during the login
      cloudOfflinePlayer = new CloudOfflinePlayer(
        proxyInfo.name(),
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        proxyInfo,
        Document.newJsonDocument());
      this.offlinePlayerCache.put(proxyInfo.uniqueId(), Optional.of(cloudOfflinePlayer));
    }
    // the selected player
    return cloudOfflinePlayer;
  }

  public void logoutPlayer(@NonNull CloudPlayer cloudPlayer) {
    var managementLock = this.playerReadWriteLocks.get(cloudPlayer.uniqueId());
    try {
      // ensure only one update operation at a time
      managementLock.lock();
      // actually process the logout
      this.logoutPlayer0(cloudPlayer);
    } finally {
      managementLock.unlock();
    }
  }

  private void logoutPlayer0(@NonNull CloudPlayer cloudPlayer) {
    // remove the player from the cache
    this.onlinePlayers.remove(cloudPlayer.uniqueId());
    cloudPlayer.lastNetworkPlayerProxyInfo(cloudPlayer.networkPlayerProxyInfo());
    // copy to an offline version
    var offlinePlayer = CloudOfflinePlayer.offlineCopy(cloudPlayer);
    // update the offline version of the player into the cache
    this.pushOfflinePlayerCache(cloudPlayer.uniqueId(), offlinePlayer);
    // push the change to the database
    this.database().insert(offlinePlayer.uniqueId().toString(), Document.newJsonDocument().appendTree(offlinePlayer));
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
        cloudPlayer = this.onlinePlayer(uniqueId);
      } finally {
        // unlock the lock to allow the logout if the player is present
        managementLock.unlock();
      }
    } else {
      cloudPlayer = this.firstOnlinePlayer(name);
    }
    // check if we should log out the player
    if (cloudPlayer != null && (tester == null || tester.test(cloudPlayer))) {
      this.logoutPlayer(cloudPlayer);
    }
  }
}
