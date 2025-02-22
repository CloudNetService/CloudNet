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

package eu.cloudnetservice.wrapper.impl.holder;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import eu.cloudnetservice.wrapper.event.ServiceInfoSnapshotPublishEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

/**
 * The default implementation of a service info holder for the wrapper.
 *
 * @since 4.0
 */
@Singleton
@Provides(ServiceInfoHolder.class)
public final class WrapperServiceInfoHolder implements ServiceInfoHolder {

  private final EventManager eventManager;
  private final WrapperConfiguration configuration;

  private volatile ServiceInfoSnapshot lastServiceInfoSnapshot;
  private volatile ServiceInfoSnapshot currentServiceInfoSnapshot;

  @Inject
  private WrapperServiceInfoHolder(@NonNull EventManager eventManager, @NonNull WrapperConfiguration configuration) {
    this.eventManager = eventManager;
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setup() {
    Preconditions.checkState(this.currentServiceInfoSnapshot == null, "Cannot setup twice");

    var suppliedServiceSnapshot = this.configuration.serviceInfoSnapshot();
    // init the last snapshot and the current snapshot
    this.lastServiceInfoSnapshot = suppliedServiceSnapshot;
    this.currentServiceInfoSnapshot = new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      suppliedServiceSnapshot.address(),
      ProcessSnapshot.self(),
      suppliedServiceSnapshot.configuration(),
      System.currentTimeMillis(),
      ServiceLifeCycle.RUNNING,
      suppliedServiceSnapshot.propertyHolder());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    return this.currentServiceInfoSnapshot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot lastServiceInfo() {
    return this.lastServiceInfoSnapshot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot createServiceInfoSnapshot() {
    return this.createServiceInfoSnapshot(this.serviceInfo().propertyHolder());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot createServiceInfoSnapshot(@NonNull Document properties) {
    // call the event to configure the service properties
    var info = this.serviceInfo();
    var event = this.eventManager.callEvent(new ServiceInfoPropertiesConfigureEvent(properties.mutableCopy(), info));

    // construct the new service snapshot based on the old info & the configured properties
    return new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      info.address(),
      ProcessSnapshot.self(),
      this.configuration.serviceConfiguration(),
      info.connectedTime(),
      ServiceLifeCycle.RUNNING,
      event.propertyHolder().immutableCopy());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceInfoSnapshot configureServiceInfoSnapshot() {
    var serviceInfoSnapshot = this.createServiceInfoSnapshot();
    this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    return serviceInfoSnapshot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void publishServiceInfoUpdate() {
    this.publishServiceInfoUpdate(this.createServiceInfoSnapshot());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void publishServiceInfoUpdate(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    // add configuration stuff when updating the current service snapshot
    if (this.configuration.serviceConfiguration().serviceId().equals(serviceInfoSnapshot.serviceId())) {
      this.configureServiceInfoSnapshot(serviceInfoSnapshot);
    }

    // send the update to all nodes and services
    ChannelMessage.builder()
      .targetAll()
      .message("update_service_info")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(serviceInfoSnapshot))
      .build()
      .send();
  }

  /**
   * Configures the given service info snapshot and updates the current and old service snapshot.
   *
   * @param serviceInfoSnapshot the service snapshot to configure.
   * @throws NullPointerException if the given snapshot is null.
   */
  private void configureServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.eventManager.callEvent(new ServiceInfoSnapshotPublishEvent(serviceInfoSnapshot));

    // update the current & last snapshot
    this.lastServiceInfoSnapshot = this.currentServiceInfoSnapshot;
    this.currentServiceInfoSnapshot = serviceInfoSnapshot;
  }
}
