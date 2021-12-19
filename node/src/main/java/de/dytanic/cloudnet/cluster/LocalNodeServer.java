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
import de.dytanic.cloudnet.command.source.DriverCommandSource;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.event.cluster.LocalNodeSnapshotConfigureEvent;
import de.dytanic.cloudnet.service.defaults.provider.EmptySpecificCloudServiceProvider;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class LocalNodeServer extends DefaultNodeServer implements NodeServer {

  private final CloudNet cloudNet;
  private final long startupMillis;
  private final NodeServerProvider<? extends NodeServer> provider;

  private boolean drain;

  protected LocalNodeServer(
    @NonNull CloudNet cloudNet,
    @NonNull NodeServerProvider<? extends NodeServer> provider
  ) {
    this.cloudNet = cloudNet;
    this.provider = provider;
    this.startupMillis = System.currentTimeMillis();
    this.cloudNet.rpcProviderFactory().newHandler(NodeServer.class, this).registerToDefaultRegistry();
    this.drain = false;
  }

  @Override
  public @NonNull NodeServerProvider<? extends NodeServer> provider() {
    return this.provider;
  }

  @Override
  public boolean available() {
    return this.cloudNet.running() && this.nodeInfo != null;
  }

  @Override
  public boolean drain() {
    return this.drain;
  }

  @Override
  public void drain(boolean drain) {
    this.drain = drain;
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    var commandSource = new DriverCommandSource();
    this.cloudNet.commandProvider().execute(new DriverCommandSource(), commandLine);

    return commandSource.messages();
  }

  @Override
  public @NonNull CloudServiceFactory cloudServiceFactory() {
    return this.cloudNet.cloudServiceFactory();
  }

  @Override
  public @Nullable SpecificCloudServiceProvider cloudServiceProvider(@NonNull ServiceInfoSnapshot snapshot) {
    var service = this.cloudNet.cloudServiceProvider().localCloudService(snapshot);
    return service == null ? EmptySpecificCloudServiceProvider.INSTANCE : service;
  }

  @Override
  public void close() {
    this.cloudNet.stop();
  }

  public void publishNodeInfoSnapshotUpdate() {
    // create a new node info snapshot
    var snapshot = new NetworkClusterNodeInfoSnapshot(
      System.currentTimeMillis(),
      this.startupMillis,
      this.cloudNet.config().maxMemory(),
      this.cloudNet.cloudServiceProvider().currentUsedHeapMemory(),
      this.cloudNet.cloudServiceProvider().currentReservedMemory(),
      this.cloudNet.cloudServiceProvider().localCloudServices().size(),
      this.drain,
      this.nodeInfo,
      this.cloudNet.version(),
      ProcessSnapshot.self(),
      this.cloudNet.config().maxCPUUsageToStartServices(),
      this.cloudNet.moduleProvider().modules().stream()
        .map(IModuleWrapper::moduleConfiguration)
        .collect(Collectors.toSet()),
      this.currentSnapshot == null ? JsonDocument.newDocument() : this.currentSnapshot.properties());
    // configure the snapshot
    snapshot = this.cloudNet.eventManager().callEvent(new LocalNodeSnapshotConfigureEvent(snapshot)).snapshot();
    // set the snapshot
    this.nodeInfoSnapshot(snapshot);
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
