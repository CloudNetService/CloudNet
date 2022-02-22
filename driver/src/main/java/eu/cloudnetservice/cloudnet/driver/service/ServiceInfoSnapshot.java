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

package eu.cloudnetservice.cloudnet.driver.service;

import com.google.common.collect.ComparisonChain;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.property.ServiceProperty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends JsonDocPropertyHolder
  implements Nameable, Cloneable, Comparable<ServiceInfoSnapshot> {

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
    @NonNull HostAndPort address,
    @NonNull HostAndPort connectAddress,
    @NonNull ProcessSnapshot processSnapshot,
    @NonNull ServiceConfiguration configuration,
    long connectedTime,
    @NonNull ServiceLifeCycle lifeCycle,
    @NonNull JsonDocument properties
  ) {
    super(properties);
    this.creationTime = creationTime;
    this.connectedTime = connectedTime;
    this.address = address;
    this.connectAddress = connectAddress;
    this.lifeCycle = lifeCycle;
    this.processSnapshot = processSnapshot;
    this.configuration = configuration;
  }

  public long creationTime() {
    return this.creationTime;
  }

  public @NonNull ServiceId serviceId() {
    return this.configuration.serviceId();
  }

  public @NonNull HostAndPort address() {
    return this.address;
  }

  public @NonNull HostAndPort connectAddress() {
    return this.connectAddress;
  }

  public boolean connected() {
    return this.connectedTime != -1;
  }

  public long connectedTime() {
    return this.connectedTime;
  }

  @Internal
  public void connectedTime(long connectedTime) {
    this.connectedTime = connectedTime;
  }

  public ServiceLifeCycle lifeCycle() {
    return this.lifeCycle;
  }

  @Internal
  public void lifeCycle(ServiceLifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  public @NonNull ProcessSnapshot processSnapshot() {
    return this.processSnapshot;
  }

  public @NonNull ServiceConfiguration configuration() {
    return this.configuration;
  }

  public @NonNull SpecificCloudServiceProvider provider() {
    return CloudNetDriver.instance()
      .cloudServiceProvider()
      .serviceProvider(this.serviceId().uniqueId());
  }

  public <T> @NonNull Optional<T> property(@NonNull ServiceProperty<T> property) {
    return property.read(this);
  }

  public <T> void property(@NonNull ServiceProperty<T> property, @Nullable T value) {
    property.write(this, value);
  }

  @Override
  public @NonNull String name() {
    return this.serviceId().name();
  }

  @Override
  public int compareTo(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return ComparisonChain.start()
      .compare(this.serviceId().taskName(), serviceInfoSnapshot.serviceId().taskName())
      .compare(this.serviceId().taskServiceId(), serviceInfoSnapshot.serviceId().taskServiceId())
      .result();
  }

  @Override
  public @NonNull ServiceInfoSnapshot clone() {
    try {
      return (ServiceInfoSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }
}
