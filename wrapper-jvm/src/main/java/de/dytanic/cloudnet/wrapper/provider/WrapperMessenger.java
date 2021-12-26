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

package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.NetworkComponent;
import de.dytanic.cloudnet.driver.network.def.PacketServerChannelMessage;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.DefaultMessenger;
import de.dytanic.cloudnet.wrapper.Wrapper;
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
    this.component.sendPacket(new PacketServerChannelMessage(channelMessage));
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return this.component.firstChannel()
      .queryPacketManager()
      .sendQueryPacket(new PacketServerChannelMessage(channelMessage))
      .join()
      .content()
      .readObject(MESSAGES);
  }
}
