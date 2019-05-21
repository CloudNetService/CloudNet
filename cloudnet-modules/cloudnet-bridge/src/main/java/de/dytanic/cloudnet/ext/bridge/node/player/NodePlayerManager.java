package de.dytanic.cloudnet.ext.bridge.node.player;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public final class NodePlayerManager implements IPlayerManager {

    @Getter
    private static NodePlayerManager instance;

    private final Map<UUID, CloudPlayer> onlineCloudPlayers = Maps.newConcurrentHashMap();

    private final String databaseName;

    public NodePlayerManager(String databaseName)
    {
        this.databaseName = databaseName;

        /*= --------------------------------- =*/

        instance = this;
    }

    /*= ---------------------------------------------------------------- =*/

    public IDatabase getDatabase()
    {
        return CloudNet.getInstance().getDatabaseProvider().getDatabase(databaseName);
    }

    /*= ---------------------------------------------------------------- =*/

    @Override
    public CloudPlayer getOnlinePlayer(UUID uniqueId)
    {
        return onlineCloudPlayers.get(uniqueId);
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayer(String name)
    {
        Validate.checkNotNull(name);

        return Iterables.filter(onlineCloudPlayers.values(), new Predicate<CloudPlayer>() {
            @Override
            public boolean test(CloudPlayer cloudPlayer)
            {
                return cloudPlayer.getName().equalsIgnoreCase(name);
            }
        });
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment)
    {
        Validate.checkNotNull(environment);

        return Iterables.filter(onlineCloudPlayers.values(), new Predicate<CloudPlayer>() {
            @Override
            public boolean test(CloudPlayer cloudPlayer)
            {
                return (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment) ||
                    (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment() == environment);
            }
        });
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers()
    {
        return Iterables.newArrayList(onlineCloudPlayers.values());
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId)
    {
        Validate.checkNotNull(uniqueId);

        JsonDocument jsonDocument = getDatabase().get(uniqueId.toString());

        return jsonDocument != null ? jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE) : null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayer(String name)
    {
        Validate.checkNotNull(name);

        return Iterables.map(getDatabase().get(new JsonDocument("name", name)), new Function<JsonDocument, ICloudOfflinePlayer>() {
            @Override
            public ICloudOfflinePlayer apply(JsonDocument jsonDocument)
            {
                return jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE);
            }
        });
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers()
    {
        List<? extends ICloudOfflinePlayer> cloudOfflinePlayers = Iterables.newArrayList();

        getDatabase().iterate(new BiConsumer<String, JsonDocument>() {
            @Override
            public void accept(String s, JsonDocument jsonDocument)
            {
                cloudOfflinePlayers.add(jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE));
            }
        });

        return cloudOfflinePlayers;
    }

    /*= ---------------------------------------------------------------------------------- =*/

    @Override
    public ITask<ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId)
    {
        return schedule(new Callable<ICloudPlayer>() {
            @Override
            public ICloudPlayer call() throws Exception
            {
                return getOnlinePlayer(uniqueId);
            }
        });
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(String name)
    {
        return schedule(new Callable<List<? extends ICloudPlayer>>() {
            @Override
            public List<? extends ICloudPlayer> call() throws Exception
            {
                return getOnlinePlayer(name);
            }
        });
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment)
    {
        return schedule(new Callable<List<? extends ICloudPlayer>>() {
            @Override
            public List<? extends ICloudPlayer> call() throws Exception
            {
                return getOnlinePlayers(environment);
            }
        });
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync()
    {
        return schedule(new Callable<List<? extends ICloudPlayer>>() {
            @Override
            public List<? extends ICloudPlayer> call() throws Exception
            {
                return getOnlinePlayers();
            }
        });
    }

    @Override
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId)
    {
        return schedule(new Callable<ICloudOfflinePlayer>() {
            @Override
            public ICloudOfflinePlayer call() throws Exception
            {
                return getOfflinePlayer(uniqueId);
            }
        });
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(String name)
    {
        return schedule(new Callable<List<? extends ICloudOfflinePlayer>>() {
            @Override
            public List<? extends ICloudOfflinePlayer> call() throws Exception
            {
                return getOfflinePlayer(name);
            }
        });
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync()
    {
        return schedule(new Callable<List<? extends ICloudOfflinePlayer>>() {
            @Override
            public List<? extends ICloudOfflinePlayer> call() throws Exception
            {
                return getRegisteredPlayers();
            }
        });
    }

    /*= -------------------------------------------------------------------------------------- =*/

    @Override
    public void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer)
    {
        Validate.checkNotNull(cloudOfflinePlayer);

        updateOfflinePlayer0(cloudOfflinePlayer);
        CloudNetDriver.getInstance().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
            "update_offline_cloud_player",
            new JsonDocument(
                "offlineCloudPlayer", cloudOfflinePlayer
            )
        );
    }

    public void updateOfflinePlayer0(ICloudOfflinePlayer cloudOfflinePlayer)
    {
        getDatabase().update(cloudOfflinePlayer.getUniqueId().toString(), JsonDocument.newDocument(cloudOfflinePlayer));
    }

    @Override
    public void updateOnlinePlayer(ICloudPlayer cloudPlayer)
    {
        Validate.checkNotNull(cloudPlayer);

        updateOnlinePlayer0(cloudPlayer);
        CloudNetDriver.getInstance().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
            "update_online_cloud_player",
            new JsonDocument(
                "cloudPlayer", cloudPlayer
            )
        );
    }

    public void updateOnlinePlayer0(ICloudPlayer cloudPlayer)
    {
        updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
    }

    @Override
    public void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName)
    {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(serviceName);

        CloudNetDriver.getInstance().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
            "send_on_proxy_player_to_server",
            new JsonDocument()
                .append("uniqueId", cloudPlayer.getUniqueId())
                .append("serviceName", serviceName)
        );
    }

    @Override
    public void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message)
    {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(message);

        CloudNetDriver.getInstance().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
            "send_message_to_proxy_player",
            new JsonDocument()
                .append("uniqueId", cloudPlayer.getUniqueId())
                .append("name", cloudPlayer.getName())
                .append("message", message)
        );
    }

    @Override
    public void proxyKickPlayer(ICloudPlayer cloudPlayer, String kickMessage)
    {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(kickMessage);

        CloudNetDriver.getInstance().sendChannelMessage(
            BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
            "kick_on_proxy_player_from_network",
            new JsonDocument()
                .append("uniqueId", cloudPlayer.getUniqueId())
                .append("name", cloudPlayer.getName())
                .append("kickMessage", kickMessage)
        );
    }

    /*= ------------------------------------------------------------------------------------ =*/

    private <T> ITask<T> schedule(Callable<T> callable)
    {
        return CloudNet.getInstance().getTaskScheduler().schedule(callable);
    }
}