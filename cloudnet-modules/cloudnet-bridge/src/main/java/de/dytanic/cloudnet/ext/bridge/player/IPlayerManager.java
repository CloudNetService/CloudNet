package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;

import java.util.List;
import java.util.UUID;

public interface IPlayerManager {

    ICloudPlayer getOnlinePlayer(UUID uniqueId);

    List<? extends ICloudPlayer> getOnlinePlayer(String name);

    List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment);

    List<? extends ICloudPlayer> getOnlinePlayers();

    ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId);

    List<? extends ICloudOfflinePlayer> getOfflinePlayer(String name);

    List<? extends ICloudOfflinePlayer> getRegisteredPlayers();


    ITask<? extends ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId);

    ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(String name);

    ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment);

    ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync();

    ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId);

    ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(String name);

    ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync();


    void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer);

    void updateOnlinePlayer(ICloudPlayer cloudPlayer);

    void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName);

    void proxyKickPlayer(ICloudPlayer cloudPlayer, String message);

    void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message);

    void broadcastMessage(String message);

    void broadcastMessage(String message, String permission);

}