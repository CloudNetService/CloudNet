/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A messenger to communicate between services in CloudNet.
 */
public interface CloudMessenger {

  /**
   * Sends a channel message to all services and nodes in the cluster. It can be received with the {@link
   * ChannelMessageReceiveEvent}.
   *
   * @param channel the channel to identify the message, this can be anything and doesn't have to be registered
   * @param message the message to identify the message, this can be anything and doesn't have to be registered
   * @param data    extra data for the message
   */
  default void sendChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
    this.sendChannelMessage(ChannelMessage.builder().channel(channel).message(message).json(data).targetAll().build());
  }

  /**
   * Sends a channel message to a specific service in the cluster. It can be received with the {@link
   * ChannelMessageReceiveEvent}.
   *
   * @param targetServiceInfoSnapshot the info of the service which will receive the message
   * @param channel                   the channel to identify the message, this can be anything and doesn't have to be
   *                                  registered
   * @param message                   the message to identify the message, this can be anything and doesn't have to be
   *                                  registered
   * @param data                      extra data for the message
   */
  default void sendChannelMessage(@NotNull ServiceInfoSnapshot targetServiceInfoSnapshot, @NotNull String channel,
    @NotNull String message, @NotNull JsonDocument data) {
    this.sendChannelMessage(ChannelMessage.builder().channel(channel).message(message).json(data)
      .targetService(targetServiceInfoSnapshot.getName()).build());
  }

  /**
   * Sends a channel message to all services of a specific task in the cluster. It can be received with the {@link
   * ChannelMessageReceiveEvent}.
   *
   * @param targetServiceTask the task which will receive the message
   * @param channel           the channel to identify the message, this can be anything and doesn't have to be
   *                          registered
   * @param message           the message to identify the message, this can be anything and doesn't have to be
   *                          registered
   * @param data              extra data for the message
   */
  default void sendChannelMessage(@NotNull ServiceTask targetServiceTask, @NotNull String channel,
    @NotNull String message, @NotNull JsonDocument data) {
    this.sendChannelMessage(
      ChannelMessage.builder().channel(channel).message(message).json(data).targetTask(targetServiceTask.getName())
        .build());
  }

  /**
   * Sends a channel message to all services of a specific environment in the cluster. It can be received with the
   * {@link ChannelMessageReceiveEvent}.
   *
   * @param targetEnvironment the environment which will receive the message
   * @param channel           the channel to identify the message, this can be anything and doesn't have to be
   *                          registered
   * @param message           the message to identify the message, this can be anything and doesn't have to be
   *                          registered
   * @param data              extra data for the message
   */
  default void sendChannelMessage(@NotNull ServiceEnvironmentType targetEnvironment, @NotNull String channel,
    @NotNull String message, @NotNull JsonDocument data) {
    this.sendChannelMessage(
      ChannelMessage.builder().channel(channel).message(message).json(data).targetEnvironment(targetEnvironment)
        .build());
  }

  /**
   * Sends a channel message into the cluster.
   *
   * @param channelMessage the channel message to be sent
   */
  void sendChannelMessage(@NotNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers.
   *
   * @param channelMessage the channel message to be sent
   * @return a collection containing the responses from all receivers
   */
  @NotNull
  ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers.
   *
   * @param channelMessage the channel message to be sent
   * @return a collection containing the responses from all receivers
   */
  @NotNull
  Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers. This method only returns the
   * first of all received messages.
   *
   * @param channelMessage the channel message to be sent
   * @return the response of the first receiver
   */
  @NotNull
  ITask<ChannelMessage> sendSingleChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers. This method only returns the
   * first of all received messages.
   *
   * @param channelMessage the channel message to be sent
   * @return the response of the first receiver
   */
  @Nullable
  ChannelMessage sendSingleChannelMessageQuery(@NotNull ChannelMessage channelMessage);

}
