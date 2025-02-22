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

package eu.cloudnetservice.driver.cluster;

import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Holds information about a node which is connected to the cluster. Every node in the cluster should be connected to
 * each other node, so that every node knows everything about each other node.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public final class NodeInfoSnapshot implements DefaultedDocPropertyHolder {

  protected final long creationTime;
  protected final long startupMillis;

  protected final int maxMemory;
  protected final int usedMemory;
  protected final int reservedMemory;
  protected final int currentServicesCount;

  protected final boolean drain;

  protected final NetworkClusterNode node;
  protected final CloudNetVersion version;

  protected final ProcessSnapshot processSnapshot;
  protected final double maxCPUUsageToStartServices;

  protected final Collection<ModuleConfiguration> modules;

  protected final Document properties;

  /**
   * Constructs a new network cluster node info snapshot instance.
   *
   * @param creationTime               the epoch milli creation time of the snapshot, normally the current time.
   * @param startupMillis              the epoch milli when the node first started.
   * @param maxMemory                  the maximum amount of memory the node is allowed to use to start services.
   * @param usedMemory                 the currently used memory, including only services which are running.
   * @param reservedMemory             the currently reserved memory, including each service in any state.
   * @param currentServicesCount       the current amount of services, including each service in any state.
   * @param drain                      if the current node no longer starts services and will shut down when all
   *                                   services on it are stopped.
   * @param node                       the network node information of the node.
   * @param version                    the version this node is running on.
   * @param processSnapshot            the process snapshot of this.
   * @param maxCPUUsageToStartServices the maximum amount of cpu the node is allowed to use to start services.
   * @param modules                    the modules which are running on this node.
   * @param properties                 the properties of this node, mainly for developers.
   * @throws NullPointerException if either the given node, version, process snapshot, modules or properties are null.
   */
  public NodeInfoSnapshot(
    long creationTime,
    long startupMillis,
    int maxMemory,
    int usedMemory,
    int reservedMemory,
    int currentServicesCount,
    boolean drain,
    @NonNull NetworkClusterNode node,
    @NonNull CloudNetVersion version,
    @NonNull ProcessSnapshot processSnapshot,
    double maxCPUUsageToStartServices,
    @NonNull Collection<ModuleConfiguration> modules,
    @NonNull Document properties
  ) {
    this.properties = properties;
    this.creationTime = creationTime;
    this.startupMillis = startupMillis;
    this.maxMemory = maxMemory;
    this.usedMemory = usedMemory;
    this.reservedMemory = reservedMemory;
    this.currentServicesCount = currentServicesCount;
    this.drain = drain;
    this.node = node;
    this.version = version;
    this.processSnapshot = processSnapshot;
    this.maxCPUUsageToStartServices = maxCPUUsageToStartServices;
    this.modules = modules;
  }

  /**
   * Get the epoch milli creation time of this snapshot.
   *
   * @return the creation time of this snapshot.
   */
  public long creationTime() {
    return this.creationTime;
  }

  /**
   * Get the epoch milli startup time of the associated node.
   *
   * @return the startup time of the associated node.
   */
  public long startupMillis() {
    return this.startupMillis;
  }

  /**
   * Get the maximum amount of memory the associated node is allowed to use to start services.
   *
   * @return the maximum amount of memory to start services.
   */
  public int maxMemory() {
    return this.maxMemory;
  }

  /**
   * Get the amount of memory currently used by all services which are running on the associated node.
   *
   * @return the amount of memory currently used by all services which are running.
   */
  public int usedMemory() {
    return this.usedMemory;
  }

  /**
   * Get the amount of memory currently reserved by all services in any state on the associated node.
   *
   * @return the amount of memory currently reserved by all services in any state.
   */
  public int reservedMemory() {
    return this.reservedMemory;
  }

  /**
   * Get the amount of services in any state on the associated node.
   *
   * @return the amount of services in any state on the associated node.
   */
  public int currentServicesCount() {
    return this.currentServicesCount;
  }

  /**
   * Get the current draining state of associated node. If a node is marked as draining, the head node will no longer
   * request service starts and only wait for existing services to stop.
   *
   * @return the current draining state.
   */
  public boolean draining() {
    return this.drain;
  }

  /**
   * Get the offline node information associated with this node.
   *
   * @return the offline node information.
   */
  public @NonNull NetworkClusterNode node() {
    return this.node;
  }

  /**
   * Get the version the associated node is running on.
   *
   * @return the version.
   */
  public @NonNull CloudNetVersion version() {
    return this.version;
  }

  /**
   * Get the process snapshot made when this snapshot was created on the associated node.
   *
   * @return the process snapshot made when this snapshot was created.
   */
  public @NonNull ProcessSnapshot processSnapshot() {
    return this.processSnapshot;
  }

  /**
   * Get the maximum amount of cpu the associated node is allowed to consume before stopping and delaying service
   * starts.
   *
   * @return the maximum amount of the cpu (in percent).
   */
  public double maxProcessorUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  /**
   * Get all modules which are running on the associated node.
   *
   * @return all modules which are running on the associated node.
   */
  public @NonNull Collection<ModuleConfiguration> modules() {
    return this.modules;
  }

  /**
   * Get a rounded percentage of the memory the associated node is currently using.
   *
   * @return a rounded percentage of the memory the associated node is currently using.
   */
  public int memoryUsagePercentage() {
    return (this.reservedMemory() * 100) / this.maxMemory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }
}
