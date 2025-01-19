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

package eu.cloudnetservice.wrapper.holder;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * Provides information about the current service info, and methods to update it.
 *
 * @since 4.0
 */
public interface ServiceInfoHolder {

  /**
   * Setups the initial service information, this method can only be called once per lifecycle.
   *
   * @throws IllegalStateException if called when the provider is already initialized.
   */
  void setup();

  /**
   * Get the current service info snapshot which is synced to all components in the network.
   *
   * @return the current service info snapshot.
   */
  @NonNull ServiceInfoSnapshot serviceInfo();

  /**
   * Gets the last service info snapshot before the current one was created.
   *
   * @return the last service info snapshot.
   */
  @NonNull ServiceInfoSnapshot lastServiceInfo();

  /**
   * Creates a new service snapshot of this service, copying over the properties of the current service info snapshot.
   * Only the creation time and process snapshot of the new snapshot differ from the old snapshot.
   *
   * @return the newly created service snapshot for this service.
   */
  @NonNull ServiceInfoSnapshot createServiceInfoSnapshot();

  /**
   * Creates a new service snapshot of this service using the provided properties for the new snapshot. Only the
   * creation time, process snapshot and properties of the new snapshot differ from the old snapshot.
   * <p>
   * Changes made to the current service snapshot (like modifying the properties of it) will not reflect into the
   * snapshot returned by this method.
   *
   * @param properties the properties to apply to the new snapshot.
   * @return the newly created service snapshot for this service.
   * @throws NullPointerException if the given properties document is null.
   */
  @NonNull ServiceInfoSnapshot createServiceInfoSnapshot(@NonNull Document properties);

  /**
   * Creates a new service snapshot of this service, copying over the properties of the current service info snapshot,
   * then configuring it using the registered listeners of the {@code ServiceInfoSnapshotConfigureEvent}.
   * <p>
   * Changes made to the current service snapshot (like modifying the properties of it) will not reflect into the
   * snapshot returned by this method.
   * <p>
   * This method will (unlike the {@code createServiceInfoSnapshot} method) change the current and old service snapshot
   * to the newly created and configured one.
   *
   * @return the newly created service snapshot for this service.
   */
  @NonNull ServiceInfoSnapshot configureServiceInfoSnapshot();

  /**
   * Creates a new service snapshot, configures it, updates the current and old one and sends an update to all
   * components which are currently registered within the CloudNet network.
   */
  void publishServiceInfoUpdate();

  /**
   * Updates the given service snapshot to all components which are currently registered within the CloudNet network.
   * This method will configure the given snapshot if it belongs to the current wrapper instance.
   *
   * @param serviceInfoSnapshot the service snapshot to update.
   * @throws NullPointerException if the given service snapshot is null.
   */
  void publishServiceInfoUpdate(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);
}
