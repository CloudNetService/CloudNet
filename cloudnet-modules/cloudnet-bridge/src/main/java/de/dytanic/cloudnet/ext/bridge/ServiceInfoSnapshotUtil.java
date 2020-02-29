package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetPlayerInfo;

import java.util.Collection;
import java.util.UUID;

public final class ServiceInfoSnapshotUtil {

    private ServiceInfoSnapshotUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the version (on Spigot {@code Bukkit.getVersion()}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the version of the server or {@code null} if the bridge plugin isn't loaded yet
     */
    public static String getVersion(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Version");
    }

    /**
     * Gets the online count (on Spigot {@code Bukkit.getOnlinePlayers().size()}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the onine count of the server or {@code 0} if the bridge plugin isn't loaded yet
     */
    public static int getOnlineCount(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getInt("Online-Count");
    }

    /**
     * Gets the sum of online counts on all services of a specific task.
     *
     * @param taskName the case-insensitive name of the task
     * @return the sum of all servers or {@code 0} if there is no server of that task online
     * @see #getOnlineCount(ServiceInfoSnapshot)
     * @see GeneralCloudServiceProvider#getCloudServices(String)
     */
    public static int getTaskOnlineCount(String taskName) {
        return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(taskName).stream()
                .mapToInt(ServiceInfoSnapshotUtil::getOnlineCount)
                .sum();
    }

    /**
     * Gets the sum of online counts on all services of a specific group.
     *
     * @param groupName the case-insensitive name of the group
     * @return the sum of all servers or {@code 0} if there is no server of that task online
     * @see #getOnlineCount(ServiceInfoSnapshot)
     * @see GeneralCloudServiceProvider#getCloudServicesByGroup(String)
     */
    public static int getGroupOnlineCount(String groupName) {
        return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesByGroup(groupName).stream()
                .mapToInt(ServiceInfoSnapshotUtil::getOnlineCount)
                .sum();
    }

    /**
     * Gets the max players (on Spigot {@code BukkitCloudNetHelper.getMaxPlayers()}, the default value is {@code Bukkit.getMaxPlayers()}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the max players of the server or {@code 0} if the bridge plugin isn't loaded yet
     */
    public static int getMaxPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    /**
     * Checks if the bridge plugin is loaded on a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return {@code true} if the bridge plugin is loaded or {@code false} otherwise
     */
    public static boolean isOnline(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getBoolean("Online");
    }

    /**
     * Checks if a specific server is empty.
     * Empty means, that no player is connected to it AND the server has to be online, if it isn't online, this returns {@code false}.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return {@code true} if the server is empty or {@code false} otherwise
     * @see #getOnlineCount(ServiceInfoSnapshot)
     * @see #isOnline(ServiceInfoSnapshot)
     */
    public static boolean isEmptyService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") == 0;
    }

    /**
     * Checks if a specific server is full.
     * Full means, that the online count is at least the max players count is connected to it AND
     * the server has to be online, if it isn't online, this returns {@code false}.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return {@code true} if the server is full or {@code false} otherwise
     * @see #getOnlineCount(ServiceInfoSnapshot)
     * @see #getMaxPlayers(ServiceInfoSnapshot)
     * @see #isOnline(ServiceInfoSnapshot)
     */
    public static boolean isFullService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().contains("Max-Players") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") >=
                        serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    /**
     * Checks if a specific server is starting.
     * Starting means, that the lifecycle is {@link ServiceLifeCycle#RUNNING} and it is not online.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return {@code true} if the server is starting or {@code false} otherwise
     * @see #isOnline(ServiceInfoSnapshot)
     * @see ServiceInfoSnapshot#getLifeCycle()
     */
    public static boolean isStartingService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING && !isOnline(serviceInfoSnapshot);
    }

    /**
     * Checks if a specific server is in game.
     * "In game" means, that the lifecycle is {@link ServiceLifeCycle#RUNNING}, the server is online and
     * the at least one of the following contains "ingame" or "running" (case-insensitive): Motd, Extra, State.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return {@code true} if the server is starting or {@code false} otherwise
     * @see #isOnline(ServiceInfoSnapshot)
     * @see ServiceInfoSnapshot#getLifeCycle()
     * @see #getMotd(ServiceInfoSnapshot)
     * @see #getExtra(ServiceInfoSnapshot)
     * @see #getState(ServiceInfoSnapshot)
     */
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

    /**
     * Gets the motd (on Spigot {@code BukkitCloudNetHelper.getApiMotd()}, the default value is {@code Bukkit.getMotd()}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the motd of the server or {@code null} if the bridge plugin isn't loaded yet
     */
    public static String getMotd(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Motd");
    }

    /**
     * Gets the state (on Spigot {@code BukkitCloudNetHelper.getState()}, the default value is {@code "LOBBY"}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the state of the server or {@code null} if the bridge plugin isn't loaded yet
     */
    public static String getState(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("State");
    }

    /**
     * Gets the extra (on Spigot {@code BukkitCloudNetHelper.getExtra()}, the default value is {@code ""}) of a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the extra of the server or {@code null} if the bridge plugin isn't loaded yet
     */
    public static String getExtra(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().getString("Extra");
    }

    /**
     * Gets the plugins on a specific server.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the plugins of the server or {@code null} if the bridge plugin isn't loaded yet
     */
    public static Collection<PluginInfo> getPlugins(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().get("Plugins", new TypeToken<Collection<PluginInfo>>() {
        }.getType());
    }

    /**
     * Gets the players on a specific server.
     * The {@link JsonDocument}s in the collection always contain a string with the uniqueId and the name of a player.
     * You can get the uniqueId of a player using {@link JsonDocument#get(String, Class)} with "uniqueId" as the key
     * and the class of {@link UUID} as the class.
     * <p>
     * What exactly the {@link JsonDocument} looks like depends on the environment of the given {@link ServiceInfoSnapshot}.
     * The specific classes are listed below.
     *
     * @param serviceInfoSnapshot the info of the server
     * @return the players of the server or {@code null} if the bridge plugin isn't loaded yet
     * @see BungeeCloudNetPlayerInfo
     * @see VelocityCloudNetPlayerInfo
     * @see BukkitCloudNetHelper
     * @see NukkitCloudNetHelper
     */
    public static Collection<JsonDocument> getPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getProperties().get("Players", new TypeToken<Collection<JsonDocument>>() {
        }.getType());
    }
}
