package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.WorldInfo;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.math.Location;
import io.gomint.server.GoMintServer;
import io.gomint.server.network.Protocol;
import io.gomint.world.Gamerule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GoMintCloudNetHelper extends BridgeServerHelper {

    private GoMintCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void init() {
        BridgeServerHelper.setMotd(GoMint.instance().getMotd());
        BridgeServerHelper.setState("LOBBY");
        BridgeServerHelper.setMaxPlayers(GoMint.instance().getMaxPlayers());
    }

    public static GoMintServer getGoMintServer() {
        return (GoMintServer) GoMint.instance();
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", Protocol.MINECRAFT_PE_NETWORK_VERSION)
                .append("GoMint-Version", GoMint.instance().getVersion())
                .append("Max-Players", BridgeServerHelper.getMaxPlayers())
                .append("Motd", BridgeServerHelper.getMotd())
                .append("Extra", BridgeServerHelper.getExtra())
                .append("State", BridgeServerHelper.getState())
                .append("TPS", GoMint.instance().getTPS())
                .append("Online-Count", GoMint.instance().getPlayers().size())
                .append("Players", GoMint.instance().getPlayers().stream().map(entityPlayer -> {
                    io.gomint.server.entity.EntityPlayer player = (io.gomint.server.entity.EntityPlayer) entityPlayer;

                    Location location = entityPlayer.getLocation();

                    return new GoMintCloudNetPlayerInfo(
                            entityPlayer.getHealth(),
                            entityPlayer.getMaxHealth(),
                            entityPlayer.getSaturation(),
                            entityPlayer.getLevel(),
                            entityPlayer.getPing(),
                            entityPlayer.getLocale(),
                            new WorldPosition(
                                    location.getX(),
                                    location.getY(),
                                    location.getZ(),
                                    location.getYaw(),
                                    location.getPitch(),
                                    location.getWorld().getWorldName()
                            ),
                            new HostAndPort(player.getConnection().getConnection().getAddress()),
                            entityPlayer.getUUID(),
                            entityPlayer.isOnline(),
                            entityPlayer.getName(),
                            entityPlayer.getDeviceInfo().getDeviceName(),
                            entityPlayer.getXboxID(),
                            entityPlayer.getGamemode().name()
                    );
                }).collect(Collectors.toList()))
                .append("Worlds", GoMint.instance().getWorlds().stream().map(world -> {
                    Map<String, String> gameRules = new HashMap<>();

                    for (Field field : Gamerule.class.getFields()) {
                        if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) &&
                                Modifier.isPublic(field.getModifiers()) && Gamerule.class.isAssignableFrom(field.getType())) {
                            try {
                                field.setAccessible(true);
                                Gamerule<?> gameRule = (Gamerule<?>) field.get(null);
                                gameRules.put(gameRule.getNbtName(), String.valueOf(world.getGamerule(gameRule)));

                            } catch (IllegalAccessException exception) {
                                exception.printStackTrace();
                            }
                        }
                    }

                    return new WorldInfo(
                            new UUID(0, 0),
                            world.getLevelName(),
                            world.getDifficulty().name(),
                            gameRules
                    );
                }).collect(Collectors.toList()));

    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(EntityPlayer entityPlayer) {
        io.gomint.server.entity.EntityPlayer player = (io.gomint.server.entity.EntityPlayer) entityPlayer;

        return BridgeHelper.createNetworkConnectionInfo(
                player.getUUID(),
                player.getName(),
                Protocol.MINECRAFT_PE_PROTOCOL_VERSION,
                new HostAndPort(player.getConnection().getConnection().getAddress()),
                new HostAndPort(getGoMintServer().getServerConfig().getListener().getIp(), GoMint.instance().getPort()),
                true,
                false,
                BridgeHelper.createOwnNetworkServiceInfo()
        );
    }

    public static NetworkPlayerServerInfo createNetworkPlayerServerInfo(EntityPlayer entityPlayer, boolean login) {
        WorldPosition worldPosition;

        if (login) {
            worldPosition = new WorldPosition(-1, -1, -1, -1, -1, "world");
        } else {
            Location location = entityPlayer.getLocation();

            worldPosition = new WorldPosition(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    location.getWorld().getWorldName()
            );
        }

        io.gomint.server.entity.EntityPlayer player = (io.gomint.server.entity.EntityPlayer) entityPlayer;

        return new NetworkPlayerServerInfo(
                entityPlayer.getUUID(),
                entityPlayer.getName(),
                entityPlayer.getXboxID(),
                entityPlayer.getHealth(),
                entityPlayer.getMaxHealth(),
                entityPlayer.getSaturation(),
                entityPlayer.getLevel(),
                worldPosition,
                new HostAndPort(player.getConnection().getConnection().getAddress()),
                BridgeHelper.createOwnNetworkServiceInfo()
        );
    }
}