package de.dytanic.cloudnet.ext.bridge.player.executor;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PlayerExecutor {

    /**
     * Gets the uniqueId of the player this player executor handles.
     *
     * @return the UUID of the player
     */
    @NotNull
    UUID getPlayerUniqueId();

    /**
     * Connects an online player to a specific service.
     *
     * @param serviceName the name of the service the player should be sent to
     */
    void connect(@NotNull String serviceName);

    /**
     * Kicks an online player from the network with a specific reason.
     *
     * @param message the reason for the kick which will be displayed in the players client
     */
    void kick(@NotNull String message);

    /**
     * Sends a plugin message to a specific online player.
     *
     * @param message the message to be sent to the player
     */
    void sendChatMessage(@NotNull String message);

    /**
     * Sends a message to a specific online player. The tag has to be registered in the proxy.
     *
     * @param tag  the tag of the plugin message
     * @param data the data of the plugin message
     */
    void sendPluginMessage(@NotNull String tag, @NotNull byte[] data);

}
