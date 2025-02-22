/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.cluster.defaults;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.event.cluster.LocalNodeSnapshotConfigureEvent;
import eu.cloudnetservice.node.impl.command.source.DriverCommandSource;
import eu.cloudnetservice.node.impl.tick.DefaultShutdownHandler;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Singleton
@Provides(LocalNodeServer.class)
public class DefaultLocalNodeServer implements LocalNodeServer {

  private final CloudNetVersion version;
  private final EventManager eventManager;
  private final NodeServerProvider provider;
  private final Configuration configuration;
  private final ModuleProvider moduleProvider;
  private final CommandProvider commandProvider;
  private final CloudServiceFactory cloudServiceFactory;
  private final CloudServiceManager cloudServiceProvider;
  private final Provider<DefaultShutdownHandler> shutdownHandlerProvider;

  private final long creationMillis = System.currentTimeMillis();

  // current state
  private volatile boolean draining = false;
  private volatile Instant lastStateChange = Instant.now();
  private volatile NodeServerState state = NodeServerState.UNAVAILABLE;

  // node info
  private volatile NodeInfoSnapshot currentSnapshot;
  private volatile NodeInfoSnapshot lastSnapshot;
  private volatile Instant lastNodeInfoUpdate = Instant.now();

  @Inject
  public DefaultLocalNodeServer(
    @NonNull CloudNetVersion version,
    @NonNull EventManager eventManager,
    @NonNull NodeServerProvider provider,
    @NonNull Configuration configuration,
    @NonNull ModuleProvider moduleProvider,
    @NonNull CommandProvider commandProvider,
    @NonNull CloudServiceFactory cloudServiceFactory,
    @NonNull CloudServiceManager cloudServiceProvider,
    @NonNull Provider<DefaultShutdownHandler> shutdownHandlerProvider
  ) {
    this.version = version;
    this.eventManager = eventManager;
    this.provider = provider;
    this.configuration = configuration;
    this.moduleProvider = moduleProvider;
    this.commandProvider = commandProvider;
    this.cloudServiceFactory = cloudServiceFactory;
    this.cloudServiceProvider = cloudServiceProvider;
    this.shutdownHandlerProvider = shutdownHandlerProvider;
  }

  @Override
  public @NonNull String name() {
    return this.info().uniqueId();
  }

  @Override
  public boolean head() {
    return this.provider().headNode().equals(this);
  }

  @Override
  public boolean available() {
    return this.state == NodeServerState.READY;
  }

  @Override
  public void shutdown() {
    this.shutdownHandlerProvider.get().shutdown();
  }

  @Override
  public @NonNull CompletableFuture<Void> connect() {
    return CompletableFuture.completedFuture(null); // yes we are connected to us now :)
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
    return this.configuration.identity();
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
    Preconditions.checkState(state == NodeServerState.READY, "Local node only accepts state changes to READY");
    // set the state
    this.state = state;
    this.lastStateChange = Instant.now();
  }

  @Override
  public @NonNull Instant lastStateChange() {
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
  public @UnknownNullability NodeInfoSnapshot nodeInfoSnapshot() {
    return this.currentSnapshot;
  }

  @Override
  public @UnknownNullability NodeInfoSnapshot lastNodeInfoSnapshot() {
    return this.lastSnapshot;
  }

  @Override
  public void updateNodeInfoSnapshot(@Nullable NodeInfoSnapshot snapshot) {
    Preconditions.checkNotNull(snapshot, "Local node cannot accept null snapshots");
    // pre-move the current snapshot to the last snapshot
    this.lastSnapshot = this.currentSnapshot;
    this.currentSnapshot = snapshot;
    this.lastNodeInfoUpdate = Instant.now();
  }

  @Override
  public @NonNull Instant lastNodeInfoUpdate() {
    return this.lastNodeInfoUpdate;
  }

  @Override
  public @NonNull CloudServiceFactory serviceFactory() {
    return this.cloudServiceFactory;
  }

  @Override
  public @Nullable SpecificCloudServiceProvider serviceProvider(@NonNull UUID uniqueId) {
    return this.cloudServiceProvider.localCloudService(uniqueId);
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    var sender = new DriverCommandSource();
    TaskUtil.getOrDefault(this.commandProvider.execute(sender, commandLine), null);
    return sender.messages();
  }

  @Override
  public void close() {
    // if someone requests to close the local node just shutdown the node
    this.shutdown();
  }

  @Override
  public void updateLocalSnapshot() {
    var snapshot = new NodeInfoSnapshot(
      System.currentTimeMillis(),
      this.creationMillis,
      this.configuration.maxMemory(),
      this.cloudServiceProvider.currentUsedHeapMemory(),
      this.cloudServiceProvider.currentReservedMemory(),
      this.cloudServiceProvider.localCloudServices().size(),
      this.draining,
      this.info(),
      this.version,
      ProcessSnapshot.self(),
      this.configuration.maxCPUUsageToStartServices(),
      this.moduleProvider.modules().stream()
        .map(ModuleWrapper::moduleConfiguration)
        .collect(Collectors.toSet()),
      this.currentSnapshot == null ? Document.newJsonDocument() : this.currentSnapshot.propertyHolder());
    // configure the snapshot
    snapshot = this.eventManager.callEvent(new LocalNodeSnapshotConfigureEvent(snapshot)).snapshot();
    this.updateNodeInfoSnapshot(snapshot);
  }
}
