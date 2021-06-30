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

package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class BaseComponentMessenger {

  private BaseComponentMessenger() {
    throw new UnsupportedOperationException();
  }

  public static void sendMessage(ICloudPlayer cloudPlayer, BaseComponent[] messages) {
    Preconditions.checkNotNull(cloudPlayer);

    sendMessage(cloudPlayer.getUniqueId(), messages);
  }

  public static void sendMessage(UUID uniqueId, BaseComponent[] messages) {
    Preconditions.checkNotNull(uniqueId);
    Preconditions.checkNotNull(messages);

    DefaultPlayerExecutor.builder()
      .message("send_message_component")
      .buffer(ProtocolBuffer.create()
        .writeUUID(uniqueId)
        .writeString(ComponentSerializer.toString(messages))
      )
      .build().send();
  }

  public static void broadcastMessage(BaseComponent[] messages) {
    Preconditions.checkNotNull(messages);

    broadcastMessage(messages, null);
  }

  public static void broadcastMessage(BaseComponent[] messages, String permission) {
    Preconditions.checkNotNull(messages);

    DefaultPlayerExecutor.builder()
      .message("broadcast_message_component")
      .buffer(ProtocolBuffer.create()
        .writeString(ComponentSerializer.toString(messages))
        .writeOptionalString(permission)
      )
      .build().send();
  }
}
