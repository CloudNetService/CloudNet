package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

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

    public static int getMaxPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    public static boolean isOnline(ServiceInfoSnapshot serviceInfoSnapshot) {

        return serviceInfoSnapshot.getProperties().getBoolean("Online");
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
