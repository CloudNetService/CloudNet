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

package de.dytanic.cloudnet.driver.service;

import com.google.common.collect.ComparisonChain;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import java.lang.reflect.Type;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends SerializableJsonDocPropertyable implements INameable,
  Comparable<ServiceInfoSnapshot>, SerializableObject {

  public static final Type TYPE = ServiceInfoSnapshot.class;

  protected long creationTime;

  protected HostAndPort address;

  protected HostAndPort connectAddress;

  protected long connectedTime;

  protected ServiceLifeCycle lifeCycle;

  protected ProcessSnapshot processSnapshot;

  protected ServiceConfiguration configuration;

  public ServiceInfoSnapshot(long creationTime, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle,
    ProcessSnapshot processSnapshot, ServiceConfiguration configuration) {
    this(creationTime, address, connectedTime, lifeCycle, processSnapshot, JsonDocument.newDocument(), configuration);
  }

  public ServiceInfoSnapshot(long creationTime, HostAndPort address, long connectedTime, ServiceLifeCycle lifeCycle,
    ProcessSnapshot processSnapshot, JsonDocument properties, ServiceConfiguration configuration) {
    this(creationTime, address, address, connectedTime, lifeCycle, processSnapshot, properties, configuration);
  }

  public ServiceInfoSnapshot(long creationTime, HostAndPort address, HostAndPort connectAddress, long connectedTime,
    ServiceLifeCycle lifeCycle, ProcessSnapshot processSnapshot, JsonDocument properties,
    ServiceConfiguration configuration) {
    this.creationTime = creationTime;
    this.address = address;
    this.connectAddress = connectAddress;
    this.connectedTime = connectedTime;
    this.lifeCycle = lifeCycle;
    this.processSnapshot = processSnapshot;
    this.properties = properties;
    this.configuration = configuration;
  }

  public ServiceInfoSnapshot() {
  }

  /**
   * Returns the creation time of this ServiceInfoSnapshot. Used is a Unix timestamp {@link System#currentTimeMillis()}
   *
   * @return the creation time of the ServiceInfoSnapshot
   */
  public long getCreationTime() {
    return this.creationTime;
  }

  /**
   * Returns the {@link ServiceId}, containing the name of the Task, the uniqueId and more
   *
   * @return the serviceId of this ServiceInfoSnapshot
   */
  public ServiceId getServiceId() {
    return this.configuration.getServiceId();
  }

  public HostAndPort getAddress() {
    return this.address;
  }

  public HostAndPort getConnectAddress() {
    return this.connectAddress;
  }

  /**
   * @return if the service is connected to the Node or not.
   */
  public boolean isConnected() {
    return this.connectedTime != -1;
  }

  /**
   * @return the unix timestamp when this ServiceInfoSnapshot was connected to it's Node
   */
  public long getConnectedTime() {
    return this.connectedTime;
  }

  /**
   * Sets the unix timestamp since which the service is connected to the Node
   *
   * @param connectedTime the unix timestamp
   */
  public void setConnectedTime(long connectedTime) {
    this.connectedTime = connectedTime;
  }

  /**
   * @return the current {@link ServiceLifeCycle} of the ServiceInfoSnapshot
   */
  public ServiceLifeCycle getLifeCycle() {
    return this.lifeCycle;
  }

  /**
   * Sets the lifeCycle of this ServiceInfoSnapshot to the given one
   *
   * @param lifeCycle the lifecycle to be set
   */
  public void setLifeCycle(ServiceLifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  /**
   * @return the {@link ProcessSnapshot} of this ServiceInfoSnapshot, containing the cpu usage, memory usage and other
   * process related things
   */
  public ProcessSnapshot getProcessSnapshot() {
    return this.processSnapshot;
  }

  /**
   * Sets the processSnapshot of this ServiceInfoSnapshot to the given one
   *
   * @param processSnapshot the new {@link ProcessSnapshot} for this ServiceInfoSnapshot
   */
  public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
    this.processSnapshot = processSnapshot;
  }

  /**
   * @return the corresponding ServiceConfiguration for this ServiceInfoSnapshot, containing mostly settings from the
   * {@link GroupConfiguration} and {@link ServiceTask}
   */
  public ServiceConfiguration getConfiguration() {
    return this.configuration;
  }

  /**
   * Returns a new service specific CloudServiceProvider
   * <p>
   * See {@link CloudNetDriver#getCloudServiceProvider(ServiceInfoSnapshot)}
   *
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public SpecificCloudServiceProvider provider() {
    return CloudNetDriver.getInstance().getCloudServiceProvider(this);
  }

  /**
   * Reads a specific property out of all the properties of the ServiceInfoSnapshot.
   * <p>
   * See {@link ServiceConfiguration#getProperties()} and {@link ServiceProperty#get(ServiceInfoSnapshot)}
   *
   * @param property the property to look for
   * @param <T>      the return type of the property
   * @return a {@link Optional<T>} with the result of the lookup
   */
  @NotNull
  public <T> Optional<T> getProperty(@NotNull ServiceProperty<T> property) {
    return property.get(this);
  }

  /**
   * Sets a specific property in the ServiceInfoSnapshot
   * <p>
   * See {@link ServiceProperty#set(ServiceInfoSnapshot, Object)}
   *
   * @param property the property to assign the new value to
   * @param value    the new value to be assigned
   * @param <T>      the generic type of the {@link ServiceProperty}
   */
  public <T> void setProperty(@NotNull ServiceProperty<T> property, @Nullable T value) {
    property.set(this, value);
  }

  /**
   * @return the name of this ServiceInfoSnapshot (e.g. "Lobby-187")
   */
  @Override
  public String getName() {
    return this.getServiceId().getName();
  }

  @Override
  public int compareTo(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return ComparisonChain.start()
      .compare(this.getServiceId().getTaskName(), serviceInfoSnapshot.getServiceId().getTaskName())
      .compare(this.getServiceId().getTaskServiceId(), serviceInfoSnapshot.getServiceId().getTaskServiceId())
      .result();
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeLong(this.creationTime);
    buffer.writeObject(this.address);
    buffer.writeObject(this.connectAddress);
    buffer.writeLong(this.connectedTime);
    buffer.writeEnumConstant(this.lifeCycle);
    buffer.writeObject(this.processSnapshot);
    buffer.writeObject(this.configuration);

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.creationTime = buffer.readLong();
    this.address = buffer.readObject(HostAndPort.class);
    this.connectAddress = buffer.readObject(HostAndPort.class);
    this.connectedTime = buffer.readLong();
    this.lifeCycle = buffer.readEnumConstant(ServiceLifeCycle.class);
    this.processSnapshot = buffer.readObject(ProcessSnapshot.class);
    this.configuration = buffer.readObject(ServiceConfiguration.class);

    super.read(buffer);
  }
}
