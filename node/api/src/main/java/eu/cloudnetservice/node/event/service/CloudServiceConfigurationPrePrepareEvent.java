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

package eu.cloudnetservice.node.event.service;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.service.CloudServiceManager;
import lombok.NonNull;

/**
 * An event that is called before the actual preparation (setting the service id, including group templates etc.) of a
 * service configuration is done. This event is called in the last stage before the service gets created. The
 * configuration can still be modified without the risk of causing configuration issues.
 * <p>
 * Note that this event is only called on the head node, as no other node is allowed to start services nor prepare the
 * configuration for them.
 *
 * @since 4.0
 */
public final class CloudServiceConfigurationPrePrepareEvent extends Event {

  private final CloudServiceManager cloudServiceManager;
  private final ServiceConfiguration originalConfiguration;
  private final ServiceConfiguration.Builder modifiableConfiguration;

  /**
   * Constructs a new cloud service configuration pre-prepare event instance.
   *
   * @param cloudServiceManager     the service manager which will keep track of the service after the creation.
   * @param originalConfiguration   the configuration on which basis the service should get created.
   * @param modifiableConfiguration a modifiable copy of the original configuration with no changes applied.
   * @throws NullPointerException if the given service manager, original or modifiable configuration is null.
   */
  public CloudServiceConfigurationPrePrepareEvent(
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull ServiceConfiguration originalConfiguration,
    @NonNull ServiceConfiguration.Builder modifiableConfiguration
  ) {
    this.cloudServiceManager = cloudServiceManager;
    this.originalConfiguration = originalConfiguration;
    this.modifiableConfiguration = modifiableConfiguration;
  }

  /**
   * Get the service manager that will keep track of the service after creation.
   *
   * @return the service manager that will keep track of the service after creation.
   */
  public @NonNull CloudServiceManager cloudServiceManager() {
    return this.cloudServiceManager;
  }

  /**
   * Get the original, unmodifiable version of the service configuration which was passed in to create a service from.
   *
   * @return the original service configuration passed to create the service.
   */
  public @NonNull ServiceConfiguration originalConfiguration() {
    return this.originalConfiguration;
  }

  /**
   * Get a modifiable version of the original service configuration. Changes made to the configuration will reflect into
   * the service creation process and take effect immediately.
   *
   * @return a modifiable copy of the original configuration which allows changes to be applied to it.
   */
  public @NonNull ServiceConfiguration.Builder modifiableConfiguration() {
    return this.modifiableConfiguration;
  }
}
