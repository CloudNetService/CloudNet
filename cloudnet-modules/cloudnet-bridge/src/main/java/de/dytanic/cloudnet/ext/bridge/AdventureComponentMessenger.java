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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class AdventureComponentMessenger {

  private AdventureComponentMessenger() {
    throw new UnsupportedOperationException();
  }

  public static void sendMessage(ICloudPlayer cloudPlayer, Component message) {
    Preconditions.checkNotNull(cloudPlayer);

    sendMessage(cloudPlayer.getUniqueId(), message);
  }

  public static void sendMessage(UUID uniqueId, Component message) {
    Preconditions.checkNotNull(uniqueId);
    Preconditions.checkNotNull(message);

    DefaultPlayerExecutor.builder()
      .message("send_message_component")
      .buffer(ProtocolBuffer.create()
        .writeUUID(uniqueId)
        .writeString(GsonComponentSerializer.gson().serialize(message))
      )
      .build().send();
  }

  public static void broadcastMessage(Component message) {
    Preconditions.checkNotNull(message);

    broadcastMessage(message, null);
  }

  public static void broadcastMessage(Component message, String permission) {
    Preconditions.checkNotNull(message);

    DefaultPlayerExecutor.builder()
      .message("broadcast_message_component")
      .buffer(ProtocolBuffer.create()
        .writeString(GsonComponentSerializer.gson().serialize(message))
        .writeOptionalString(permission)
      )
      .build().send();
  }
}
