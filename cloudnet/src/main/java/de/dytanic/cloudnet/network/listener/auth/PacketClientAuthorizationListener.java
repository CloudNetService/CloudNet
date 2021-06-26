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

package de.dytanic.cloudnet.network.listener.auth;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelAuthCloudServiceSuccessEvent;
import de.dytanic.cloudnet.network.ClusterUtils;
import de.dytanic.cloudnet.network.listener.PacketClientServiceInfoUpdateListener;
import de.dytanic.cloudnet.network.listener.PacketServerChannelMessageListener;
import de.dytanic.cloudnet.network.listener.PacketServerSetGlobalLogLevelListener;
import de.dytanic.cloudnet.network.listener.cluster.PacketServerSyncTemplateStorageChunkListener;
import de.dytanic.cloudnet.network.listener.driver.PacketServerDriverAPIListener;
import de.dytanic.cloudnet.network.listener.driver.PacketServerRemoteDatabaseActionListener;
import de.dytanic.cloudnet.network.packet.PacketServerAuthorizationResponse;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.UUID;

public final class PacketClientAuthorizationListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("authorization") && packet.getHeader().contains("credentials")) {
      JsonDocument credentials = packet.getHeader().getDocument("credentials");

      switch (packet.getHeader().get("authorization", PacketClientAuthorization.PacketAuthorizationType.class)) {
        case NODE_TO_NODE:
          if (credentials.contains("clusterId") && credentials.contains("clusterNode") &&
            this.getCloudNet().getConfig().getClusterConfig().getClusterId()
              .equals(credentials.get("clusterId", UUID.class))) {
            NetworkClusterNode clusterNode = credentials.get("clusterNode", new TypeToken<NetworkClusterNode>() {
            }.getType());

            for (IClusterNodeServer clusterNodeServer : this.getCloudNet().getClusterNodeServerProvider()
              .getNodeServers()) {
              if (clusterNodeServer.isAcceptableConnection(channel, clusterNode.getUniqueId())) {
                this.getCloudNet().registerClusterPacketRegistryListeners(channel.getPacketRegistry(), false);

                channel.sendPacket(new PacketServerAuthorizationResponse(true, "successful"));
                channel.sendPacket(new PacketServerSetGlobalLogLevel(CloudNet.getInstance().getLogger().getLevel()));

                clusterNodeServer.setChannel(channel);
                CloudNetDriver.getInstance().getEventManager()
                  .callEvent(new NetworkChannelAuthClusterNodeSuccessEvent(clusterNodeServer, channel));

                this.getCloudNet().getLogger().info(
                  LanguageManager.getMessage("cluster-server-networking-connected")
                    .replace("%id%", clusterNode.getUniqueId())
                    .replace("%serverAddress%",
                      channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                    .replace("%clientAddress%",
                      channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
                );

                ClusterUtils.sendSetupInformationPackets(channel, credentials.getBoolean("secondNodeConnection"));
                return;
              }
            }
          }
          break;
        case WRAPPER_TO_NODE:
          if (credentials.contains("connectionKey") && credentials.contains("serviceId")) {
            String connectionKey = credentials.getString("connectionKey");
            ServiceId serviceId = credentials.get("serviceId", ServiceId.class);

            ICloudService cloudService = this.getCloudNet().getCloudServiceManager()
              .getCloudService(serviceId.getUniqueId());

            if (connectionKey != null && cloudService != null && cloudService.getConnectionKey().equals(connectionKey)
              &&
              cloudService.getServiceId().getTaskServiceId() == serviceId.getTaskServiceId() &&
              cloudService.getServiceId().getNodeUniqueId().equals(serviceId.getNodeUniqueId())) {
              //- packet channel registry
              channel.getPacketRegistry()
                .addListener(PacketConstants.CHANNEL_MESSAGING_CHANNEL, new PacketServerChannelMessageListener(true));
              channel.getPacketRegistry().addListener(PacketConstants.INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL,
                new PacketClientServiceInfoUpdateListener());

              channel.getPacketRegistry().addListener(PacketConstants.INTERNAL_DEBUGGING_CHANNEL,
                new PacketServerSetGlobalLogLevelListener(true));

              channel.getPacketRegistry().addListener(PacketConstants.INTERNAL_DATABASE_API_CHANNEL,
                new PacketServerRemoteDatabaseActionListener());

              channel.getPacketRegistry()
                .addListener(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, new PacketServerDriverAPIListener());

              channel.getPacketRegistry().addListener(PacketConstants.CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL,
                new PacketServerSyncTemplateStorageChunkListener(true));

              //-

              channel.sendPacket(new PacketServerAuthorizationResponse(true, "successful"));
              channel.sendPacket(new PacketServerSetGlobalLogLevel(CloudNet.getInstance().getLogger().getLevel()));

              cloudService.setNetworkChannel(channel);
              cloudService.getServiceInfoSnapshot().setConnectedTime(System.currentTimeMillis());

              CloudNetDriver.getInstance().getEventManager()
                .callEvent(new NetworkChannelAuthCloudServiceSuccessEvent(cloudService, channel));

              this.getCloudNet().getLogger().info(LanguageManager.getMessage("cloud-service-networking-connected")
                .replace("%id%", cloudService.getServiceId().getUniqueId().toString())
                .replace("%task%", cloudService.getServiceId().getTaskName())
                .replace("%serviceId%", String.valueOf(cloudService.getServiceId().getTaskServiceId()))
                .replace("%serverAddress%",
                  channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%",
                  channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
              );

              this.getCloudNet().sendAll(
                new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(),
                  PacketClientServerServiceInfoPublisher.PublisherType.CONNECTED));
              return;
            }
          }
          break;
        default:
          break;
      }

      channel.sendPacket(new PacketServerAuthorizationResponse(false, "access denied"));
      channel.close();
    }
  }

  private CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}
