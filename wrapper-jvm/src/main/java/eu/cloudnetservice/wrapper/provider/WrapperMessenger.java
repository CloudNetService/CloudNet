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

package eu.cloudnetservice.wrapper.provider;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.def.PacketServerChannelMessage;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import eu.cloudnetservice.driver.provider.defaults.DefaultMessenger;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;

@Singleton
@Provides(CloudMessenger.class)
public class WrapperMessenger extends DefaultMessenger implements CloudMessenger {

  private static final Type MESSAGES = TypeFactory.parameterizedClass(Collection.class, ChannelMessage.class);

  private final NetworkClient networkClient;

  @Inject
  public WrapperMessenger(@NonNull NetworkClient networkClient) {
    this.networkClient = networkClient;
  }

  @Override
  public void sendChannelMessage(@NonNull ChannelMessage channelMessage) {
    if (channelMessage.sendSync()) {
      this.networkClient.sendPacketSync(new PacketServerChannelMessage(channelMessage, true));
    } else {
      this.networkClient.sendPacket(new PacketServerChannelMessage(channelMessage, true));
    }
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    Collection<ChannelMessage> response = this.networkClient.firstChannel()
      .sendQueryAsync(new PacketServerChannelMessage(channelMessage, true))
      .join()
      .content()
      .readObject(MESSAGES);
    return Objects.requireNonNullElse(response, List.of());
  }
}
