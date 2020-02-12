package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;

import java.util.Collection;

public final class ServiceInfoSnapshotUtil {

    private ServiceInfoSnapshotUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getVersion(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Version");
    }

    public static int getOnlineCount(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getInt("Online-Count");
    }

    public static int getTaskOnlineCount(String taskName) {
        return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(taskName).stream()
                .mapToInt(ServiceInfoSnapshotUtil::getOnlineCount)
                .sum();
    }

    public static int getGroupOnlineCount(String groupName) {
        return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesByGroup(groupName).stream()
                .mapToInt(ServiceInfoSnapshotUtil::getOnlineCount)
                .sum();
    }

    public static int getMaxPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    public static boolean isOnline(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getBoolean("Online");
    }

    public static boolean isEmptyService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") == 0;
    }

    public static boolean isFullService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().contains("Max-Players") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") >=
                        serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    public static boolean isStartingService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING && !serviceInfoSnapshot.getProperties().contains("Online");
    }

    public static boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING
                && serviceInfoSnapshot.isConnected()
                &&
                serviceInfoSnapshot.getProperties().getBoolean("Online")
                && (
                (serviceInfoSnapshot.getProperties().contains("Motd") &&
                        (
                                serviceInfoSnapshot.getProperties().getString("Motd").toLowerCase().contains("ingame") ||
                                        serviceInfoSnapshot.getProperties().getString("Motd").toLowerCase().contains("running")
                        )
                ) ||
                        (serviceInfoSnapshot.getProperties().contains("Extra") &&
                                (
                                        serviceInfoSnapshot.getProperties().getString("Extra").toLowerCase().contains("ingame") ||
                                                serviceInfoSnapshot.getProperties().getString("Extra").toLowerCase().contains("running")
                                )
                        ) ||
                        (serviceInfoSnapshot.getProperties().contains("State") &&
                                (
                                        serviceInfoSnapshot.getProperties().getString("State").toLowerCase().contains("ingame") ||
                                                serviceInfoSnapshot.getProperties().getString("State").toLowerCase().contains("running")
                                )
                        )
        );
    }

    public static String getMotd(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Motd");
    }

    public static String getState(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("State");
    }

    public static String getExtra(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Extra");
    }

    public static Collection<PluginInfo> getPlugins(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().get("Plugins", new TypeToken<Collection<PluginInfo>>() {
        }.getType());
    }

    public static Collection<JsonDocument> getPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().get("Players", new TypeToken<Collection<JsonDocument>>() {
        }.getType());
    }
}
