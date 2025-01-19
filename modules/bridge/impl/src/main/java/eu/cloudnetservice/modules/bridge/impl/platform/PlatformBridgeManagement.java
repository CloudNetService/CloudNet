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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.impl.network.rpc.generation.RPCInternalInstanceFactory;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.config.ProxyFallback;
import eu.cloudnetservice.modules.bridge.config.ProxyFallbackConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeConfigurationUpdateEvent;
import eu.cloudnetservice.modules.bridge.impl.InternalBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.fallback.FallbackProfile;
import eu.cloudnetservice.modules.bridge.impl.platform.listener.PlatformChannelMessageListener;
import eu.cloudnetservice.modules.bridge.impl.platform.listener.PlatformInformationListener;
import eu.cloudnetservice.modules.bridge.impl.rpc.ComponentObjectSerializer;
import eu.cloudnetservice.modules.bridge.impl.rpc.TitleObjectSerializer;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import io.vavr.Tuple2;
import java.time.Duration;
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
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformBridgeManagement<P, I> implements InternalBridgeManagement {

  protected static final Predicate<ServiceInfoSnapshot> CONNECTED_SERVICE_TESTER = service -> service.connected()
    && service.lifeCycle() == ServiceLifeCycle.RUNNING
    && service.readProperty(BridgeDocProperties.IS_ONLINE);

  protected final RPCSender sender;
  protected final EventManager eventManager;
  protected final PlayerManager playerManager;
  protected final ServiceTaskProvider taskProvider;
  protected final ServiceInfoHolder serviceInfoHolder;
  protected final CloudServiceProvider serviceProvider;
  protected final BridgeServiceHelper bridgeServiceHelper;
  protected final NetworkServiceInfo ownNetworkServiceInfo;
  protected final WrapperConfiguration wrapperConfig;
  protected final LoadingCache<UUID, FallbackProfile> fallbackProfiles;
  protected final Map<UUID, ServiceInfoSnapshot> cachedServices;

  protected volatile ServiceTask selfTask;
  protected volatile BridgeConfiguration configuration;
  protected volatile ProxyFallbackConfiguration currentFallbackConfiguration;
  // cache utils
  protected volatile Predicate<ServiceInfoSnapshot> cacheTester;
  protected volatile Consumer<ServiceInfoSnapshot> cacheRegisterListener;
  protected volatile Consumer<ServiceInfoSnapshot> cacheUnregisterListener;

  public PlatformBridgeManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfig
  ) {
    this.eventManager = eventManager;
    this.taskProvider = taskProvider;
    this.bridgeServiceHelper = serviceHelper;
    this.serviceInfoHolder = serviceInfoHolder;
    this.serviceProvider = serviceProvider;
    this.wrapperConfig = wrapperConfig;
    this.cachedServices = new ConcurrentHashMap<>();
    this.fallbackProfiles = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(10))
      .build($ -> new FallbackProfile());
    // fill the cache access with no-op stuff
    this.cacheTester = $ -> false;
    this.cacheRegisterListener = this.cacheUnregisterListener = $ -> {
    };

    // init the rpc handler
    DefaultObjectMapper.DEFAULT_MAPPER
      .registerBinding(Title.class, new TitleObjectSerializer(), false)
      .registerBinding(Component.class, new ComponentObjectSerializer(), false);

    // init the player manager once
    this.sender = rpcFactory.newRPCSenderBuilder(BridgeManagement.class).targetComponent(networkClient).build();
    this.playerManager = rpcFactory.newRPCBasedImplementationBuilder(PlatformPlayerManager.class)
      .targetComponent(networkClient)
      .superclass(PlayerManager.class)
      .generateImplementation()
      .withAdditionalConstructorParameters(
        RPCInternalInstanceFactory.SpecialArg.RPC_SENDER,
        RPCInternalInstanceFactory.SpecialArg.CHANNEL_SUPPLIER)
      .allocate();

    // create the network service info of this service
    this.ownNetworkServiceInfo = NetworkServiceInfo.fromServiceInfoSnapshot(wrapperConfig.serviceInfoSnapshot());
    // load the configuration using rpc - all updates will be received from the channel message
    this.configurationSilently(this.sender.invokeMethod("configuration").fireSync());
    // register the common listeners
    eventManager.registerListener(new PlatformInformationListener(this));
    eventManager.registerListener(new PlatformChannelMessageListener(this.eventManager, this));
  }

  @Override
  public @NonNull BridgeConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public void configuration(@NonNull BridgeConfiguration configuration) {
    this.sender.invokeCaller(configuration).fireSync();
  }

  public void configurationSilently(@NonNull BridgeConfiguration configuration) {
    this.configuration = configuration;
    this.eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
    this.currentFallbackConfiguration = configuration.fallbackConfigurations().stream()
      .filter(config -> this.wrapperConfig.serviceConfiguration().groups().contains(config.targetGroup()))
      .findFirst()
      .orElse(null);
  }

  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    configureEvent.propertyHolder().append("Online", Boolean.TRUE);
    configureEvent.propertyHolder().append("Motd", this.bridgeServiceHelper.motd().get());
    configureEvent.propertyHolder().append("Extra", this.bridgeServiceHelper.extra().get());
    configureEvent.propertyHolder().append("State", this.bridgeServiceHelper.state().get());
    configureEvent.propertyHolder().append("Max-Players", this.bridgeServiceHelper.maxPlayers().get());
  }

  public @NonNull Collection<ServiceInfoSnapshot> cachedServices() {
    return this.cachedServices.values();
  }

  public @Nullable ServiceTask selfTask() {
    return this.selfTask;
  }

  public void handleTaskUpdate(@NonNull String name, @Nullable ServiceTask task) {
    if (this.wrapperConfig.serviceConfiguration().serviceId().taskName().equals(name)) {
      this.selfTask = task;
    }
  }

  public @NonNull Optional<ServiceInfoSnapshot> cachedService(@NonNull Predicate<ServiceInfoSnapshot> filter) {
    return this.cachedServices.values().stream().filter(filter).findFirst();
  }

  public @NonNull Optional<ServiceInfoSnapshot> cachedService(@NonNull UUID uniqueId) {
    return Optional.ofNullable(this.cachedServices.get(uniqueId));
  }

  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot) {
    // if the service is not yet cached check if we need to cache it
    if (!this.cachedServices.containsKey(snapshot.serviceId().uniqueId())) {
      // check if we should cache it
      if (this.cacheTester.test(snapshot)) {
        this.cacheRegisterListener.accept(snapshot);
        this.cachedServices.put(snapshot.serviceId().uniqueId(), snapshot);
      }
    } else {
      // if the service is already cached we need to check if we should still cache it
      if (this.cacheTester.test(snapshot)) {
        this.cachedServices.replace(snapshot.serviceId().uniqueId(), snapshot);
      } else {
        this.cacheUnregisterListener.accept(snapshot);
        this.cachedServices.remove(snapshot.serviceId().uniqueId());
      }
    }
  }

  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull UUID playerId,
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NonNull Function<String, Boolean> permissionTester
  ) {
    // get the currently applying fallback config, if no config applies to the current group then we don't need to filter
    // out a fallback for the player
    var config = this.currentFallbackConfiguration;
    if (config == null) {
      return Optional.empty();
    }

    // get the fallback profile for the player
    var profile = this.fallbackProfiles.get(playerId);
    // search for the best fallback
    return this.possibleFallbacks(currentServerName, virtualHost, permissionTester)
      // get all services we have cached of the task
      .map(fallback -> new Tuple2<>(fallback, this.anyTaskService(fallback.task(), profile, currentServerName)))
      // filter out all fallbacks that have no services
      .filter(possibility -> possibility._2().isPresent())
      // get the first possibility with the highest priority
      .min(Comparator.comparing(Tuple2::_1))
      // extract the target service
      .map(Tuple2::_2)
      // add the service to the tried ones
      .map(service -> {
        // we cannot flat-map because of the orElseGet
        service.ifPresent(ser -> profile.selectService(ser.name()));
        return service;
      }).orElseGet(() -> {
        // check if the configuration has a default fallback task
        if (config.defaultFallbackTask() == null) {
          return Optional.empty();
        }
        // get any service associated with the task
        return this.anyTaskService(config.defaultFallbackTask(), profile, currentServerName)
          .map(service -> {
            // select as the service we are connecting to
            profile.selectService(service.name());
            return service;
          });
      });
  }

  public @NonNull Stream<ProxyFallback> possibleFallbacks(
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NonNull Function<String, Boolean> permissionTester
  ) {
    // get the currently applying fallback config, if none is present for the group then do nothing
    var config = this.currentFallbackConfiguration;
    if (config == null) {
      return Stream.empty();
    }

    // get all groups of the service the player is currently on
    var currentGroups = this.cachedService(service -> service.name().equals(currentServerName))
      .map(service -> service.configuration().groups())
      .orElse(Collections.emptySet());
    // find all matching fallback configurations
    return config.fallbacks().stream()
      // check if a forced host is required
      .filter(fallback -> fallback.forcedHost() == null
        || (currentServerName == null && fallback.forcedHost().equalsIgnoreCase(virtualHost)))
      // check if the player has the permission to connect to the fallback
      .filter(fallback -> fallback.permission() == null || permissionTester.apply(fallback.permission()))
      // check if the fallback is available from the current group the player is on
      .filter(fallback -> fallback.availableOnGroups().isEmpty()
        || fallback.availableOnGroups().stream().anyMatch(currentGroups::contains));
  }

  public boolean isOnAnyFallbackInstance(
    @Nullable String currentServerName,
    @Nullable String virtualHost,
    @NonNull Function<String, Boolean> permissionTester
  ) {
    // get the currently applying fallback config, if it is null return false as we don't know if the player is on a fallback
    var config = this.currentFallbackConfiguration;
    if (config == null) {
      return false;
    }

    // check if the current server of the player is given
    return this.cachedService(service -> service.name().equals(currentServerName))
      .map(service -> {
        // check if the configuration has a default fallback task
        if (config.defaultFallbackTask() != null
          && service.serviceId().taskName().equals(config.defaultFallbackTask())) {
          return true;
        }
        // check if the player is on any fallback configuration
        return this.possibleFallbacks(currentServerName, virtualHost, permissionTester)
          .anyMatch(fallback -> service.serviceId().taskName().equals(fallback.task()));
      }).orElse(false);
  }

  protected @NonNull Optional<ServiceInfoSnapshot> anyTaskService(
    @NonNull String task,
    @NonNull FallbackProfile profile,
    @Nullable String currentServerName
  ) {
    return this.cachedServices.values().stream()
      // check if the service is associated with the task of the fallback
      .filter(service -> service.serviceId().taskName().equals(task))
      // check if the player failed to connect to that fallback during the current iteration
      .filter(service -> !profile.hasTried(service.name()))
      // check if the service is marked as joinable
      .filter(service -> service.connected() && service.readProperty(BridgeDocProperties.IS_ONLINE))
      // check if the player is not currently connected to that service
      .filter(service -> currentServerName == null || !service.name().equals(currentServerName))
      // find the service with the lowest player count known to use
      .min((optionA, optionB) -> {
        var playersOnOptionA = optionA.readProperty(BridgeDocProperties.ONLINE_COUNT);
        var playersOnOptionB = optionB.readProperty(BridgeDocProperties.ONLINE_COUNT);
        // compare the player count
        return Integer.compare(playersOnOptionA, playersOnOptionB);
      });
  }

  public void handleFallbackConnectionSuccess(@NonNull UUID uniqueId) {
    // if present clear the profile
    var profile = this.fallbackProfiles.getIfPresent(uniqueId);
    if (profile != null) {
      profile.reset();
    }
  }

  public void removeFallbackProfile(@NonNull UUID uniqueId) {
    this.fallbackProfiles.invalidate(uniqueId);
  }

  @Override
  public void postInit() {
    // publish a service update to append all property information
    this.serviceInfoHolder.publishServiceInfoUpdate();
    // load all services and cache the ones which are matching the cache policy
    this.serviceProvider.servicesAsync().thenAccept(services -> {
      for (var service : services) {
        this.handleServiceUpdate(service);
      }
    });
    // get the service task associated with this service if present
    this.selfTask = this.taskProvider.serviceTask(this.wrapperConfig.serviceConfiguration().serviceId().taskName());
  }

  public @NonNull NetworkServiceInfo ownNetworkServiceInfo() {
    return this.ownNetworkServiceInfo;
  }

  public abstract @NonNull ServicePlayer wrapPlayer(@NonNull P player);

  public abstract @NonNull I createPlayerInformation(@NonNull P player);

  public abstract @NonNull BiFunction<P, String, Boolean> permissionFunction();

  public abstract boolean isOnAnyFallbackInstance(@NonNull P player);

  public abstract @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull P player);

  public abstract @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull P player, @Nullable String currServer);

  public abstract void handleFallbackConnectionSuccess(@NonNull P player);

  public abstract void removeFallbackProfile(@NonNull P player);

  public abstract @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId);
}
