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

package eu.cloudnetservice.modules.syncproxy.impl.platform;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConfigurationUpdateEvent;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyLoginConfiguration;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyMotd;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyTabList;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyTabListConfiguration;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformSyncProxyManagement<P> implements SyncProxyManagement {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  protected final Map<UUID, Integer> proxyOnlineCountCache = new HashMap<>();

  protected final RPCSender rpcSender;
  protected final RPCFactory rpcFactory;
  protected final EventManager eventManager;
  protected final NetworkClient networkClient;
  protected final WrapperConfiguration wrapperConfig;
  protected final ServiceInfoHolder serviceInfoHolder;
  protected final CloudServiceProvider serviceProvider;
  protected final ScheduledExecutorService executorService;

  protected SyncProxyConfiguration configuration;
  protected SyncProxyLoginConfiguration currentLoginConfiguration;
  protected SyncProxyTabListConfiguration currentTabListConfiguration;
  protected ScheduledFuture<?> currentUpdateTask;

  protected PlatformSyncProxyManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull ScheduledExecutorService executorService
  ) {
    this.rpcFactory = rpcFactory;
    this.eventManager = eventManager;
    this.networkClient = networkClient;
    this.wrapperConfig = wrapperConfig;
    this.serviceInfoHolder = serviceInfoHolder;
    this.serviceProvider = serviceProvider;
    this.executorService = executorService;

    this.rpcSender = rpcFactory.newRPCSenderBuilder(SyncProxyManagement.class).targetComponent(networkClient).build();
  }

  protected void init() {
    // get the config from the node
    this.configurationSilently(this.rpcSender.invokeMethod("configuration").fireSync());
    // cache all services that are already started
    this.serviceProvider.servicesAsync().thenAccept(services -> {
      for (var service : services) {
        this.cacheServiceInfoSnapshot(service);
      }
    });
  }

  public void configurationSilently(@NonNull SyncProxyConfiguration configuration) {
    this.configuration = configuration;
    this.eventManager.callEvent(new SyncProxyConfigurationUpdateEvent(configuration));

    this.currentLoginConfiguration = configuration.loginConfigurations()
      .stream()
      .filter(loginConfiguration -> this.wrapperConfig.serviceConfiguration().groups()
        .contains(loginConfiguration.targetGroup()))
      .findFirst()
      .orElse(null);

    this.currentTabListConfiguration = configuration.tabListConfigurations()
      .stream()
      .filter(tabListConfiguration -> this.wrapperConfig.serviceConfiguration().groups()
        .contains(tabListConfiguration.targetGroup()))
      .findFirst()
      .orElse(null);

    this.scheduleTabListUpdate();
    this.applyWhitelist();
  }

  @Override
  public @NonNull SyncProxyConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull SyncProxyConfiguration configuration) {
    this.rpcSender.invokeCaller(configuration).fireSync();
  }

  public @Nullable SyncProxyMotd randomMotd() {
    if (this.currentLoginConfiguration != null) {
      var motds =
        this.currentLoginConfiguration.maintenance()
          ? this.currentLoginConfiguration.maintenanceMotds()
          : this.currentLoginConfiguration.motds();
      if (!motds.isEmpty()) {
        return motds.get(ThreadLocalRandom.current().nextInt(motds.size()));
      }
    }
    // we dont have any motd
    return null;
  }

  public void applyWhitelist() {
    // check if there is a configuration for this targetGroup
    if (this.currentLoginConfiguration != null && this.currentLoginConfiguration.maintenance()) {
      for (var onlinePlayer : this.onlinePlayers()) {
        // check if the player is allowed to join
        if (!this.checkPlayerMaintenance(onlinePlayer)) {
          this.disconnectPlayer(onlinePlayer, this.configuration.message("player-login-not-whitelisted", null));
        }
      }
    }
  }

  public @Nullable SyncProxyLoginConfiguration currentLoginConfiguration() {
    return this.currentLoginConfiguration;
  }

  public @Nullable SyncProxyTabListConfiguration currentTabListConfiguration() {
    return this.currentTabListConfiguration;
  }

  public int onlinePlayerCount() {
    return this.proxyOnlineCountCache.values().stream().mapToInt(value -> value).sum();
  }

  protected int maxPlayerCount() {
    if (this.currentLoginConfiguration == null) {
      return 0;
    }

    return this.currentLoginConfiguration.maxPlayers();
  }

  public void cacheServiceInfoSnapshot(@NonNull ServiceInfoSnapshot snapshot) {
    if (ServiceEnvironmentType.minecraftProxy(snapshot.serviceId().environment()) && this.checkServiceGroup(snapshot)) {
      this.proxyOnlineCountCache.put(
        snapshot.serviceId().uniqueId(),
        snapshot.readProperty(BridgeDocProperties.ONLINE_COUNT));
    }
  }

  public void removeCachedServiceInfoSnapshot(@NonNull ServiceInfoSnapshot snapshot) {
    this.proxyOnlineCountCache.remove(snapshot.serviceId().uniqueId());
  }

  public @Nullable String serviceUpdateMessage(
    @NonNull String key,
    @NonNull ServiceInfoSnapshot serviceInfoSnapshot
  ) {
    return this.configuration.message(key, message -> message
      .replace("%service%", serviceInfoSnapshot.name())
      .replace("%node%", serviceInfoSnapshot.serviceId().nodeUniqueId()));
  }

  protected void scheduleTabListUpdate() {
    if (this.currentUpdateTask != null) {
      this.currentUpdateTask.cancel(true);
      this.currentUpdateTask = null;
    }

    if (this.currentTabListConfiguration != null && !this.currentTabListConfiguration.entries().isEmpty()) {
      this.currentUpdateTask = this.executorService.scheduleWithFixedDelay(
        () -> {
          var tabList = this.currentTabListConfiguration.tick();
          this.updateTabList(tabList);
        },
        0,
        (long) (1000 / this.currentTabListConfiguration.animationsPerSecond()),
        TimeUnit.MILLISECONDS);
    }
  }

  protected void updateTabList(@NonNull SyncProxyTabList tabList) {
    var onlinePlayers = this.onlinePlayerCount();
    var maxPlayers = this.maxPlayerCount();
    for (var onlinePlayer : this.onlinePlayers()) {
      this.updateTabList(onlinePlayer, tabList, onlinePlayers, maxPlayers);
    }
  }

  protected void updateTabList(
    @NonNull P player,
    @NonNull SyncProxyTabList tabList,
    int onlinePlayers,
    int maxPlayers
  ) {
    var header = this.replaceTabListItem(
      tabList.header(),
      player,
      onlinePlayers,
      maxPlayers);
    var footer = this.replaceTabListItem(
      tabList.footer(),
      player,
      onlinePlayers,
      maxPlayers);

    this.playerTabList(player, header, footer);
  }

  protected boolean checkServiceGroup(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.currentLoginConfiguration == null) {
      return false;
    }

    return snapshot.configuration().groups().contains(this.currentLoginConfiguration.targetGroup());
  }

  public boolean checkPlayerMaintenance(@NonNull P player) {
    if (this.currentLoginConfiguration == null) {
      return false;
    }
    // check if the player is explicitly whitelisted
    var whitelist = this.currentLoginConfiguration.whitelist();
    if (whitelist.contains(this.playerName(player))
      || whitelist.contains(this.playerUniqueId(player).toString())) {
      return true;
    }

    return this.checkPlayerPermission(player, "cloudnet.syncproxy.maintenance");
  }

  public abstract @NonNull Collection<P> onlinePlayers();

  public abstract @NonNull String playerName(@NonNull P player);

  public abstract @NonNull UUID playerUniqueId(@NonNull P player);

  public abstract void playerTabList(@NonNull P player, @Nullable String header, @Nullable String footer);

  public abstract void disconnectPlayer(@NonNull P player, @NonNull String message);

  public abstract void messagePlayer(@NonNull P player, @Nullable String message);

  public abstract boolean checkPlayerPermission(@NonNull P player, @NonNull String permission);

  private @NonNull String replaceTabListItem(
    @NonNull String input,
    @NonNull P player,
    int onlinePlayers,
    int maxPlayers
  ) {
    input = input.replace("%time%", TIME_FORMATTER.format(LocalTime.now()))
      .replace("%syncproxy_online_players%", String.valueOf(onlinePlayers))
      .replace("%syncproxy_max_players%", String.valueOf(maxPlayers))
      .replace("%player_name%", this.playerName(player));
    return BridgeServiceHelper.fillCommonPlaceholders(input, null, this.serviceInfoHolder.serviceInfo());
  }
}
