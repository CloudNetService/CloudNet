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

package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveGroupConfigurationsUpdateEvent;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import java.util.ArrayList;
import java.util.List;

public final class PacketServerSetGroupConfigurationListListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    List<GroupConfiguration> groupConfigurations = new ArrayList<>(
      packet.getBuffer().readObjectCollection(GroupConfiguration.class));
    NetworkUpdateType updateType = packet.getBuffer().readEnumConstant(NetworkUpdateType.class);

    if (updateType == null) {
      return;
    }

    NodeGroupConfigurationProvider provider = (NodeGroupConfigurationProvider) CloudNet.getInstance()
      .getGroupConfigurationProvider();

    NetworkChannelReceiveGroupConfigurationsUpdateEvent event = new NetworkChannelReceiveGroupConfigurationsUpdateEvent(
      channel, groupConfigurations, updateType);
    CloudNetDriver.getInstance().getEventManager().callEvent(event);

    if (!event.isCancelled()) {
      groupConfigurations =
        event.getGroupConfigurations() != null ? event.getGroupConfigurations() : groupConfigurations;
      switch (updateType) {
        case SET:
          provider.setGroupConfigurationsWithoutClusterSync(groupConfigurations);
          break;
        case ADD:
          for (GroupConfiguration groupConfiguration : groupConfigurations) {
            provider.addGroupConfigurationWithoutClusterSync(groupConfiguration);
          }
          break;
        case REMOVE:
          for (GroupConfiguration groupConfiguration : groupConfigurations) {
            provider.removeGroupConfigurationWithoutClusterSync(groupConfiguration.getName());
          }
          break;
        default:
          break;
      }
    }
  }
}
