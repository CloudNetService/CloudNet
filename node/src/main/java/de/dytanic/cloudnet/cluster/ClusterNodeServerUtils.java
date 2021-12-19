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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget.Type;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import lombok.NonNull;

final class ClusterNodeServerUtils {

  private static final Logger LOGGER = LogManager.logger(ClusterNodeServerUtils.class);

  private ClusterNodeServerUtils() {
    throw new UnsupportedOperationException();
  }

  public static void handleNodeServerClose(@NonNull INetworkChannel channel, @NonNull IClusterNodeServer server) {
    for (var snapshot : CloudNet.instance().cloudServiceProvider().services()) {
      if (snapshot.serviceId().nodeUniqueId().equalsIgnoreCase(server.nodeInfo().uniqueId())) {
        // store the last lifecycle for the update event
        var lifeCycle = snapshot.lifeCycle();
        // mark the service as deleted
        snapshot.lifeCycle(ServiceLifeCycle.DELETED);
        // publish the update to the local service manager
        CloudNet.instance().cloudServiceProvider().handleServiceUpdate(snapshot, null);
        // call the local change event
        CloudNet.instance().eventManager().callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        // send the change to all service - all other nodes will handle the close as well (if there are any)
        if (!CloudNet.instance().cloudServiceProvider().localCloudServices().isEmpty()) {
          targetLocalServices()
            .message("update_service_lifecycle")
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .buffer(DataBuf.empty().writeObject(lifeCycle).writeObject(snapshot))
            .build()
            .send();
        }
      }
    }

    LOGGER.info(I18n.trans("cluster-server-networking-disconnected")
      .replace("%id%", server.nodeInfo().uniqueId())
      .replace("%serverAddress%", channel.serverAddress().toString())
      .replace("%clientAddress%", channel.clientAddress().toString()));
  }

  private static @NonNull ChannelMessage.Builder targetLocalServices() {
    var builder = ChannelMessage.builder();
    // iterate over all local services - if the service is connected append it as target
    for (var service : CloudNet.instance().cloudServiceProvider().localCloudServices()) {
      if (service.networkChannel() != null) {
        builder.target(Type.SERVICE, service.serviceId().name());
      }
    }
    // for chaining
    return builder;
  }
}
