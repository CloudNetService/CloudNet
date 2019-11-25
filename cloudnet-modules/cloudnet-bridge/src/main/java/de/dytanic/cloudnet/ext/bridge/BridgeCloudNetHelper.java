package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BridgeCloudNetHelper {

    private BridgeCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void changeToIngame(Consumer<String> stateChanger) {
        stateChanger.accept("INGAME");
        BridgeHelper.updateServiceInfo();

        String task = Wrapper.getInstance().getServiceId().getTaskName();

        CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTaskAsync(task).onComplete(serviceTask -> {
            if (serviceTask != null) {
                CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(serviceTask).onComplete(serviceInfoSnapshot -> {
                    if (serviceInfoSnapshot != null) {
                        CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).start();
                    }
                });
            }
        });
    }


    public static String filterServiceForPlayer(String currentServer, BiFunction<String, String, List<Map.Entry<String, ServiceInfoSnapshot>>> filteredEntries,
                                                Predicate<String> permissionCheck) {
        AtomicReference<String> server = new AtomicReference<>();

        BridgeConfigurationProvider.load().getBungeeFallbackConfigurations().stream()
                .filter(
                        proxyFallbackConfiguration ->
                                proxyFallbackConfiguration.getTargetGroup() != null &&
                                        Arrays.asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups())
                                                .contains(proxyFallbackConfiguration.getTargetGroup())
                )
                .forEach(configuration -> {
                    List<ProxyFallback> proxyFallbacks = configuration.getFallbacks();
                    Collections.sort(proxyFallbacks);

                    for (ProxyFallback proxyFallback : proxyFallbacks) {
                        if (server.get() != null)
                            break;
                        if (proxyFallback.getTask() == null || (proxyFallback.getPermission() != null && !permissionCheck.test(proxyFallback.getPermission()))) {
                            continue;
                        }

                        filteredEntries.apply(proxyFallback.getTask(), currentServer)
                                .stream()
                                .map(Map.Entry::getValue).min(Comparator.comparingInt(ServiceInfoSnapshotUtil::getOnlineCount))
                                .ifPresent(serviceInfoSnapshot -> server.set(serviceInfoSnapshot.getServiceId().getName()));
                    }

                    if (server.get() == null) {
                        filteredEntries.apply(configuration.getDefaultFallbackTask(), currentServer)
                                .stream()
                                .map(Map.Entry::getValue).min(Comparator.comparingInt(ServiceInfoSnapshotUtil::getOnlineCount))
                                .ifPresent(serviceInfoSnapshot -> server.set(serviceInfoSnapshot.getServiceId().getName()));
                    }
                });

        return server.get();
    }

    public static boolean isFallbackService(ServiceInfoSnapshot serviceInfoSnapshot) {
        for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations()) {
            if (bungeeFallbackConfiguration.getTargetGroup() != null && Iterables.contains(
                    bungeeFallbackConfiguration.getTargetGroup(),
                    Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
            )) {
                for (ProxyFallback bungeeFallback : bungeeFallbackConfiguration.getFallbacks()) {
                    if (bungeeFallback.getTask() != null && serviceInfoSnapshot.getServiceId().getTaskName().equals(bungeeFallback.getTask())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
