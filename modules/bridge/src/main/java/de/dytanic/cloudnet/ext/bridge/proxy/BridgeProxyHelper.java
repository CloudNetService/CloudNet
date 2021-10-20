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

package de.dytanic.cloudnet.ext.bridge.proxy;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BridgeProxyHelper {

  public static final Map<String, ServiceInfoSnapshot> SERVICE_CACHE = new ConcurrentHashMap<>();
  private static final Map<UUID, PlayerFallbackProfile> PROFILES = new ConcurrentHashMap<>();

  private static volatile int maxPlayers;

  private BridgeProxyHelper() {
    throw new UnsupportedOperationException();
  }

  public static Collection<ServiceInfoSnapshot> getCachedServiceInfoSnapshots() {
    return SERVICE_CACHE.values();
  }

  public static Stream<ServiceInfoSnapshot> getCachedServiceInfoSnapshots(String task) {
    return getCachedServiceInfoSnapshots().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equals(task));
  }

  public static ServiceInfoSnapshot getCachedServiceInfoSnapshot(@NotNull String name) {
    return SERVICE_CACHE.get(name);
  }

  public static void cacheServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    SERVICE_CACHE.put(serviceInfoSnapshot.getName(), serviceInfoSnapshot);
  }

  public static void removeCachedServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    SERVICE_CACHE.remove(serviceInfoSnapshot.getName());
  }

  public static void handleConnectionFailed(UUID uniqueId, String serviceName) {
    if (PROFILES.containsKey(uniqueId)) {
      PROFILES.get(uniqueId).addKick(serviceName);
    }
  }

  public static void startConnecting(UUID uniqueId) {
    PROFILES.put(uniqueId, new PlayerFallbackProfile());
  }

  public static void clearFallbackProfile(UUID uniqueId) {
    PROFILES.remove(uniqueId);
  }

  public static Stream<ProxyFallbackConfiguration> getProxyFallbackConfigurations() {
    return BridgeConfigurationProvider.load().getBungeeFallbackConfigurations().stream()
      .filter(proxyFallbackConfiguration -> proxyFallbackConfiguration.getTargetGroup() != null &&
        Arrays.asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups())
          .contains(proxyFallbackConfiguration.getTargetGroup())
      );
  }

  public static Stream<ProxyFallback> getFallbacks() {
    return getProxyFallbackConfigurations()
      .flatMap(proxyFallbackConfiguration -> {
        List<ProxyFallback> fallbacks = new ArrayList<>(proxyFallbackConfiguration.getFallbacks());
        if (proxyFallbackConfiguration.getDefaultFallbackTask() != null) {
          fallbacks
            .add(new ProxyFallback(proxyFallbackConfiguration.getDefaultFallbackTask(), null, Integer.MIN_VALUE));
        }
        return fallbacks.stream();
      });
  }

  public static Stream<ProxyFallback> filterPlayerFallbacks(@NotNull UUID uniqueId,
    @Nullable String currentServer,
    @NotNull Predicate<String> permissionTester) {
    return filterPlayerFallbacks(uniqueId, currentServer, null, permissionTester);
  }

  public static Stream<ProxyFallback> filterPlayerFallbacks(@NotNull UUID uniqueId,
    @Nullable String currentServer,
    @Nullable String virtualHost,
    @NotNull Predicate<String> permissionTester) {
    ServiceInfoSnapshot currentService = currentServer == null ? null : SERVICE_CACHE.get(currentServer);
    Collection<String> serviceGroups =
      currentService == null ? new ArrayList<>() : Arrays.asList(currentService.getConfiguration().getGroups());

    return getFallbacks()
      .filter(proxyFallback -> proxyFallback.getForcedHost() == null || (currentService == null && proxyFallback
        .getForcedHost().equalsIgnoreCase(virtualHost)))
      .filter(
        proxyFallback -> proxyFallback.getPermission() == null || permissionTester.test(proxyFallback.getPermission()))
      .filter(proxyFallback -> proxyFallback.getAvailableOnGroups() == null
        || proxyFallback.getAvailableOnGroups().isEmpty()
        || proxyFallback.getAvailableOnGroups().stream().anyMatch(serviceGroups::contains));
  }

  public static Optional<ServiceInfoSnapshot> getNextFallback(@NotNull UUID uniqueId,
    @Nullable String currentServer,
    @NotNull Predicate<String> permissionTester) {
    return getNextFallback(uniqueId, currentServer, null, permissionTester);
  }

  public static Optional<ServiceInfoSnapshot> getNextFallback(@NotNull UUID uniqueId,
    @Nullable String currentServer,
    @Nullable String virtualHost,
    @NotNull Predicate<String> permissionTester) {
    PlayerFallbackProfile profile = PROFILES.computeIfAbsent(uniqueId, uuid -> new PlayerFallbackProfile());

    return filterPlayerFallbacks(uniqueId, currentServer, virtualHost, permissionTester)
      .flatMap(proxyFallback -> getCachedServiceInfoSnapshots(proxyFallback.getTask())
        .map(serviceInfoSnapshot -> new PlayerFallback(proxyFallback.getPriority(), serviceInfoSnapshot)))

      .filter(fallback -> fallback.getTarget().isConnected() && fallback.getTarget()
        .getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false))
      .filter(fallback -> !fallback.getTarget().getName().equals(currentServer))
      .filter(fallback -> profile.canConnect(fallback.getTarget()))

      .min(PlayerFallback::compareTo)
      .map(PlayerFallback::getTarget);
  }

  public static boolean isFallbackService(String name) {
    return isFallbackService(getCachedServiceInfoSnapshot(name));
  }

  public static boolean isFallbackService(@Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot != null && getFallbacks()
      .filter(proxyFallback -> proxyFallback.getTask() != null)
      .anyMatch(proxyFallback -> proxyFallback.getTask().equals(serviceInfoSnapshot.getServiceId().getTaskName()));
  }

  public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(UUID uniqueId,
    String currentServer,
    Predicate<String> permissionTester,
    Function<ServiceInfoSnapshot, CompletableFuture<Boolean>> serverConnector) {
    return connectToFallback(uniqueId, currentServer, null, permissionTester, serverConnector);
  }

  public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(UUID uniqueId,
    String currentServer,
    String virtualHost,
    Predicate<String> permissionTester,
    Function<ServiceInfoSnapshot, CompletableFuture<Boolean>> serverConnector) {
    BridgeProxyHelper.startConnecting(uniqueId);
    CompletableFuture<ServiceInfoSnapshot> future = new CompletableFuture<>();
    Optional<ServiceInfoSnapshot> optionalFallback = getNextFallback(uniqueId, currentServer, virtualHost,
      permissionTester);
    if (optionalFallback.isPresent()) {
      tryFallback(uniqueId, optionalFallback.get(), currentServer, virtualHost, future, permissionTester,
        serverConnector);
    } else {
      BridgeProxyHelper.clearFallbackProfile(uniqueId);
      future.complete(null);
    }
    return future;
  }

  private static void tryFallback(UUID uniqueId,
    ServiceInfoSnapshot serviceInfoSnapshot, String currentServer,
    String virtualHost,
    CompletableFuture<ServiceInfoSnapshot> future,
    Predicate<String> permissionTester,
    Function<ServiceInfoSnapshot, CompletableFuture<Boolean>> serverConnector) {
    serverConnector.apply(serviceInfoSnapshot).thenAccept(success -> {
      if (!success) {
        BridgeProxyHelper.handleConnectionFailed(uniqueId, serviceInfoSnapshot.getName());
        Optional<ServiceInfoSnapshot> optionalNewFallback = BridgeProxyHelper
          .getNextFallback(uniqueId, currentServer, virtualHost, permissionTester);
        optionalNewFallback.ifPresent(
          newFallback -> tryFallback(uniqueId, newFallback, currentServer, virtualHost, future, permissionTester,
            serverConnector));
        if (!optionalNewFallback.isPresent()) {
          BridgeProxyHelper.clearFallbackProfile(uniqueId);
          future.complete(null);
        }
      } else {
        future.complete(serviceInfoSnapshot);
      }
    });
  }

  public static int getMaxPlayers() {
    return BridgeProxyHelper.maxPlayers;
  }

  public static void setMaxPlayers(int maxPlayers) {
    BridgeProxyHelper.maxPlayers = maxPlayers;
  }
}
