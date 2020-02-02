package de.dytanic.cloudnet.ext.bridge.sponge;

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
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.ExperienceHolderData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.stream.Collectors;

public final class SpongeCloudNetHelper {

    private static volatile String
            apiMotd = Sponge.getServer().getMotd().toPlain(),
            extra = "",
            state = "LOBBY";
    private static volatile int maxPlayers = Sponge.getServer().getMaxPlayers();

    private SpongeCloudNetHelper() {
        throw new UnsupportedOperationException();
    }


    public static void changeToIngame() {
        BridgeHelper.changeToIngame(s -> SpongeCloudNetHelper.state = s);
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", Sponge.getPlatform().getMinecraftVersion())
                .append("Sponge-Version", Sponge.getPlatform().getContainer(Platform.Component.API).getVersion())
                .append("Online-Count", Sponge.getServer().getOnlinePlayers().size())
                .append("Max-Players", maxPlayers)
                .append("Motd", apiMotd)
                .append("Extra", extra)
                .append("State", state)
                .append("Outgoing-Channels", Sponge.getChannelRegistrar().getRegisteredChannels(Platform.Type.SERVER))
                .append("Incoming-Channels", Sponge.getChannelRegistrar().getRegisteredChannels(Platform.Type.CLIENT))
                .append("Online-Mode", Sponge.getServer().getOnlineMode())
                .append("Whitelist-Enabled", Sponge.getServer().hasWhitelist())
                .append("Players", Sponge.getServer().getOnlinePlayers().stream().map(player -> {
                    Location<World> location = player.getLocation();

                    Optional<ExperienceHolderData> holderData = player.get(ExperienceHolderData.class);

                    return new SpongeCloudNetPlayerInfo(
                            player.getUniqueId(),
                            player.getName(),
                            player.getConnection().getLatency(),
                            player.getHealthData().health().get(),
                            player.getHealthData().maxHealth().get(),
                            player.saturation().get(),
                            holderData.map(experienceHolderData -> experienceHolderData.level().get()).orElse(0),
                            new WorldPosition(
                                    location.getX(),
                                    location.getY(),
                                    location.getZ(),
                                    0F,
                                    0F,
                                    player.getWorld().getName()
                            ),
                            new HostAndPort(player.getConnection().getAddress())
                    );
                }).collect(Collectors.toList()))
                .append("Plugins", Sponge.getGame().getPluginManager().getPlugins().stream().map(pluginContainer -> {
                    PluginInfo pluginInfo = new PluginInfo(pluginContainer.getId(), pluginContainer.getVersion().isPresent() ? pluginContainer.getVersion().get() : null);

                    pluginInfo.getProperties()
                            .append("name", pluginContainer.getName())
                            .append("authors", pluginContainer.getAuthors())
                            .append("url", pluginContainer.getUrl().isPresent() ? pluginContainer.getUrl().get() : null)
                            .append("description", pluginContainer.getDescription().isPresent() ? pluginContainer.getDescription().get() : null);

                    return pluginInfo;
                }).collect(Collectors.toList()))
                .append("Worlds", Sponge.getServer().getWorlds().stream()
                        .map(world -> new WorldInfo(world.getUniqueId(), world.getName(), world.getDifficulty().getName(), world.getGameRules()))
                        .collect(Collectors.toList()));
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
        boolean onlineMode = Sponge.getServer().getOnlineMode();

        return BridgeHelper.createNetworkConnectionInfo(
                player.getUniqueId(),
                player.getName(),
                -1,
                new HostAndPort(player.getConnection().getAddress()),
                new HostAndPort(Sponge.getServer().getBoundAddress().orElse(null)),
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
            Location<?> location = player.getLocation();
            worldPosition = new WorldPosition(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    0F,
                    0F,
                    player.getWorld().getName()
            );
        }
        Optional<ExperienceHolderData> holderData = player.get(ExperienceHolderData.class);

        return new NetworkPlayerServerInfo(
                player.getUniqueId(),
                player.getName(),
                null,
                player.getHealthData().health().get(),
                player.getHealthData().maxHealth().get(),
                player.saturation().get(),
                holderData.map(experienceHolderData -> experienceHolderData.level().get()).orElse(0),
                worldPosition,
                new HostAndPort(player.getConnection().getAddress()),
                new NetworkServiceInfo(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }

    public static String getApiMotd() {
        return SpongeCloudNetHelper.apiMotd;
    }

    public static void setApiMotd(String apiMotd) {
        SpongeCloudNetHelper.apiMotd = apiMotd;
    }

    public static String getExtra() {
        return SpongeCloudNetHelper.extra;
    }

    public static void setExtra(String extra) {
        SpongeCloudNetHelper.extra = extra;
    }

    public static String getState() {
        return SpongeCloudNetHelper.state;
    }

    public static void setState(String state) {
        SpongeCloudNetHelper.state = state;
    }

    public static int getMaxPlayers() {
        return SpongeCloudNetHelper.maxPlayers;
    }

    public static void setMaxPlayers(int maxPlayers) {
        SpongeCloudNetHelper.maxPlayers = maxPlayers;
    }
}