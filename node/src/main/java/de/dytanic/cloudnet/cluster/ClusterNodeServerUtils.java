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
import org.jetbrains.annotations.NotNull;

final class ClusterNodeServerUtils {

  private static final Logger LOGGER = LogManager.logger(ClusterNodeServerUtils.class);

  private ClusterNodeServerUtils() {
    throw new UnsupportedOperationException();
  }

  public static void handleNodeServerClose(@NotNull INetworkChannel channel, @NotNull IClusterNodeServer server) {
    for (var snapshot : CloudNet.getInstance().cloudServiceProvider().services()) {
      if (snapshot.serviceId().nodeUniqueId().equalsIgnoreCase(server.getNodeInfo().uniqueId())) {
        // store the last lifecycle for the update event
        var lifeCycle = snapshot.lifeCycle();
        // mark the service as deleted
        snapshot.lifeCycle(ServiceLifeCycle.DELETED);
        // publish the update to the local service manager
        CloudNet.getInstance().cloudServiceProvider().handleServiceUpdate(snapshot, null);
        // call the local change event
        CloudNet.getInstance().eventManager().callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        // send the change to all service - all other nodes will handle the close as well (if there are any)
        if (!CloudNet.getInstance().cloudServiceProvider().getLocalCloudServices().isEmpty()) {
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
      .replace("%id%", server.getNodeInfo().uniqueId())
      .replace("%serverAddress%", channel.serverAddress().toString())
      .replace("%clientAddress%", channel.clientAddress().toString()));
  }

  private static @NotNull ChannelMessage.Builder targetLocalServices() {
    var builder = ChannelMessage.builder();
    // iterate over all local services - if the service is connected append it as target
    for (var service : CloudNet.getInstance().cloudServiceProvider().getLocalCloudServices()) {
      if (service.getNetworkChannel() != null) {
        builder.target(Type.SERVICE, service.getServiceId().name());
      }
    }
    // for chaining
    return builder;
  }
}
