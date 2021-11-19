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
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends JsonDocPropertyHolder
  implements INameable, Cloneable, Comparable<ServiceInfoSnapshot> {

  protected final long creationTime;

  protected final HostAndPort address;
  protected final HostAndPort connectAddress;

  protected final ProcessSnapshot processSnapshot;
  protected final ServiceConfiguration configuration;

  protected volatile long connectedTime;
  protected volatile ServiceLifeCycle lifeCycle;

  @Internal
  public ServiceInfoSnapshot(
    long creationTime,
    @NotNull HostAndPort address,
    @NotNull HostAndPort connectAddress,
    @NotNull ProcessSnapshot processSnapshot,
    @NotNull ServiceConfiguration configuration,
    long connectedTime,
    @NotNull ServiceLifeCycle lifeCycle,
    @NotNull JsonDocument properties
  ) {
    this.creationTime = creationTime;
    this.connectedTime = connectedTime;
    this.address = address;
    this.connectAddress = connectAddress;
    this.lifeCycle = lifeCycle;
    this.processSnapshot = processSnapshot;
    this.configuration = configuration;
    this.properties = properties;
  }

  public long getCreationTime() {
    return this.creationTime;
  }

  public @NotNull ServiceId getServiceId() {
    return this.configuration.getServiceId();
  }

  public @NotNull HostAndPort getAddress() {
    return this.address;
  }

  public @NotNull HostAndPort getConnectAddress() {
    return this.connectAddress;
  }

  public boolean isConnected() {
    return this.connectedTime != -1;
  }

  public long getConnectedTime() {
    return this.connectedTime;
  }

  @Internal
  public void setConnectedTime(long connectedTime) {
    this.connectedTime = connectedTime;
  }

  public ServiceLifeCycle getLifeCycle() {
    return this.lifeCycle;
  }

  @Internal
  public void setLifeCycle(ServiceLifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  public @NotNull ProcessSnapshot getProcessSnapshot() {
    return this.processSnapshot;
  }

  public @NotNull ServiceConfiguration getConfiguration() {
    return this.configuration;
  }

  public @NotNull SpecificCloudServiceProvider provider() {
    return CloudNetDriver.getInstance()
      .getCloudServiceProvider()
      .getSpecificProvider(this.getServiceId().getUniqueId());
  }

  public <T> @NotNull Optional<T> getProperty(@NotNull ServiceProperty<T> property) {
    return property.get(this);
  }

  public <T> void setProperty(@NotNull ServiceProperty<T> property, @Nullable T value) {
    property.set(this, value);
  }

  @Override
  public @NotNull String getName() {
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
  public @NotNull ServiceInfoSnapshot clone() {
    try {
      return (ServiceInfoSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }
}
