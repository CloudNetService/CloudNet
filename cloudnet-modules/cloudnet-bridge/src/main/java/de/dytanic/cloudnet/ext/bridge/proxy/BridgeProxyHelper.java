package de.dytanic.cloudnet.ext.bridge.proxy;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BridgeProxyHelper {

    public static final Map<String, ServiceInfoSnapshot> SERVICE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerFallbackProfile> PROFILES = new ConcurrentHashMap<>();

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

    public static ServiceInfoSnapshot getCachedServiceInfoSnapshot(String name) {
        return SERVICE_CACHE.get(name);
    }

    public static void cacheServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        SERVICE_CACHE.put(serviceInfoSnapshot.getName(), serviceInfoSnapshot);
    }

    public static void removeCachedServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
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
                        fallbacks.add(new ProxyFallback(proxyFallbackConfiguration.getDefaultFallbackTask(), null, Integer.MIN_VALUE));
                    }
                    return fallbacks.stream();
                });
    }

    public static Stream<ProxyFallback> filterPlayerFallbacks(@NotNull UUID uniqueId, @Nullable String currentServer, @NotNull Predicate<String> permissionTester) {
        ServiceInfoSnapshot playerService = currentServer == null ? null : SERVICE_CACHE.get(currentServer);
        Collection<String> playerServiceGroups = playerService == null ? new ArrayList<>() : Arrays.asList(playerService.getConfiguration().getGroups());

        return getFallbacks()
                .filter(proxyFallback -> proxyFallback.getPermission() == null || permissionTester.test(proxyFallback.getPermission()))
                .filter(proxyFallback -> proxyFallback.getAvailableOnGroups() == null
                        || proxyFallback.getAvailableOnGroups().isEmpty()
                        || proxyFallback.getAvailableOnGroups().stream().anyMatch(playerServiceGroups::contains));
    }

    public static Optional<ServiceInfoSnapshot> getNextFallback(@NotNull UUID uniqueId, @Nullable String currentServer, @NotNull Predicate<String> permissionTester) {
        PlayerFallbackProfile profile = PROFILES.computeIfAbsent(uniqueId, uuid -> new PlayerFallbackProfile());

        return filterPlayerFallbacks(uniqueId, currentServer, permissionTester)
                .flatMap(proxyFallback -> getCachedServiceInfoSnapshots(proxyFallback.getTask()).map(serviceInfoSnapshot -> new PlayerFallback(proxyFallback.getPriority(), serviceInfoSnapshot)))

                .filter(fallback -> fallback.getTarget().isConnected() && fallback.getTarget().getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false))
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

    public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(UUID uniqueId, String currentServer,
                                                                           Predicate<String> permissionTester,
                                                                           Function<ServiceInfoSnapshot, CompletableFuture<Boolean>> serverConnector) {
        BridgeProxyHelper.startConnecting(uniqueId);
        CompletableFuture<ServiceInfoSnapshot> future = new CompletableFuture<>();
        Optional<ServiceInfoSnapshot> optionalFallback = getNextFallback(uniqueId, currentServer, permissionTester);
        if (optionalFallback.isPresent()) {
            tryFallback(uniqueId, optionalFallback.get(), currentServer, future, permissionTester, serverConnector);
        } else {
            BridgeProxyHelper.clearFallbackProfile(uniqueId);
            future.complete(null);
        }
        return future;
    }

    private static void tryFallback(UUID uniqueId, ServiceInfoSnapshot serviceInfoSnapshot, String currentServer,
                                    CompletableFuture<ServiceInfoSnapshot> future,
                                    Predicate<String> permissionTester,
                                    Function<ServiceInfoSnapshot, CompletableFuture<Boolean>> serverConnector) {
        serverConnector.apply(serviceInfoSnapshot).thenAccept(success -> {
            if (!success) {
                BridgeProxyHelper.handleConnectionFailed(uniqueId, serviceInfoSnapshot.getName());
                Optional<ServiceInfoSnapshot> optionalNewFallback = BridgeProxyHelper.getNextFallback(uniqueId, currentServer, permissionTester);
                optionalNewFallback.ifPresent(newFallback -> tryFallback(uniqueId, newFallback, currentServer, future, permissionTester, serverConnector));
                if (!optionalNewFallback.isPresent()) {
                    BridgeProxyHelper.clearFallbackProfile(uniqueId);
                    future.complete(null);
                }
            } else {
                future.complete(serviceInfoSnapshot);
            }
        });
    }

}
