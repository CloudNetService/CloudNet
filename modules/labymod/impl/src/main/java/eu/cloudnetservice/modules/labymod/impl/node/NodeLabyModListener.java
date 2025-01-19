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

package eu.cloudnetservice.modules.labymod.impl.node;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.modules.labymod.LabyModManagement;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
final class NodeLabyModListener {

  @EventListener
  public void handleConfigUpdate(@NonNull ChannelMessageReceiveEvent event, @NonNull NodeLabyModManagement management) {
    if (event.channel().equals(LabyModManagement.LABYMOD_MODULE_CHANNEL)
      && LabyModManagement.LABYMOD_UPDATE_CONFIG.equals(event.message())) {
      // read the configuration & write it
      var configuration = event.content().readObject(LabyModConfiguration.class);
      management.configurationSilently(configuration);
    }
  }
}
