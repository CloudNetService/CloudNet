package de.dytanic.cloudnet.ext.bridge.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

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
        BridgeHelper.changeToIngame(s -> BukkitCloudNetHelper.state = s);
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        Collection<BukkitCloudNetPlayerInfo> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location location = player.getLocation();

            players.add(new BukkitCloudNetPlayerInfo(
                    player.getUniqueId(),
                    player.getName(),
                    player.getHealth(),
                    player.getMaxHealth(),
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
                .append("Online-Count", Bukkit.getOnlinePlayers().size())
                .append("Max-Players", maxPlayers)
                .append("Motd", apiMotd)
                .append("Extra", extra)
                .append("State", state)
                .append("Outgoing-Channels", Bukkit.getMessenger().getOutgoingChannels())
                .append("Incoming-Channels", Bukkit.getMessenger().getIncomingChannels())
                .append("Online-Mode", Bukkit.getOnlineMode())
                .append("Whitelist-Enabled", Bukkit.hasWhitelist())
                .append("Whitelist", Bukkit.getWhitelistedPlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList()))
                .append("Allow-Nether", Bukkit.getAllowNether())
                .append("Allow-End", Bukkit.getAllowEnd())
                .append("Players", players)
                .append("Plugins", Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(plugin -> {
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
                }).collect(Collectors.toList()))
                .append("Worlds", Bukkit.getWorlds().stream().map(world -> {
                    Map<String, String> gameRules = new HashMap<>();

                    for (String entry : world.getGameRules()) {
                        gameRules.put(entry, world.getGameRuleValue(entry));
                    }

                    return new WorldInfo(world.getUID(), world.getName(), world.getDifficulty().name(), gameRules);
                }).collect(Collectors.toList()))
        ;
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
        return BridgeHelper.createNetworkConnectionInfo(
                player.getUniqueId(),
                player.getName(),
                -1,
                new HostAndPort(player.getAddress()),
                new HostAndPort("0.0.0.0", Bukkit.getServer().getPort()),
                Bukkit.getServer().getOnlineMode(),
                false,
                new NetworkServiceInfo(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        Wrapper.getInstance().getServiceId(),
                        Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
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
                player.getHealth(),
                player.getMaxHealth(),
                player.getSaturation(),
                player.getLevel(),
                worldPosition,
                new HostAndPort(player.getAddress()),
                new NetworkServiceInfo(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        Wrapper.getInstance().getServiceId(),
                        Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
                )
        );
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