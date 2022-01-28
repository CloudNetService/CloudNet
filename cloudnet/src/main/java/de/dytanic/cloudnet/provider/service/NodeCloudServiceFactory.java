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

package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.LocalNodeServer;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientDriverAPI;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.DefaultCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.defaults.DefaultCloudServiceManager;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory {

  private final CloudNet cloudNet;
  private final DefaultCloudServiceManager cloudServiceManager;

  public NodeCloudServiceFactory(CloudNet cloudNet, DefaultCloudServiceManager cloudServiceManager) {
    this.cloudNet = cloudNet;
    this.cloudServiceManager = cloudServiceManager;
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
    Preconditions.checkNotNull(serviceConfiguration);

    if (this.cloudNet.getClusterNodeServerProvider().getSelfNode().isHeadNode()) {
      if (this.cloudNet.isMainThread()) {
        return this.createCloudServiceAsHeadNode(serviceConfiguration);
      } else {
        return this.cloudNet.runTask(() -> this.createCloudServiceAsHeadNode(serviceConfiguration))
          .get(5, TimeUnit.SECONDS, null);
      }
    } else {
      return this.cloudNet.getClusterNodeServerProvider().getHeadNode()
        .getCloudServiceFactory().createCloudService(serviceConfiguration);
    }
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
    return this.cloudNet.runTask(() -> this.createCloudService(serviceConfiguration));
  }

  private ServiceInfoSnapshot createCloudServiceAsHeadNode(ServiceConfiguration serviceConfiguration) {
    NodeServer nodeServer;
    if (serviceConfiguration.getServiceId().getNodeUniqueId() == null) {
      Collection<String> allowedNodes = serviceConfiguration.getServiceId().getAllowedNodes();
      if (allowedNodes == null) {
        allowedNodes = Collections.emptySet();
      }

      nodeServer = this.cloudNet
        .searchLogicNodeServer(allowedNodes, serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
    } else {
      String nodeUniqueId = serviceConfiguration.getServiceId().getNodeUniqueId();
      if (this.cloudNet.getClusterNodeServerProvider().getSelfNode().getNodeInfo().getUniqueId().equals(nodeUniqueId)) {
        nodeServer = this.cloudNet.getClusterNodeServerProvider().getSelfNode();
      } else {
        nodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(nodeUniqueId);
      }
    }

    if (nodeServer != null && nodeServer.isAvailable()) {
      this.cloudServiceManager.prepareServiceConfiguration(nodeServer, serviceConfiguration);
      nodeServer.getNodeInfoSnapshot()
        .addReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());

      ServiceInfoSnapshot snapshot;
      if (nodeServer instanceof LocalNodeServer) {
        ICloudService cloudService = this.cloudNet.getCloudServiceManager()
          .createCloudService(serviceConfiguration)
          .get(5, TimeUnit.SECONDS, null);
        snapshot = cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
      } else if (nodeServer instanceof IClusterNodeServer) {
        snapshot = ((IClusterNodeServer) nodeServer).getChannel().sendQueryAsync(new PacketClientDriverAPI(
            DriverAPIRequestType.FORCE_CREATE_CLOUD_SERVICE_BY_CONFIGURATION,
            buffer -> buffer.writeObject(serviceConfiguration)
          ))
          .map(packet -> packet.getBuffer().readOptionalObject(ServiceInfoSnapshot.class))
          .get(6, TimeUnit.SECONDS, null);
      } else {
        return null;
      }

      return snapshot;
    }

    return null;
  }
}
