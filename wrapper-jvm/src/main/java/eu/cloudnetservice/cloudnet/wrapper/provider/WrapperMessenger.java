/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.NetworkComponent;
import eu.cloudnetservice.cloudnet.driver.network.def.PacketServerChannelMessage;
import eu.cloudnetservice.cloudnet.driver.provider.CloudMessenger;
import eu.cloudnetservice.cloudnet.driver.provider.defaults.DefaultMessenger;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;

public class WrapperMessenger extends DefaultMessenger implements CloudMessenger {

  private static final Type MESSAGES = TypeToken.getParameterized(Collection.class, ChannelMessage.class).getType();

  private final NetworkComponent component;

  public WrapperMessenger(@NonNull Wrapper wrapper) {
    this.component = wrapper.networkClient();
  }

  @Override
  public void sendChannelMessage(@NonNull ChannelMessage channelMessage) {
    if (channelMessage.sendSync()) {
      this.component.sendPacketSync(new PacketServerChannelMessage(channelMessage, true));
    } else {
      this.component.sendPacket(new PacketServerChannelMessage(channelMessage, true));
    }
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return this.component.firstChannel()
      .queryPacketManager()
      .sendQueryPacket(new PacketServerChannelMessage(channelMessage, true))
      .join()
      .content()
      .readObject(MESSAGES);
  }
}
