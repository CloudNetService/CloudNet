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
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.provider.service.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalNodeServer extends DefaultNodeServer implements NodeServer {

  private final CloudNet cloudNet;
  private final long startupMillis;
  private final NodeServerProvider<? extends NodeServer> provider;

  protected LocalNodeServer(
    @NotNull CloudNet cloudNet,
    @NotNull NodeServerProvider<? extends NodeServer> provider
  ) {
    this.cloudNet = cloudNet;
    this.provider = provider;
    this.startupMillis = System.currentTimeMillis();

    this.setNodeInfo(cloudNet.getConfig().getIdentity());
  }

  @Override
  public @NotNull NodeServerProvider<? extends NodeServer> getProvider() {
    return this.provider;
  }

  @Override
  public boolean isAvailable() {
    return this.cloudNet.isRunning();
  }

  @Override
  public @NotNull String[] sendCommandLine(@NotNull String commandLine) {
    Collection<String> result = new ArrayList<>();
    // TODO this.cloudNet.getCommandMap().dispatchCommand(new DriverCommandSender(result), commandLine);
    return result.toArray(new String[0]);
  }

  @Override
  public @NotNull CloudServiceFactory getCloudServiceFactory() {
    return this.cloudNet.getCloudServiceFactory();
  }

  @Override
  public @Nullable SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot snapshot) {
    ICloudService service = this.cloudNet.getCloudServiceProvider().getLocalCloudService(snapshot);
    return service == null ? EmptySpecificCloudServiceProvider.INSTANCE : service;
  }

  @Override
  public void close() throws Exception {
    this.cloudNet.stop();
  }

  // TODO: modules are missing
  public void publishNodeInfoSnapshotUpdate() {
    // create a new node info snapshot
    this.setNodeInfoSnapshot(new NetworkClusterNodeInfoSnapshot(
      System.currentTimeMillis(),
      this.startupMillis,
      this.cloudNet.getConfig().getMaxMemory(),
      this.cloudNet.getCloudServiceProvider().getCurrentUsedHeapMemory(),
      this.cloudNet.getCloudServiceProvider().getCurrentReservedMemory(),
      this.cloudNet.getCloudServiceProvider().getLocalCloudServices().size(),
      this.nodeInfo,
      CloudNet.getInstance().getVersion(),
      ProcessSnapshot.self(),
      this.cloudNet.getConfig().getMaxCPUUsageToStartServices(),
      Collections.emptyList(),
      this.currentSnapshot == null ? JsonDocument.newDocument() : this.currentSnapshot.getProperties()));
    // send the new node info to all nodes
    ChannelMessage.builder()
      .targetNodes()
      .message("update_node_info_snapshot")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(this.currentSnapshot))
      .build()
      .send();
  }
}
