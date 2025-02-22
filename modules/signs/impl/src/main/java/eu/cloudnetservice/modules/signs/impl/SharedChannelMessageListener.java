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

package eu.cloudnetservice.modules.signs.impl;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import java.util.Collection;
import lombok.NonNull;

public final class SharedChannelMessageListener {

  @EventListener
  public void handleChannelMessage(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull InternalSignManagement signManagement
  ) {
    if (event.channel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME)) {
      switch (event.message()) {
        // as sign was created
        case AbstractSignManagement.SIGN_CREATED -> signManagement.handleInternalSignCreate(
          event.content().readObject(Sign.class));

        // a sign was deleted
        case AbstractSignManagement.SIGN_DELETED -> signManagement.handleInternalSignRemove(
          event.content().readObject(WorldPosition.class));

        // a bulk of signs gets deleted
        case AbstractSignManagement.SIGN_BULK_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          for (var position : positions) {
            signManagement.handleInternalSignRemove(position);
          }
        }

        // the sign configuration was updated
        case AbstractSignManagement.SIGN_CONFIGURATION_UPDATE -> signManagement.handleInternalSignConfigUpdate(
          event.content().readObject(SignsConfiguration.class));

        // unknown message
        default -> {
        }
      }
    }
  }
}
