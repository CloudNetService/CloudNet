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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform;

import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyConfigurationUpdateEvent;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyLoginConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyMotd;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyTabList;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyTabListConfiguration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformSyncProxyManagement<P> implements SyncProxyManagement {

  private static final Random RANDOM = new Random();

  protected final Map<UUID, Integer> proxyOnlineCountCache = new ConcurrentHashMap<>();

  protected final RPCSender rpcSender;
  protected final IEventManager eventManager;

  protected SyncProxyConfiguration configuration;
  protected SyncProxyLoginConfiguration currentLoginConfiguration;
  protected SyncProxyTabListConfiguration currentTabListConfiguration;

  protected PlatformSyncProxyManagement() {
    Wrapper wrapper = Wrapper.getInstance();

    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), SyncProxyManagement.class);
    this.eventManager = wrapper.getEventManager();
    // cache all services that are already started
    wrapper.getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
      for (ServiceInfoSnapshot service : services) {
        this.cacheServiceInfoSnapshot(service);
      }
    });
  }

  protected void init() {
    this.setConfigurationSilently(this.rpcSender.invokeMethod("getConfiguration").fireSync());
  }

  public void setConfigurationSilently(@NotNull SyncProxyConfiguration configuration) {
    this.configuration = configuration;
    this.eventManager.callEvent(new SyncProxyConfigurationUpdateEvent(configuration));

    this.currentLoginConfiguration = configuration.getLoginConfigurations()
      .stream()
      .filter(loginConfiguration -> Wrapper.getInstance().getServiceConfiguration().getGroups()
        .contains(loginConfiguration.getTargetGroup()))
      .findFirst()
      .orElse(null);

    this.currentTabListConfiguration = configuration.getTabListConfigurations()
      .stream()
      .filter(tabListConfiguration -> Wrapper.getInstance().getServiceConfiguration().getGroups()
        .contains(tabListConfiguration.getTargetGroup()))
      .findFirst()
      .orElse(null);

    this.scheduleTabListUpdate();
    this.applyWhitelist();
  }

  @Override
  public @NotNull SyncProxyConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  public void setConfiguration(@NotNull SyncProxyConfiguration configuration) {
    this.rpcSender.invokeMethod("setConfiguration", configuration).fireSync();
  }

  public @Nullable SyncProxyMotd getRandomMotd() {
    if (this.currentLoginConfiguration == null) {
      return null;
    }

    List<SyncProxyMotd> motds =
      this.currentLoginConfiguration.isMaintenance()
        ? this.currentLoginConfiguration.getMaintenanceMotds()
        : this.currentLoginConfiguration.getMotds();

    if (motds.isEmpty()) {
      return null;
    }

    return motds.get(RANDOM.nextInt(motds.size()));
  }

  public void applyWhitelist() {
    // check if there is a configuration for this targetGroup
    if (this.currentLoginConfiguration == null) {
      return;
    }
    // check if the maintenance is enabled, if not we dont need to apply anything
    if (!this.currentLoginConfiguration.isMaintenance()) {
      return;
    }

    for (P onlinePlayer : this.getOnlinePlayers()) {
      // check if the player is allowed to join
      if (!this.checkPlayerMaintenance(onlinePlayer)) {
        this.disconnectPlayer(onlinePlayer, this.configuration.getMessage("player-login-not-whitelisted", null));
      }
    }
  }

  public @Nullable SyncProxyLoginConfiguration getCurrentLoginConfiguration() {
    return this.currentLoginConfiguration;
  }

  public @Nullable SyncProxyTabListConfiguration getCurrentTabListConfiguration() {
    return this.currentTabListConfiguration;
  }

  public int getOnlinePlayerCount() {
    return this.proxyOnlineCountCache.values().stream().mapToInt(value -> value).sum();
  }

  protected int getMaxPlayerCount() {
    if (this.currentLoginConfiguration == null) {
      return 0;
    }

    return this.currentLoginConfiguration.getMaxPlayers();
  }

  public void cacheServiceInfoSnapshot(@NotNull ServiceInfoSnapshot snapshot) {
    if (ServiceEnvironmentType.isMinecraftProxy(snapshot.getServiceId().getEnvironment())
      && this.checkServiceGroup(snapshot)) {
      this.proxyOnlineCountCache.put(snapshot.getServiceId().getUniqueId(),
        BridgeServiceProperties.MAX_PLAYERS.get(snapshot).orElse(0));
      this.updateTabList();
    }
  }

  public void removeCachedServiceInfoSnapshot(@NotNull ServiceInfoSnapshot snapshot) {
    if (this.proxyOnlineCountCache.remove(snapshot.getServiceId().getUniqueId()) != null) {
      this.updateTabList();
    }
  }

  public @Nullable String getServiceUpdateMessage(
    @NotNull String key,
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot
  ) {
    return this.configuration.getMessage(key, message -> message
      .replace("%service%", serviceInfoSnapshot.getName())
      .replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId()));
  }

  protected void scheduleTabListUpdate() {
    if (this.currentTabListConfiguration != null && !this.currentTabListConfiguration.getEntries().isEmpty()) {
      SyncProxyTabList tabList = this.currentTabListConfiguration.tick();

      this.schedule(this::scheduleTabListUpdate,
        (long) (1000D / this.currentTabListConfiguration.getAnimationsPerSecond()),
        TimeUnit.MILLISECONDS);

      this.updateTabList(tabList);
    }
  }

  public void updateTabList() {
    if (this.currentTabListConfiguration == null || this.currentTabListConfiguration.getEntries().isEmpty()) {
      return;
    }

    this.updateTabList(this.currentTabListConfiguration.getCurrentEntry());
  }

  protected void updateTabList(@NotNull SyncProxyTabList tabList) {
    int onlinePlayers = this.getOnlinePlayerCount();
    int maxPlayers = this.getMaxPlayerCount();
    for (P onlinePlayer : this.getOnlinePlayers()) {
      this.updateTabList(onlinePlayer, tabList, onlinePlayers, maxPlayers);
    }
  }

  protected void updateTabList(
    @NotNull P player,
    @NotNull SyncProxyTabList tabList,
    int onlinePlayers,
    int maxPlayers
  ) {
    String header = SyncProxyTabList.replaceTabListItem(tabList.getHeader(), this.getPlayerUniqueId(player),
      onlinePlayers, maxPlayers);
    String footer = SyncProxyTabList.replaceTabListItem(tabList.getFooter(), this.getPlayerUniqueId(player),
      onlinePlayers, maxPlayers);

    this.setPlayerTabList(player, header, footer);
  }

  protected boolean checkServiceGroup(@NotNull ServiceInfoSnapshot snapshot) {
    if (this.currentLoginConfiguration == null) {
      return false;
    }

    return snapshot.getConfiguration().getGroups().contains(this.currentLoginConfiguration.getTargetGroup());
  }

  public boolean checkPlayerMaintenance(@NotNull P player) {
    if (this.currentLoginConfiguration == null) {
      return false;
    }
    Set<String> whitelist = this.currentLoginConfiguration.getWhitelist();
    if (whitelist.contains(this.getPlayerName(player))
      || whitelist.contains(this.getPlayerUniqueId(player).toString())) {
      return true;
    }

    return this.checkPlayerPermission(player, "cloudnet.syncproxy.maintenance");
  }

  public abstract void schedule(@NotNull Runnable runnable, long time, @NotNull TimeUnit unit);

  public abstract @NotNull Collection<P> getOnlinePlayers();

  public abstract @NotNull String getPlayerName(@NotNull P player);

  public abstract @NotNull UUID getPlayerUniqueId(@NotNull P player);

  public abstract void setPlayerTabList(@NotNull P player, @Nullable String header, @Nullable String footer);

  public abstract void disconnectPlayer(@NotNull P player, @NotNull String message);

  public abstract void messagePlayer(@NotNull P player, @Nullable String message);

  public abstract boolean checkPlayerPermission(@NotNull P player, @NotNull String permission);

}
