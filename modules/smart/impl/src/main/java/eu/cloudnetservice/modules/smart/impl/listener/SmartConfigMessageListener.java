/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.smart.impl.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.modules.smart.impl.NodeSmartServiceManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class SmartConfigMessageListener {

  private final NodeSmartServiceManagement nodeSmartServiceManagement;

  @Inject
  public SmartConfigMessageListener(@NonNull NodeSmartServiceManagement nodeSmartServiceManagement) {
    this.nodeSmartServiceManagement = nodeSmartServiceManagement;
  }

  @EventListener
  private void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (NodeSmartServiceManagement.SMART_CHANNEL_NAME.equals(event.channel())) {
      switch (event.message()) {
        case NodeSmartServiceManagement.SMART_CONFIG_ADD -> {
          var config = event.content().readObject(SmartServiceTaskConfig.class);
          this.nodeSmartServiceManagement.addSmartServiceTaskConfigSilently(config);
        }
        case NodeSmartServiceManagement.SMART_CONFIG_REMOVE -> {
          var taskName = event.content().readString();
          this.nodeSmartServiceManagement.removeSmartServiceTaskConfigSilently(taskName);
        }
      }
    }
  }
}
