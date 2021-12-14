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

package de.dytanic.cloudnet.network.listener.message;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.group.GroupConfigurationAddEvent;
import de.dytanic.cloudnet.driver.event.events.group.GroupConfigurationRemoveEvent;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import java.lang.reflect.Type;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class GroupChannelMessageListener {

  private static final Type GROUPS = TypeToken.getParameterized(Collection.class, GroupConfiguration.class).getType();

  private final IEventManager eventManager;
  private final NodeGroupConfigurationProvider groupProvider;

  public GroupChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull NodeGroupConfigurationProvider groupProvider
  ) {
    this.eventManager = eventManager;
    this.groupProvider = groupProvider;
  }

  @EventListener
  public void handleChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL) && event.getMessage() != null) {
      switch (event.getMessage()) {
        // set groups
        case "set_group_configurations" -> {
          Collection<GroupConfiguration> groups = event.getContent().readObject(GROUPS);
          this.groupProvider.setGroupConfigurationsSilently(groups);
        }

        // add group
        case "add_group_configuration" -> {
          var configuration = event.getContent().readObject(GroupConfiguration.class);

          this.groupProvider.addGroupConfigurationSilently(configuration);
          this.eventManager.callEvent(new GroupConfigurationAddEvent(configuration));
        }

        // remove group
        case "remove_group_configuration" -> {
          var configuration = event.getContent().readObject(GroupConfiguration.class);

          this.groupProvider.removeGroupConfigurationSilently(configuration);
          this.eventManager.callEvent(new GroupConfigurationRemoveEvent(configuration));
        }

        // none of our business
        default -> {
        }
      }
    }
  }
}
