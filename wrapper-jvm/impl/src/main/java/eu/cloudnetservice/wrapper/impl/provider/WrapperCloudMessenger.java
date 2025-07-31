/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.provider;

import dev.derklaro.aerogel.auto.annotation.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.channel.BaseCloudMessenger;
import eu.cloudnetservice.driver.impl.network.standard.ChannelMessagePacket;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

@Singleton
@Provides(CloudMessenger.class)
public class WrapperCloudMessenger extends BaseCloudMessenger {

  private static final Type CHANNEL_MESSAGE_LIST_TYPE =
    TypeFactory.parameterizedClass(List.class, ChannelMessage.class);

  private final NetworkClient networkClient;

  @Inject
  public WrapperCloudMessenger(@NonNull NetworkClient networkClient) {
    this.networkClient = networkClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendChannelMessage(@NonNull ChannelMessage channelMessage) {
    if (channelMessage.sendSync()) {
      this.networkClient.sendPacketSync(new ChannelMessagePacket(channelMessage, true));
    } else {
      this.networkClient.sendPacket(new ChannelMessagePacket(channelMessage, true));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message
  ) {
    return this.networkClient.firstChannel()
      .sendQueryAsync(new ChannelMessagePacket(message, true))
      .thenApply(response -> {
        var packetContent = response.content();
        try {
          Collection<ChannelMessage> responses = packetContent.readObject(CHANNEL_MESSAGE_LIST_TYPE);
          return Objects.requireNonNullElse(responses, List.of());
        } finally {
          packetContent.forceRelease();
        }
      });
  }
}
