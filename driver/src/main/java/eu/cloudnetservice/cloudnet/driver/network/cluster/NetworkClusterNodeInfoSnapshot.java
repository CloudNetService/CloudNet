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

package eu.cloudnetservice.cloudnet.driver.network.cluster;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import eu.cloudnetservice.cloudnet.driver.CloudNetVersion;
import eu.cloudnetservice.cloudnet.driver.module.ModuleConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ProcessSnapshot;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeInfoSnapshot extends JsonDocPropertyHolder {

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

  public NetworkClusterNodeInfoSnapshot(
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
    @NonNull JsonDocument properties
  ) {
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
    this.properties = properties;
  }

  public long creationTime() {
    return this.creationTime;
  }

  public long startupMillis() {
    return this.startupMillis;
  }

  public int maxMemory() {
    return this.maxMemory;
  }

  public int usedMemory() {
    return this.usedMemory;
  }

  public int reservedMemory() {
    return this.reservedMemory;
  }

  public int currentServicesCount() {
    return this.currentServicesCount;
  }

  public boolean draining() {
    return this.drain;
  }

  public @NonNull NetworkClusterNode node() {
    return this.node;
  }

  public @NonNull CloudNetVersion version() {
    return this.version;
  }

  public @NonNull ProcessSnapshot processSnapshot() {
    return this.processSnapshot;
  }

  public double maxProcessorUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  public @NonNull Collection<ModuleConfiguration> modules() {
    return this.modules;
  }
}
