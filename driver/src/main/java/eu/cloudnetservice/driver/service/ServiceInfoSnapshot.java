/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.service;

import com.google.common.collect.ComparisonChain;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.JsonDocPropertyHolder;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.property.ServiceProperty;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents the state of a service at the snapshot creation time. A service snapshot (once created) will never change
 * its state again. If the latest information is needed from a service, make sure you actually force the service into
 * updating its last snapshot to get a new, clean version of it.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends JsonDocPropertyHolder
  implements Nameable, Cloneable, Comparable<ServiceInfoSnapshot> {

  protected final long creationTime;

  protected final HostAndPort address;

  protected final ProcessSnapshot processSnapshot;
  protected final ServiceConfiguration configuration;

  protected final long connectedTime;
  protected final ServiceLifeCycle lifeCycle;

  /**
   * Constructs a new service info snapshot. This constructor is for internal use only, there should be no reason
   * normally why you need to create a service info snapshot instance yourself.
   *
   * @param creationTime    the unix timestamp of the snapshot creation time.
   * @param address         the address to which the service was bound if started.
   * @param processSnapshot the current snapshot of the process resource usage.
   * @param configuration   the configuration base used to create the service.
   * @param connectedTime   the time when the service connected to the node, -1 if not yet connected.
   * @param lifeCycle       the lifecycle the service is in when creating the snapshot.
   * @param properties      the properties of the service.
   * @throws NullPointerException if one of the constructor parameters is null.
   */
  @ApiStatus.Internal
  public ServiceInfoSnapshot(
    long creationTime,
    @NonNull HostAndPort address,
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
    this.lifeCycle = lifeCycle;
    this.processSnapshot = processSnapshot;
    this.configuration = configuration;
  }

  /**
   * Get the unix timestamp when this snapshot was created. No changes on the service since that timestamp are reflected
   * into this snapshot.
   *
   * @return the creation timestamp of this snapshot.
   */
  public long creationTime() {
    return this.creationTime;
  }

  /**
   * Get the service id of the service this snapshot was created for.
   *
   * @return the id of the service wrapped by this snapshot.
   */
  public @NonNull ServiceId serviceId() {
    return this.configuration.serviceId();
  }

  /**
   * Get the address to which the service was / will be bound when started.
   *
   * @return the address of the service.
   */
  public @NonNull HostAndPort address() {
    return this.address;
  }

  /**
   * Get if the wrapped service was started and is connected to the node which started it.
   *
   * @return true if the service is connected to the handling node, false otherwise.
   */
  public boolean connected() {
    return this.connectedTime != -1;
  }

  /**
   * Get the unix timestamp when the service connected to the handling node. This method returns -1 when the service is
   * not yet connected to the node.
   *
   * @return the unix timestamp when the service connected to the handling node.
   */
  public long connectedTime() {
    return this.connectedTime;
  }

  /**
   * Get the current lifecycle of the service at the snapshot creation time.
   *
   * @return the lifecycle of the service at the snapshot creation time.
   */
  public @NonNull ServiceLifeCycle lifeCycle() {
    return this.lifeCycle;
  }

  /**
   * Get the process snapshot of the service process at the creation time of this snapshot.
   *
   * @return the process snapshot of the service.
   */
  public @NonNull ProcessSnapshot processSnapshot() {
    return this.processSnapshot;
  }

  /**
   * Get the configuration which was used to create the service this snapshot wraps.
   *
   * @return the configuration for the service wrapped by this snapshot.
   */
  public @NonNull ServiceConfiguration configuration() {
    return this.configuration;
  }

  /**
   * Get the associated service provider for the service wrapped by this snapshot. This method returns the exact
   * snapshot based on the unique id of the wrapped service. The returned provider might be a no-op one if the service
   * wrapped by this snapshot no longer exists.
   *
   * @return a service provider targeting the current wrapped service.
   */
  public @NonNull SpecificCloudServiceProvider provider() {
    var serviceProvider = InjectionLayer.boot().instance(CloudServiceProvider.class);
    return serviceProvider.serviceProvider(this.serviceId().uniqueId());
  }

  /**
   * Tries to read the given service property from the properties set in this snapshot.
   *
   * @param property the property to read.
   * @param <T>      the type which gets read by the given property.
   * @return the value of the property assigned to this snapshot, null if no value is associated.
   * @throws NullPointerException if the given property is null.
   */
  public <T> @Nullable T property(@NonNull ServiceProperty<T> property) {
    return property.read(this);
  }

  /**
   * Tries to read the given service property from the properties set in this snapshot.
   *
   * @param property the property to read.
   * @param <T>      the type which gets read by the given property.
   * @param def      the value to return if the given property is not set in this snapshot.
   * @return the value of the property assigned to this snapshot, the given default value if no value is associated.
   * @throws NullPointerException if the given property is null.
   */
  public <T> @UnknownNullability T propertyOr(@NonNull ServiceProperty<T> property, @Nullable T def) {
    return property.readOr(this, def);
  }

  /**
   * Writes the given property into the properties set in the properties of this snapshot. An update of the snapshot is
   * required in order to allow all services to see the change made to the snapshot.
   *
   * @param property the property to write.
   * @param value    the value of the property to write.
   * @param <T>      the type which gets written by the given property.
   * @throws NullPointerException if the given property is null.
   */
  public <T> void property(@NonNull ServiceProperty<T> property, @Nullable T value) {
    property.write(this, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.serviceId().name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return ComparisonChain.start()
      .compare(this.serviceId().taskName(), serviceInfoSnapshot.serviceId().taskName())
      .compare(this.serviceId().taskServiceId(), serviceInfoSnapshot.serviceId().taskServiceId())
      .result();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot clone() {
    try {
      return (ServiceInfoSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }
}
