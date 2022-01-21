/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.cluster;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageTarget.Type;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import java.util.Collection;
import lombok.NonNull;

final class ClusterNodeServerUtil {

  private static final Logger LOGGER = LogManager.logger(ClusterNodeServerUtil.class);

  private ClusterNodeServerUtil() {
    throw new UnsupportedOperationException();
  }

  public static void handleNodeServerClose(@NonNull NetworkChannel channel, @NonNull ClusterNodeServer server) {
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
        var localServices = CloudNet.instance().cloudServiceProvider().localCloudServices();
        if (!localServices.isEmpty()) {
          targetServices(localServices)
            .message("update_service_lifecycle")
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .buffer(DataBuf.empty().writeObject(lifeCycle).writeObject(snapshot))
            .build()
            .send();
        }
      }
    }

    LOGGER.info(I18n.trans("cluster-server-networking-disconnected", server.nodeInfo().uniqueId()));
  }

  private static @NonNull ChannelMessage.Builder targetServices(@NonNull Collection<CloudService> services) {
    var builder = ChannelMessage.builder();
    // iterate over all local services - if the service is connected append it as target
    for (var service : services) {
      builder.target(Type.SERVICE, service.serviceId().name());
    }
    // for chaining
    return builder;
  }
}
