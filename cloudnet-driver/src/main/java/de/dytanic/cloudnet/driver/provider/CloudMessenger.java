package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.jetbrains.annotations.NotNull;

/**
 * A messenger to communicate between services in CloudNet.
 */
public interface CloudMessenger {

    /**
     * Sends a channel message to all services in the cluster.
     * It can be received with the {@link ChannelMessageReceiveEvent}.
     *
     * @param channel the channel to identify the message, this can be anything and doesn't have to be registered
     * @param message the message to identify the message, this can be anything and doesn't have to be registered
     * @param data    extra data for the message
     */
    void sendChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    /**
     * Sends a channel message to a specific service in the cluster.
     * It can be received with the {@link ChannelMessageReceiveEvent}.
     *
     * @param targetServiceInfoSnapshot the info of the service which will receive the message
     * @param channel                   the channel to identify the message, this can be anything and doesn't have to be registered
     * @param message                   the message to identify the message, this can be anything and doesn't have to be registered
     * @param data                      extra data for the message
     */
    void sendChannelMessage(@NotNull ServiceInfoSnapshot targetServiceInfoSnapshot, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    /**
     * Sends a channel message to all services of a specific task in the cluster.
     * It can be received with the {@link ChannelMessageReceiveEvent}.
     *
     * @param targetServiceTask the task which will receive the message
     * @param channel           the channel to identify the message, this can be anything and doesn't have to be registered
     * @param message           the message to identify the message, this can be anything and doesn't have to be registered
     * @param data              extra data for the message
     */
    void sendChannelMessage(@NotNull ServiceTask targetServiceTask, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    void sendChannelMessage(@NotNull ServiceEnvironmentType targetEnvironment, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

}
