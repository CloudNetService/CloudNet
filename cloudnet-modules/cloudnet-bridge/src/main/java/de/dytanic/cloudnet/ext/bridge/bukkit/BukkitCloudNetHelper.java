package de.dytanic.cloudnet.ext.bridge.bukkit;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.WorldInfo;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public final class BukkitCloudNetHelper {

    private static volatile String
            apiMotd = Bukkit.getMotd(),
            extra = "",
            state = "LOBBY";

    private static volatile int
            maxPlayers = Bukkit.getMaxPlayers();

    private static JavaPlugin plugin;


    private BukkitCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void changeToIngame() {
        state = "INGAME";
        BridgeHelper.updateServiceInfo();

        String task = Wrapper.getInstance().getServiceId().getTaskName();

        if (!CloudNetDriver.getInstance().isServiceTaskPresent(task)) {
            CloudNetDriver.getInstance().getServiceTaskAsync(task).onComplete(serviceTask -> {
                if (serviceTask != null) {
                    CloudNetDriver.getInstance().createCloudServiceAsync(serviceTask).onComplete(serviceInfoSnapshot -> {
                        if (serviceInfoSnapshot != null) {
                            CloudNetDriver.getInstance().startCloudService(serviceInfoSnapshot);
                        }
                    });
                }
            });
        }
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        Collection<BukkitCloudNetPlayerInfo> players = Iterables.newArrayList();
        forEachPlayers(player -> {
            Location location = player.getLocation();

            players.add(new BukkitCloudNetPlayerInfo(
                    player.getUniqueId(),
                    player.getName(),
                    getHealthOfPlayer(player),
                    getMaxHealthOfPlayer(player),
                    player.getFoodLevel(),
                    player.getLevel(),
                    new WorldPosition(
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            location.getYaw(),
                            location.getPitch(),
                            location.getWorld().getName()
                    ),
                    new HostAndPort(player.getAddress())
            ));
        });

        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", Bukkit.getVersion())
                .append("Bukkit-Version", Bukkit.getBukkitVersion())
                .append("Online-Count", getOnlineCount())
                .append("Max-Players", maxPlayers)
                .append("Motd", apiMotd)
                .append("Extra", extra)
                .append("State", state)
                .append("Outgoing-Channels", Bukkit.getMessenger().getOutgoingChannels())
                .append("Incoming-Channels", Bukkit.getMessenger().getIncomingChannels())
                .append("Online-Mode", Bukkit.getOnlineMode())
                .append("Whitelist-Enabled", Bukkit.hasWhitelist())
                .append("Whitelist", Iterables.map(Bukkit.getWhitelistedPlayers(), offlinePlayer -> offlinePlayer.getName()))
                .append("Allow-Nether", Bukkit.getAllowNether())
                .append("Allow-End", Bukkit.getAllowEnd())
                .append("Players", players)
                .append("Plugins", Iterables.map(Arrays.asList(Bukkit.getPluginManager().getPlugins()), plugin -> {
                    PluginInfo pluginInfo = new PluginInfo(plugin.getName(), plugin.getDescription().getVersion());

                    pluginInfo.getProperties()
                            .append("authors", plugin.getDescription().getAuthors())
                            .append("dependencies", plugin.getDescription().getDepend())
                            .append("load-before", plugin.getDescription().getLoadBefore())
                            .append("description", plugin.getDescription().getDescription())
                            .append("commands", plugin.getDescription().getCommands())
                            .append("soft-dependencies", plugin.getDescription().getSoftDepend())
                            .append("website", plugin.getDescription().getWebsite())
                            .append("main-class", plugin.getDescription().getMain())
                            .append("prefix", plugin.getDescription().getPrefix())
                    ;

                    return pluginInfo;
                }))
                .append("Worlds", Iterables.map(Bukkit.getWorlds(), world -> {
                    Map<String, String> gameRules = Maps.newHashMap();

                    for (String entry : world.getGameRules()) {
                        gameRules.put(entry, world.getGameRuleValue(entry));
                    }

                    return new WorldInfo(world.getUID(), world.getName(), world.getDifficulty().name(), gameRules);
                }))
        ;
    }

    public static void forEachPlayers(Consumer<Player> consumer) {
        Method method;
        try {
            method = Server.class.getMethod("getOnlinePlayers");
            method.setAccessible(true);
            Object result = method.invoke(Bukkit.getServer());

            if (result instanceof Collection) {
                for (Object item : ((Collection) result)) {
                    consumer.accept((Player) item);
                }
            }

            if (result instanceof Player[]) {
                for (Player player : ((Player[]) result)) {
                    consumer.accept(player);
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
        Boolean onlineMode = Bukkit.getServer().getOnlineMode();
        if (onlineMode == null) {
            onlineMode = true;
        }

        return BridgeHelper.createNetworkConnectionInfo(
                player.getUniqueId(),
                player.getName(),
                -1,
                new HostAndPort(player.getAddress()),
                new HostAndPort("0.0.0.0", Bukkit.getServer().getPort()),
                onlineMode,
                false,
                new NetworkServiceInfo(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }

    public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(Player player, boolean login) {
        WorldPosition worldPosition;

        if (login) {
            worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
        } else {
            Location location = player.getLocation();
            worldPosition = new WorldPosition(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    location.getWorld().getName()
            );
        }

        return new NetworkPlayerServerInfo(
                player.getUniqueId(),
                player.getName(),
                null,
                getHealthOfPlayer(player),
                getMaxHealthOfPlayer(player),
                player.getSaturation(),
                player.getLevel(),
                worldPosition,
                new HostAndPort(player.getAddress()),
                new NetworkServiceInfo(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }

    public static int getOnlineCount() {
        Method method;
        try {
            method = Server.class.getMethod("getOnlinePlayers");
            method.setAccessible(true);
            Object result = method.invoke(Bukkit.getServer());

            if (result instanceof Collection) {
                return ((Collection) result).size();
            }

            if (result instanceof Player[]) {
                return ((Player[]) result).length;
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    public static double getHealthOfPlayer(Player player) {
        Validate.checkNotNull(player);

        try {

            Method method = LivingEntity.class.getMethod("getHealth");
            method.setAccessible(true);
            return ((Number) method.invoke(player)).doubleValue();

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 20D;
    }

    public static double getMaxHealthOfPlayer(Player player) {
        Validate.checkNotNull(player);

        try {

            Method method = LivingEntity.class.getMethod("getMaxHealth");
            method.setAccessible(true);
            return ((Number) method.invoke(player)).doubleValue();

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 20D;
    }

    public static String getApiMotd() {
        return BukkitCloudNetHelper.apiMotd;
    }

    public static void setApiMotd(String apiMotd) {
        BukkitCloudNetHelper.apiMotd = apiMotd;
    }

    public static String getExtra() {
        return BukkitCloudNetHelper.extra;
    }

    public static void setExtra(String extra) {
        BukkitCloudNetHelper.extra = extra;
    }

    public static String getState() {
        return BukkitCloudNetHelper.state;
    }

    public static void setState(String state) {
        BukkitCloudNetHelper.state = state;
    }

    public static int getMaxPlayers() {
        return BukkitCloudNetHelper.maxPlayers;
    }

    public static void setMaxPlayers(int maxPlayers) {
        BukkitCloudNetHelper.maxPlayers = maxPlayers;
    }

    public static JavaPlugin getPlugin() {
        return BukkitCloudNetHelper.plugin;
    }

    public static void setPlugin(JavaPlugin plugin) {
        BukkitCloudNetHelper.plugin = plugin;
    }
}