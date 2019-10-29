package de.dytanic.cloudnet.ext.bridge.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.GameRules;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.CloudNetDriver;
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

import java.util.Map;

public final class NukkitCloudNetHelper {

    private static volatile String
            apiMotd = Server.getInstance().getMotd(),
            extra = "",
            state = "LOBBY";

    private static volatile int maxPlayers = Server.getInstance().getMaxPlayers();


    private NukkitCloudNetHelper() {
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
        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", Server.getInstance().getVersion())
                .append("Codename", Server.getInstance().getCodename())
                .append("Nukkit-Version", Server.getInstance().getApiVersion())
                .append("Online-Count", Server.getInstance().getOnlinePlayers().size())
                .append("Max-Players", maxPlayers)
                .append("Motd", apiMotd)
                .append("Extra", extra)
                .append("State", state)
                .append("Allow-Nether", Server.getInstance().isNetherAllowed())
                .append("Allow-Flight", Server.getInstance().getAllowFlight())
                .append("Players", Iterables.map(Server.getInstance().getOnlinePlayers().values(), player -> new NukkitCloudNetPlayerInfo(
                        player.getHealth(),
                        player.getMaxHealth(),
                        player.getFoodData().getLevel(),
                        player.getExperienceLevel(),
                        player.getPing(),
                        new WorldPosition(
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                player.getYaw(),
                                player.getPitch(),
                                player.getLevel().getName()
                        ),
                        new HostAndPort(player.getAddress(), player.getPort()),
                        player.getUniqueId(),
                        player.getName()
                )))
                .append("Plugins", Iterables.map(Server.getInstance().getPluginManager().getPlugins().values(), plugin -> {
                    PluginInfo pluginInfo = new PluginInfo(plugin.getName(), plugin.getDescription().getVersion());

                    pluginInfo.getProperties()
                            .append("authors", plugin.getDescription().getAuthors())
                            .append("dependencies", plugin.getDescription().getDepend())
                            .append("load-before", plugin.getDescription().getLoadBefore())
                            .append("description", plugin.getDescription().getDescription())
                            .append("commands", plugin.getDescription().getCommands())
                            .append("soft-dependencies", plugin.getDescription().getSoftDepend())
                            .append("website", plugin.getDescription().getWebsite())
                            .append("main-class", plugin.getClass().getName())
                            .append("prefix", plugin.getDescription().getPrefix())
                    ;

                    return pluginInfo;
                }))
                .append("Worlds", Iterables.map(Server.getInstance().getLevels().values(), level -> {
                    Map<String, String> gameRules = Maps.newHashMap();

                    for (GameRule gameRule : level.getGameRules().getRules()) {
                        GameRules.Value type = level.getGameRules().getGameRules().get(gameRule);

                        switch (type.getType()) {
                            case FLOAT:
                                gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getFloat(gameRule)));
                                break;
                            case BOOLEAN:
                                gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getBoolean(gameRule)));
                                break;
                            case INTEGER:
                                gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getInteger(gameRule)));
                                break;
                            default:
                                gameRules.put(gameRule.getName(), String.valueOf(level.getGameRules().getString(gameRule)));
                                break;
                        }
                    }

                    return new WorldInfo(null, level.getName(), getDifficultyToString(Server.getInstance().getDifficulty()), gameRules);
                }))
        ;
    }

    public static String getDifficultyToString(int value) {
        switch (value) {
            case 1:
                return "easy";
            case 2:
                return "normal";
            case 3:
                return "hard";
            default:
                return "peaceful";
        }
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
        return BridgeHelper.createNetworkConnectionInfo(
                player.getUniqueId(),
                player.getName(),
                -1,
                new HostAndPort(player.getAddress(), player.getPort()),
                new HostAndPort("0.0.0.0", Server.getInstance().getPort()),
                true,
                false,
                new NetworkServiceInfo(
                        ServiceEnvironmentType.NUKKIT,
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
            worldPosition = new WorldPosition(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch(),
                    player.getLevel().getName()
            );
        }

        return new NetworkPlayerServerInfo(
                player.getUniqueId(),
                player.getName(),
                null,
                player.getHealth(),
                player.getMaxHealth(),
                player.getFoodData().getLevel(),
                player.getExperienceLevel(),
                worldPosition,
                new HostAndPort(player.getAddress(), player.getPort()),
                new NetworkServiceInfo(
                        ServiceEnvironmentType.NUKKIT,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }

    public static String getApiMotd() {
        return NukkitCloudNetHelper.apiMotd;
    }

    public static void setApiMotd(String apiMotd) {
        NukkitCloudNetHelper.apiMotd = apiMotd;
    }

    public static String getExtra() {
        return NukkitCloudNetHelper.extra;
    }

    public static void setExtra(String extra) {
        NukkitCloudNetHelper.extra = extra;
    }

    public static String getState() {
        return NukkitCloudNetHelper.state;
    }

    public static void setState(String state) {
        NukkitCloudNetHelper.state = state;
    }

    public static int getMaxPlayers() {
        return NukkitCloudNetHelper.maxPlayers;
    }

    public static void setMaxPlayers(int maxPlayers) {
        NukkitCloudNetHelper.maxPlayers = maxPlayers;
    }
}