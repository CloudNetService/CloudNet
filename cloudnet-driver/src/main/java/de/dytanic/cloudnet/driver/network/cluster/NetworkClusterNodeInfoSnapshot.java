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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeInfoSnapshot extends SerializableJsonDocPropertyable implements SerializableObject {

  public static final Type TYPE = new TypeToken<NetworkClusterNodeInfoSnapshot>() {
  }.getType();

  protected long creationTime;
  protected long startupMillis;
  protected NetworkClusterNode node;
  protected String version;
  protected int currentServicesCount;
  protected int usedMemory;
  protected int reservedMemory;
  protected int maxMemory;
  protected double maxCPUUsageToStartServices;
  protected ProcessSnapshot processSnapshot;
  protected Collection<ModuleConfiguration> modules;
  private double systemCpuUsage;

  /**
   * @deprecated Use {@link #NetworkClusterNodeInfoSnapshot(long, long, NetworkClusterNode, String, int, int, int, int,
   * double, ProcessSnapshot, Collection, double)} instead
   */
  @Deprecated
  public NetworkClusterNodeInfoSnapshot(long creationTime,
    NetworkClusterNode node,
    String version,
    int currentServicesCount,
    int usedMemory,
    int reservedMemory,
    int maxMemory,
    ProcessSnapshot processSnapshot,
    Collection<ModuleConfiguration> modules,
    double systemCpuUsage) {
    this(
      creationTime,
      System.nanoTime(),
      node,
      version,
      currentServicesCount,
      usedMemory,
      reservedMemory,
      maxMemory,
      0,
      processSnapshot,
      modules,
      systemCpuUsage
    );
  }

  public NetworkClusterNodeInfoSnapshot(long creationTime,
    long startupMillis,
    NetworkClusterNode node,
    String version,
    int currentServicesCount,
    int usedMemory,
    int reservedMemory,
    int maxMemory,
    double maxCPUUsageToStartServices,
    ProcessSnapshot processSnapshot,
    Collection<ModuleConfiguration> modules,
    double systemCpuUsage) {
    this.creationTime = creationTime;
    this.startupMillis = startupMillis;
    this.node = node;
    this.version = version;
    this.currentServicesCount = currentServicesCount;
    this.usedMemory = usedMemory;
    this.reservedMemory = reservedMemory;
    this.maxMemory = maxMemory;
    this.maxCPUUsageToStartServices = maxCPUUsageToStartServices;
    this.processSnapshot = processSnapshot;
    this.modules = modules;
    this.systemCpuUsage = systemCpuUsage;
  }

  public NetworkClusterNodeInfoSnapshot() {
  }

  public long getCreationTime() {
    return this.creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  public long getStartupMillis() {
    return this.startupMillis;
  }

  public NetworkClusterNode getNode() {
    return this.node;
  }

  public void setNode(NetworkClusterNode node) {
    this.node = node;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public int getCurrentServicesCount() {
    return this.currentServicesCount;
  }

  public void setCurrentServicesCount(int currentServicesCount) {
    this.currentServicesCount = currentServicesCount;
  }

  public int getUsedMemory() {
    return this.usedMemory;
  }

  public void setUsedMemory(int usedMemory) {
    this.usedMemory = usedMemory;
  }

  public int getReservedMemory() {
    return this.reservedMemory;
  }

  public void setReservedMemory(int reservedMemory) {
    this.reservedMemory = reservedMemory;
  }

  public void addReservedMemory(int reservedMemory) {
    this.reservedMemory += reservedMemory;
  }

  public int getMaxMemory() {
    return this.maxMemory;
  }

  public void setMaxMemory(int maxMemory) {
    this.maxMemory = maxMemory;
  }

  public double getMaxCPUUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  public void setMaxCPUUsageToStartServices(double maxCPUUsageToStartServices) {
    this.maxCPUUsageToStartServices = maxCPUUsageToStartServices;
  }

  public ProcessSnapshot getProcessSnapshot() {
    return this.processSnapshot;
  }

  public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
    this.processSnapshot = processSnapshot;
  }

  public Collection<ModuleConfiguration> getModules() {
    return this.modules;
  }

  public void setModules(Collection<ModuleConfiguration> modules) {
    this.modules = modules;
  }

  public double getSystemCpuUsage() {
    return this.systemCpuUsage;
  }

  public void setSystemCpuUsage(double systemCpuUsage) {
    this.systemCpuUsage = systemCpuUsage;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeLong(this.creationTime);
    buffer.writeLong(this.startupMillis);
    buffer.writeObject(this.node);
    buffer.writeString(this.version);
    buffer.writeInt(this.currentServicesCount);
    buffer.writeInt(this.usedMemory);
    buffer.writeInt(this.reservedMemory);
    buffer.writeInt(this.maxMemory);
    buffer.writeDouble(this.maxCPUUsageToStartServices);
    buffer.writeObject(this.processSnapshot);
    buffer.writeObjectCollection(this.modules);
    buffer.writeDouble(this.systemCpuUsage);

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.creationTime = buffer.readLong();
    this.startupMillis = buffer.readLong();
    this.node = buffer.readObject(NetworkClusterNode.class);
    this.version = buffer.readString();
    this.currentServicesCount = buffer.readInt();
    this.usedMemory = buffer.readInt();
    this.reservedMemory = buffer.readInt();
    this.maxMemory = buffer.readInt();
    this.maxCPUUsageToStartServices = buffer.readDouble();
    this.processSnapshot = buffer.readObject(ProcessSnapshot.class);
    this.modules = buffer.readObjectCollection(ModuleConfiguration.class);
    this.systemCpuUsage = buffer.readDouble();

    super.read(buffer);
  }
}
