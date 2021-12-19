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

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A messenger to communicate between services in CloudNet.
 */
@RPCValidation
public interface CloudMessenger {

  /**
   * Sends a channel message into the cluster.
   *
   * @param channelMessage the channel message to be sent
   */
  void sendChannelMessage(@NonNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers.
   *
   * @param channelMessage the channel message to be sent
   * @return a collection containing the responses from all receivers
   */
  @NonNull
  default ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage) {
    return CompletableTask.supply(() -> this.sendChannelMessageQuery(channelMessage));
  }

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers.
   *
   * @param channelMessage the channel message to be sent
   * @return a collection containing the responses from all receivers
   */
  @NonNull
  Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage);

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers. This method only returns the
   * first of all received messages.
   *
   * @param channelMessage the channel message to be sent
   * @return the response of the first receiver
   */
  @NonNull
  default ITask<ChannelMessage> sendSingleChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage) {
    return CompletableTask.supply(() -> this.sendSingleChannelMessageQuery(channelMessage));
  }

  /**
   * Sends a channel message into the cluster and waits for the result from the receivers. This method only returns the
   * first of all received messages.
   *
   * @param channelMessage the channel message to be sent
   * @return the response of the first receiver
   */
  @Nullable
  ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage);
}
