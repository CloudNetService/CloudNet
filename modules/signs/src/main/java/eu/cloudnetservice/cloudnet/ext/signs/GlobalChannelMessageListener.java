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

package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import java.util.Collection;
import lombok.NonNull;

public class GlobalChannelMessageListener {

  protected final SignManagement signManagement;

  public GlobalChannelMessageListener(SignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME)) {
      switch (event.message()) {
        case AbstractSignManagement.SIGN_CREATED -> this.signManagement.handleInternalSignCreate(
            event.content().readObject(Sign.class));
        case AbstractSignManagement.SIGN_DELETED -> this.signManagement.handleInternalSignRemove(
            event.content().readObject(WorldPosition.class));
        case AbstractSignManagement.SIGN_BULK_DELETE -> {
          Collection<WorldPosition> positions = event.content().readObject(WorldPosition.COL_TYPE);
          for (var position : positions) {
            this.signManagement.handleInternalSignRemove(position);
          }
        }
        case AbstractSignManagement.SIGN_CONFIGURATION_UPDATE -> this.signManagement.handleInternalSignConfigUpdate(
            event.content().readObject(SignsConfiguration.class));
        default -> {
        }
      }
    }
  }
}
