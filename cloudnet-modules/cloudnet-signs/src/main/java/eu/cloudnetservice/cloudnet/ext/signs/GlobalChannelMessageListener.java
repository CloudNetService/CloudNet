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

public class GlobalChannelMessageListener {

  protected final SignManagement signManagement;

  public GlobalChannelMessageListener(SignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME) && event.getMessage() != null) {
      switch (event.getMessage()) {
        case AbstractSignManagement.SIGN_CREATED:
          this.signManagement.handleInternalSignCreate(event.getBuffer().readObject(Sign.class));
          break;
        case AbstractSignManagement.SIGN_DELETED:
          this.signManagement.handleInternalSignRemove(event.getBuffer().readObject(WorldPosition.class));
          break;
        case AbstractSignManagement.SIGN_BULK_DELETE:
          for (WorldPosition position : event.getBuffer().readObjectCollection(WorldPosition.class)) {
            this.signManagement.handleInternalSignRemove(position);
          }
          break;
        case AbstractSignManagement.SIGN_CONFIGURATION_UPDATE:
          this.signManagement.handleInternalSignConfigUpdate(event.getBuffer().readObject(SignsConfiguration.class));
          break;
        default:
          break;
      }
    }
  }
}
