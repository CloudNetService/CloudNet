package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface IPlayerManager {

    /**
     * Gets the amount of online players on the network
     *
     * @return the online count as an int
     */
    int getOnlineCount();

    /**
     * Gets the amount of registered players in the database.
     *
     * @return the registered player count as an int
     */
    int getRegisteredCount();

    /**
     * Gets an online player by its UUID.
     *
     * @param uniqueId the UUID of the player
     * @return the player if he is online or null if not
     */
    ICloudPlayer getOnlinePlayer(UUID uniqueId);

    /**
     * Gets the first online player found by its name.
     *
     * @param name the name of the player
     * @return the online player if there is at least one player with the given name online or null if there is no player with that name online
     */
    default ICloudPlayer getFirstOnlinePlayer(String name) {
        List<? extends ICloudPlayer> players = this.getOnlinePlayers(name);
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * Gets a list of all online players with the given name (case-insensitive).
     *
     * @param name the name of the player(s)
     * @return a list containing all online players in the cloud with the given name
     * @deprecated Moved to {@link #getOnlinePlayers(String)}
     */
    @Deprecated
    default List<? extends ICloudPlayer> getOnlinePlayer(String name) {
        return this.getOnlinePlayers(name);
    }

    /**
     * Gets a list of all online players with the given name. (case-insensitive).
     *
     * @param name the name of the player(s)
     * @return a list containing all online players in the cloud with the given name
     */
    List<? extends ICloudPlayer> getOnlinePlayers(String name);

    /**
     * Gets a list of all online players on a specific environment.
     *
     * @param environment the environment to get all players from
     * @return a list containing all players that are online on the given environment
     */
    List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment);

    /**
     * Gets a list of all online players on the whole network.
     *
     * @return a list containing all players that are online on the network
     */
    List<? extends ICloudPlayer> getOnlinePlayers();

    /**
     * Gets a registered player by its UUID out of the cloud
     *
     * @param uniqueId the UUID of the player
     * @return the player if he is registered in the cloud or null if not
     */
    ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId);

    /**
     * Gets the first registered player found by its name.
     *
     * @param name the name of the player
     * @return the registered player if there is at least one player with the given name registered or null if there is no player with that name registered
     */
    default ICloudOfflinePlayer getFirstOfflinePlayer(String name) {
        List<? extends ICloudOfflinePlayer> players = this.getOfflinePlayers(name);
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * Gets a list of all registered players with the given name. (case-insensitive)
     *
     * @param name the name of the player(s)
     * @return a list containing all players registered in the cloud with the given name
     * @deprecated Moved to {@link #getOfflinePlayers(String)}
     */
    @Deprecated
    default List<? extends ICloudOfflinePlayer> getOfflinePlayer(String name) {
        return this.getOfflinePlayers(name);
    }

    /**
     * Gets a list of all online players with the given name. (case-insensitive)
     *
     * @param name the name of the player(s)
     * @return a list containing all players registered in the cloud with the given name
     */
    List<? extends ICloudOfflinePlayer> getOfflinePlayers(String name);

    /**
     * Gets a list of all registered players in the network.
     * <p>
     * Depending on the amount of registered players, this method might take a bit longer or won't even work,
     * because it takes too much memory to keep the whole list loaded.
     *
     * @return the list with every registered player in the cloud
     * @deprecated This shouldn't be used when you have many players in your database, because it can cause major problems in the cloud
     */
    @Deprecated
    List<? extends ICloudOfflinePlayer> getRegisteredPlayers();


    /**
     * Gets the amount of online players on the network
     *
     * @return the online count as an int
     */
    ITask<Integer> getOnlineCountAsync();

    /**
     * Gets the amount of registered players in the database.
     *
     * @return the registered player count as an int
     */
    ITask<Integer> getRegisteredCountAsync();

    /**
     * Gets an online player by its UUID.
     *
     * @param uniqueId the UUID of the player
     * @return the player if he is online or null if not
     */
    ITask<? extends ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId);

    /**
     * Gets the first online player found by its name.
     *
     * @param name the name of the player
     * @return the online player if there is at least one player with the given name online or null if there is no player with that name online
     */
    default ITask<ICloudPlayer> getFirstOnlinePlayerAsync(String name) {
        Value<ICloudPlayer> result = new Value<>();
        ITask<ICloudPlayer> task = new ListenableTask<>(result::getValue);
        this.getOnlinePlayersAsync(name)
                .onComplete(players -> result.setValue(players.isEmpty() ? null : players.get(0)))
                .onCancelled(listITask -> task.cancel(true));
        return task;
    }

    /**
     * Gets a list of all online players with the given name (case-insensitive).
     *
     * @param name the name of the player(s)
     * @return a list containing all online players in the cloud with the given name
     * @deprecated Moved to {@link #getOnlinePlayersAsync(String)}
     */
    @Deprecated
    default ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(String name) {
        return this.getOnlinePlayersAsync(name);
    }

    /**
     * Gets a list of all online players with the given name. (case-insensitive).
     *
     * @param name the name of the player(s)
     * @return a list containing all online players in the cloud with the given name
     */
    ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(String name);

    /**
     * Gets a list of all online players on a specific environment.
     *
     * @param environment the environment to get all players from
     * @return a list containing all players that are online on the given environment
     */
    ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment);

    /**
     * Gets a list of all online players on the whole network.
     *
     * @return a list containing all players that are online on the network
     */
    ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync();

    /**
     * Gets a list of all online players on the whole network.
     *
     * @return a list containing all players that are online on the network
     */
    ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId);

    /**
     * Gets a list of all registered players with the given name. (case-insensitive)
     *
     * @param name the name of the player(s)
     * @return a list containing all players registered in the cloud with the given name
     * @deprecated Moved to {@link #getOfflinePlayersAsync(String)}
     */
    @Deprecated
    default ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(String name) {
        return this.getOfflinePlayersAsync(name);
    }

    /**
     * Gets the first registered player found by its name.
     *
     * @param name the name of the player
     * @return the registered player if there is at least one player with the given name registered or null if there is no player with that name registered
     */
    default ITask<ICloudOfflinePlayer> getFirstOfflinePlayerAsync(String name) {
        Value<ICloudOfflinePlayer> result = new Value<>();
        ITask<ICloudOfflinePlayer> task = new ListenableTask<>(result::getValue);
        this.getOfflinePlayersAsync(name)
                .onComplete(players -> result.setValue(players.isEmpty() ? null : players.get(0)))
                .onCancelled(listITask -> task.cancel(true));
        return task;
    }

    /**
     * Gets a list of all online players with the given name. (case-insensitive)
     *
     * @param name the name of the player(s)
     * @return a list containing all players registered in the cloud with the given name
     */
    ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(String name);

    /**
     * Gets a list of all registered players in the network.
     * <p>
     * Depending on the amount of registered players, this method might take a bit longer or won't even work,
     * because it takes too much memory to keep the whole list loaded.
     *
     * @return the list with every registered player in the cloud
     * @deprecated This shouldn't be used when you have many players in your database, because it can cause major problems in the cloud
     */
    @Deprecated
    ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync();

    /**
     * Updates the given player to the database of the cloud and calls the {@link de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent} on the whole network.
     *
     * @param cloudOfflinePlayer the player to be updated
     */
    void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer);

    /**
     * Updates the given player to the database of the cloud and calls the {@link de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent} on the whole network.
     *
     * @param cloudPlayer the player to be updated
     */
    void updateOnlinePlayer(ICloudPlayer cloudPlayer);

    /**
     * Connects an online player to a specific service.
     *
     * @param cloudPlayer the player to be connected
     * @param serviceName the name of the service the player should be sent to
     */
    void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName);

    /**
     * Kicks an online player from the network with a specific reason.
     *
     * @param cloudPlayer the player to be kicked
     * @param message     the reason for the kick which will be displayed in the players client
     */
    void proxyKickPlayer(ICloudPlayer cloudPlayer, String message);

    /**
     * Sends a message to a specific online player.
     *
     * @param cloudPlayer the player to send the message to
     * @param message     the message to be sent to the player
     */
    void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message);

    /**
     * Broadcasts a specific message over the whole network.
     *
     * @param message the message to be sent to all online players
     */
    void broadcastMessage(String message);

    /**
     * Broadcasts a specific message to all online players that have the given permission.
     * If the given permission is null, no permission is checked and the message is sent to all online players.
     *
     * @param message    the message to be sent to all online players with the given permission
     * @param permission the permission to check for
     */
    void broadcastMessage(String message, String permission);

}