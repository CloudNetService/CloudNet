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

package eu.cloudnetservice.cloudnet.node.cluster.defaults;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.module.ModuleWrapper;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ProcessSnapshot;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerProvider;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerState;
import eu.cloudnetservice.cloudnet.node.command.source.DriverCommandSource;
import eu.cloudnetservice.cloudnet.node.event.cluster.LocalNodeSnapshotConfigureEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class LocalNodeServer implements NodeServer {

  private final CloudNet node;
  private final NodeServerProvider provider;

  private final long creationMillis = System.currentTimeMillis();

  // current state
  private volatile boolean draining = false;
  private volatile Instant lastStateChange = Instant.now();
  private volatile NodeServerState state = NodeServerState.UNAVAILABLE;

  // node info
  private volatile NetworkClusterNodeInfoSnapshot currentSnapshot;
  private volatile NetworkClusterNodeInfoSnapshot lastSnapshot;

  public LocalNodeServer(@NonNull CloudNet node, @NonNull NodeServerProvider provider) {
    this.node = node;
    this.provider = provider;
  }

  @Override
  public @NonNull String name() {
    return this.info().uniqueId();
  }

  @Override
  public boolean head() {
    return this.provider().headNode() == this;
  }

  @Override
  public boolean available() {
    return this.state == NodeServerState.READY;
  }

  @Override
  public void shutdown() {
    this.node.stop();
  }

  @Override
  public boolean connect() {
    return true; // yes we are connected to us now :)
  }

  @Override
  public boolean draining() {
    return this.draining;
  }

  @Override
  public void drain(boolean doDrain) {
    this.draining = doDrain;
  }

  @Override
  public void syncClusterData(boolean force) {

  }

  @Override
  public @NonNull NetworkClusterNode info() {
    return this.node.config().identity();
  }

  @Override
  public @NonNull NodeServerProvider provider() {
    return this.provider;
  }

  @Override
  public @NonNull NodeServerState state() {
    return this.state;
  }

  @Override
  public void state(@NonNull NodeServerState state) {
    Verify.verify(state == NodeServerState.READY, "Local node only accepts state changes to READY");
    // set the state
    this.state = state;
    this.lastStateChange = Instant.now();
  }

  @Override
  public @NonNull Instant lastStateChangeStamp() {
    return this.lastStateChange;
  }

  @Override
  public @UnknownNullability NetworkChannel channel() {
    return null;
  }

  @Override
  public void channel(@Nullable NetworkChannel channel) {
    // no-op
  }

  @Override
  public @UnknownNullability NetworkClusterNodeInfoSnapshot nodeInfoSnapshot() {
    return this.currentSnapshot;
  }

  @Override
  public @UnknownNullability NetworkClusterNodeInfoSnapshot lastNodeInfoSnapshot() {
    return this.lastSnapshot;
  }

  @Override
  public void updateNodeInfoSnapshot(@Nullable NetworkClusterNodeInfoSnapshot snapshot) {
    Verify.verifyNotNull(snapshot, "Local node cannot accept null snapshots");
    // pre-move the current snapshot to the last snapshot
    this.lastSnapshot = this.currentSnapshot;
    this.currentSnapshot = snapshot;
  }

  @Override
  public @NonNull CloudServiceFactory serviceFactory() {
    return this.node.cloudServiceFactory();
  }

  @Override
  public @Nullable SpecificCloudServiceProvider serviceProvider(@NonNull UUID uniqueId) {
    return this.node.cloudServiceProvider().localCloudService(uniqueId);
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    var sender = new DriverCommandSource();
    this.node.commandProvider().execute(sender, commandLine).getOrNull();
    return sender.messages();
  }

  @Override
  public void close() {
    // if someone requests to close the local node just shutdown the node
    this.shutdown();
  }

  public void updateLocalSnapshot() {
    var snapshot = new NetworkClusterNodeInfoSnapshot(
      System.currentTimeMillis(),
      this.creationMillis,
      this.node.config().maxMemory(),
      this.node.cloudServiceProvider().currentUsedHeapMemory(),
      this.node.cloudServiceProvider().currentReservedMemory(),
      this.node.cloudServiceProvider().localCloudServices().size(),
      this.draining,
      this.info(),
      this.node.version(),
      ProcessSnapshot.self(),
      this.node.config().maxCPUUsageToStartServices(),
      this.node.moduleProvider().modules().stream()
        .map(ModuleWrapper::moduleConfiguration)
        .collect(Collectors.toSet()),
      this.currentSnapshot == null ? JsonDocument.newDocument() : this.currentSnapshot.properties());
    // configure the snapshot
    snapshot = this.node.eventManager().callEvent(new LocalNodeSnapshotConfigureEvent(snapshot)).snapshot();
    this.updateNodeInfoSnapshot(snapshot);
  }
}
