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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.config.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.config.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.platform.fallback.FallbackProfile;
import de.dytanic.cloudnet.ext.bridge.platform.listener.PlatformChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.platform.listener.PlatformInformationListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.rpc.ComponentObjectSerializer;
import de.dytanic.cloudnet.ext.bridge.rpc.TitleObjectSerializer;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformBridgeManagement<P, I> implements BridgeManagement {

  protected static final Predicate<ServiceInfoSnapshot> CONNECTED_SERVICE_TESTER = service -> service.isConnected()
    && service.getLifeCycle() == ServiceLifeCycle.RUNNING
    && BridgeServiceProperties.IS_ONLINE.get(service).orElse(false);

  protected final RPCSender sender;
  protected final IEventManager eventManager;
  protected final IPlayerManager playerManager;
  protected final NetworkServiceInfo ownNetworkServiceInfo;
  protected final Map<UUID, FallbackProfile> fallbackProfiles;
  protected final Map<UUID, ServiceInfoSnapshot> cachedServices;

  protected volatile ServiceTask selfTask;
  protected volatile BridgeConfiguration configuration;
  protected volatile ProxyFallbackConfiguration currentFallbackConfiguration;
  // cache utils
  protected volatile Predicate<ServiceInfoSnapshot> cacheTester;
  protected volatile Consumer<ServiceInfoSnapshot> cacheRegisterListener;
  protected volatile Consumer<ServiceInfoSnapshot> cacheUnregisterListener;

  public PlatformBridgeManagement(@NotNull Wrapper wrapper) {
    this.eventManager = wrapper.getEventManager();
    this.cachedServices = new ConcurrentHashMap<>();
    this.fallbackProfiles = new ConcurrentHashMap<>();
    // fill the cache access with no-op stuff
    this.cacheTester = $ -> false;
    this.cacheRegisterListener = this.cacheUnregisterListener = $ -> {
    };
    // init the rpc handler
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(Title.class, new TitleObjectSerializer(), false);
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(Component.class, new ComponentObjectSerializer(), false);
    // init the player manager once
    this.playerManager = new PlatformPlayerManager(wrapper);
    this.sender = wrapper.getRPCProviderFactory().providerForClass(wrapper.getNetworkClient(), BridgeManagement.class);
    // create the network service info of this service
    this.ownNetworkServiceInfo = BridgeServiceHelper.createServiceInfo(wrapper.getCurrentServiceInfoSnapshot());
    // load the configuration using rpc - all updates will be received from the channel message
    this.setConfigurationLocally(this.sender.invokeMethod("getConfiguration").fireSync());
    // register the common listeners
    wrapper.getEventManager().registerListener(new PlatformInformationListener(this));
    wrapper.getEventManager().registerListener(new PlatformChannelMessageListener(this.eventManager, this));
  }

  @Override
  public @NotNull BridgeConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  public void setConfiguration(@NotNull BridgeConfiguration configuration) {
    this.sender.invokeMethod("setConfiguration", configuration).fireSync();
  }

  public void setConfigurationLocally(@NotNull BridgeConfiguration configuration) {
    this.configuration = configuration;
    this.eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
    this.currentFallbackConfiguration = configuration.getFallbackConfigurations().stream()
      .filter(config -> Wrapper.getInstance().getServiceConfiguration().getGroups().contains(config.getTargetGroup()))
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull IPlayerManager getPlayerManager() {
    return this.playerManager;
  }

  public void appendServiceInformation(@NotNull ServiceInfoSnapshot snapshot) {
    snapshot.getProperties().append("Online", Boolean.TRUE);
    snapshot.getProperties().append("Motd", BridgeServiceHelper.MOTD.get());
    snapshot.getProperties().append("Extra", BridgeServiceHelper.EXTRA.get());
    snapshot.getProperties().append("State", BridgeServiceHelper.STATE.get());
    snapshot.getProperties().append("Max-Players", BridgeServiceHelper.MAX_PLAYERS.get());
  }

  public @NotNull Collection<ServiceInfoSnapshot> getCachedServices() {
    return this.cachedServices.values();
  }

  public @Nullable ServiceTask getSelfTask() {
    return this.selfTask;
  }

  public void handleTaskUpdate(@NotNull String name, @Nullable ServiceTask task) {
    if (Wrapper.getInstance().getServiceId().getTaskName().equals(name)) {
      this.selfTask = task;
    }
  }

  public @NotNull Optional<ServiceInfoSnapshot> getCachedService(@NotNull Predicate<ServiceInfoSnapshot> filter) {
    return this.cachedServices.values().stream().filter(filter).findFirst();
  }

  public @NotNull Optional<ServiceInfoSnapshot> getCachedService(@NotNull UUID uniqueId) {
    return Optional.ofNullable(this.cachedServices.get(uniqueId));
  }

  public void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot) {
    // if the service is not yet cached check if we need to cache it
    if (!this.cachedServices.containsKey(snapshot.getServiceId().getUniqueId())) {
      // check if we should cache it
      if (this.cacheTester.test(snapshot)) {
        this.cacheRegisterListener.accept(snapshot);
        this.cachedServices.put(snapshot.getServiceId().getUniqueId(), snapshot);
      }
    } else {
      // if the service is already cached we need to check if we should still cache it
      if (this.cacheTester.test(snapshot)) {
        this.cachedServices.replace(snapshot.getServiceId().getUniqueId(), snapshot);
      } else {
        this.cacheUnregisterListener.accept(snapshot);
        this.cachedServices.remove(snapshot.getServiceId().getUniqueId());
      }
    }
  }

  public @NotNull Optional<ServiceInfoSnapshot> getFallback(
    @NotNull UUID playerId,
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NotNull Function<String, Boolean> permissionTester
  ) {
    // get the currently applying fallback config
    var config = Preconditions.checkNotNull(this.currentFallbackConfiguration);
    // get the fallback profile for the player
    var profile = this.fallbackProfiles.computeIfAbsent(playerId, $ -> new FallbackProfile());
    // search for the best fallback
    return this.getPossibleFallbacks(currentServerName, virtualHost, permissionTester)
      // get all services we have cached of the task
      .map(fallback -> new Pair<>(fallback, this.getAnyTaskService(fallback.getTask(), profile, currentServerName)))
      // filter out all fallbacks that have no services
      .filter(possibility -> !possibility.getSecond().isPresent())
      // get the first possibility with the highest priority
      .min(Comparator.comparing(Pair::getFirst))
      // extract the target service
      .map(Pair::getSecond)
      // add the service to the tried ones
      .map(service -> {
        // we cannot flat-map because of the orElseGet
        service.ifPresent(ser -> profile.selectService(ser.getName()));
        return service;
      }).orElseGet(() -> {
        // check if the configuration has a default fallback task
        if (config.getDefaultFallbackTask() == null) {
          return Optional.empty();
        }
        // get any service associated with the task
        return this.getAnyTaskService(config.getDefaultFallbackTask(), profile, currentServerName)
          .map(service -> {
            // select as the service we are connecting to
            profile.selectService(service.getName());
            return service;
          });
      });
  }

  public @NotNull Stream<ProxyFallback> getPossibleFallbacks(
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NotNull Function<String, Boolean> permissionTester
  ) {
    // get the currently applying fallback config
    var config = Preconditions.checkNotNull(this.currentFallbackConfiguration);
    // get all groups of the service the player is currently on
    var currentGroups = this.getCachedService(service -> service.getName().equals(currentServerName))
      .map(service -> service.getConfiguration().getGroups())
      .orElse(Collections.emptySet());
    // find all matching fallback configurations
    return config.getFallbacks().stream()
      // check if a forced host is required
      .filter(fallback -> fallback.getForcedHost() == null || fallback.getForcedHost().equals(virtualHost))
      // check if the player has the permission to connect to the fallback
      .filter(fallback -> fallback.getPermission() == null || permissionTester.apply(fallback.getPermission()))
      // check if the fallback is available from the current group the player is on
      .filter(fallback -> fallback.getAvailableOnGroups().isEmpty()
        || fallback.getAvailableOnGroups().stream().anyMatch(currentGroups::contains));
  }

  public boolean isOnAnyFallbackInstance(
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NotNull Function<String, Boolean> permissionTester
  ) {
    // check if the current server of the player is given
    return this.getCachedService(service -> service.getName().equals(currentServerName))
      .map(service -> this.getPossibleFallbacks(currentServerName, virtualHost, permissionTester)
        .anyMatch(fallback -> service.getServiceId().getTaskName().equals(fallback.getTask())))
      .orElse(false);
  }

  protected @NotNull Optional<ServiceInfoSnapshot> getAnyTaskService(
    @NotNull String task,
    @NotNull FallbackProfile profile,
    @Nullable String currentServerName
  ) {
    return this.cachedServices.values().stream()
      // check if the service is associated with the task of the fallback
      .filter(service -> service.getServiceId().getTaskName().equals(task))
      // check if the player failed to connect to that fallback during the current iteration
      .filter(service -> !profile.hasTried(service.getName()))
      // check if the service is marked as joinable
      .filter(service -> service.isConnected() && BridgeServiceProperties.IS_ONLINE.get(service).orElse(false))
      // check if the player is not currently connected to that service
      .filter(service -> currentServerName == null || !service.getName().equals(currentServerName))
      // find the service with the lowest player count known to use
      .min((optionA, optionB) -> {
        int playersOnOptionA = BridgeServiceProperties.ONLINE_COUNT.get(optionA).orElse(0);
        int playersOnOptionB = BridgeServiceProperties.ONLINE_COUNT.get(optionB).orElse(0);
        // compare the player count
        return Integer.compare(playersOnOptionA, playersOnOptionB);
      });
  }

  public void handleFallbackConnectionSuccess(@NotNull UUID uniqueId) {
    // if present clear the profile
    var profile = this.fallbackProfiles.get(uniqueId);
    if (profile != null) {
      profile.reset();
    }
  }

  public void removeFallbackProfile(@NotNull UUID uniqueId) {
    this.fallbackProfiles.remove(uniqueId);
  }

  @Override
  public void postInit() {
    // publish a service update to append all property information
    Wrapper.getInstance().publishServiceInfoUpdate();
    // load all services and cache the ones which are matching the cache policy
    CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
      for (var service : services) {
        this.handleServiceUpdate(service);
      }
    });
    // get the service task associated with this service if present
    this.selfTask = CloudNetDriver.getInstance().getServiceTaskProvider()
      .getServiceTask(Wrapper.getInstance().getServiceId().getTaskName());
  }

  public @NotNull NetworkServiceInfo getOwnNetworkServiceInfo() {
    return this.ownNetworkServiceInfo;
  }

  public abstract @NotNull ServicePlayer wrapPlayer(@NotNull P player);

  public abstract @NotNull I createPlayerInformation(@NotNull P player);

  public abstract @NotNull BiFunction<P, String, Boolean> getPermissionFunction();

  public abstract boolean isOnAnyFallbackInstance(@NotNull P player);

  public abstract @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull P player);

  public abstract @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull P player, @Nullable String currServer);

  public abstract void handleFallbackConnectionSuccess(@NotNull P player);

  public abstract void removeFallbackProfile(@NotNull P player);

  public abstract @NotNull PlayerExecutor getDirectPlayerExecutor(@NotNull UUID uniqueId);
}
