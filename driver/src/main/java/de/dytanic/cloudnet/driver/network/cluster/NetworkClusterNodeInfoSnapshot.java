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

package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.driver.CloudNetVersion;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
    @NotNull NetworkClusterNode node,
    @NotNull CloudNetVersion version,
    @NotNull ProcessSnapshot processSnapshot,
    double maxCPUUsageToStartServices,
    @NotNull Collection<ModuleConfiguration> modules,
    @NotNull JsonDocument properties
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

  public @NotNull NetworkClusterNode node() {
    return this.node;
  }

  public @NotNull CloudNetVersion version() {
    return this.version;
  }

  public @NotNull ProcessSnapshot processSnapshot() {
    return this.processSnapshot;
  }

  public double maxProcessorUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  public @NotNull Collection<ModuleConfiguration> modules() {
    return this.modules;
  }
}
