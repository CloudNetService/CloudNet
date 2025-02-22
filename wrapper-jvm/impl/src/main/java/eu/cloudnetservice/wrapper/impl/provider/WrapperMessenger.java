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

import com.google.common.collect.Iterables;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.network.standard.ChannelMessagePacket;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(CloudMessenger.class)
public class WrapperMessenger implements CloudMessenger {

  private static final Type MESSAGES = TypeFactory.parameterizedClass(Collection.class, ChannelMessage.class);

  private final NetworkClient networkClient;

  @Inject
  public WrapperMessenger(@NonNull NetworkClient networkClient) {
    this.networkClient = networkClient;
  }

  @Override
  public void sendChannelMessage(@NonNull ChannelMessage channelMessage) {
    if (channelMessage.sendSync()) {
      this.networkClient.sendPacketSync(new ChannelMessagePacket(channelMessage, true));
    } else {
      this.networkClient.sendPacket(new ChannelMessagePacket(channelMessage, true));
    }
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage).join();
  }

  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return Iterables.getFirst(this.sendChannelMessageQuery(channelMessage), null);
  }

  @Override
  public @NonNull CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.runAsync(() -> this.sendChannelMessage(channelMessage));
  }

  @Override
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message
  ) {
    return this.networkClient.firstChannel().sendQueryAsync(new ChannelMessagePacket(message, true))
      .thenApply(Packet::content)
      .thenApply(data -> Objects.requireNonNullElse(data.readObject(MESSAGES), List.of()));
  }

  @Override
  public @NonNull CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(
    @NonNull ChannelMessage message
  ) {
    return this.sendChannelMessageQueryAsync(message).thenApply(resp -> Iterables.getFirst(resp, null));
  }
}
