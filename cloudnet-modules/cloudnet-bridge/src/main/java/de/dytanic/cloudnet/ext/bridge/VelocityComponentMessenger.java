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
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import net.kyori.text.Component;
import org.jetbrains.annotations.ApiStatus;

/**
 * @deprecated Use {@link AdventureComponentMessenger} instead
 */
@Deprecated
@ApiStatus.ScheduledForRemoval
public class VelocityComponentMessenger {

  private VelocityComponentMessenger() {
    throw new UnsupportedOperationException();
  }

  public static void sendMessage(ICloudPlayer cloudPlayer, Component message) {
    Preconditions.checkNotNull(cloudPlayer);

    sendMessage(cloudPlayer.getUniqueId(), message);
  }

  public static void sendMessage(UUID uniqueId, Component message) {
    Preconditions.checkNotNull(uniqueId);
    Preconditions.checkNotNull(message);

    AdventureComponentMessenger.sendMessage(uniqueId, LegacyText3ComponentSerializer.get().deserialize(message));
  }

  public static void broadcastMessage(Component message) {
    Preconditions.checkNotNull(message);

    broadcastMessage(message, null);
  }

  public static void broadcastMessage(Component message, String permission) {
    Preconditions.checkNotNull(message);

    AdventureComponentMessenger.broadcastMessage(LegacyText3ComponentSerializer.get().deserialize(message), permission);
  }
}
