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

package eu.cloudnetservice.driver.provider;

import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/**
 * The main factory class to create cloud services based on a given service configuration. This factory is designed to
 * automatically run all needed checks against the given configuration and ensures that services are split in the best
 * way onto all nodes in the cluster, while respecting all settings made to the service configuration (for example if
 * the service configuration already has the node set to start the service on, the factory will think about on which
 * node it should start the service).
 * <p>
 * The taken input configurations are always cloned before being used, so there is no need to do this manually. This is
 * due to the fact, that
 * <ol>
 *   <li>there might be changes made by the system to the given configuration which should not get reflected to the
 *   later created services based on the configuration.
 *   <li>changes made to the configuration should not get reflected into the service, this might lead to unexpected
 *   behaviour of the system.
 * </ol>
 * <p>
 * A method call to any of the create methods will always generate a different result if the service was created
 * successfully, even if the same configuration was used multiple times. You can safely assume, that a service created
 * by this factory is <strong>ALWAYS</strong> unique.
 * <p>
 * All factory methods are thread safe, calling them with the same configuration across multiple threads will not lead
 * to duplicate service results.
 *
 * @since 4.0
 */
public interface CloudServiceFactory {

  /**
   * Creates and prepares a new cloud service based on the given configuration. This method can be called with the same
   * configuration multiple times and will always (if the service was created successfully) in a different result.
   * <p>
   * This method will return a result with the state set to {@code FAILED} if the service could not get created. This
   * can happen (for example) if:
   * <ol>
   *   <li>no node in the cluster can currently pick up the service (for example if all nodes are draining).
   *   <li>the configured node in the configuration is not connected or draining.
   *   <li>the selected node to start the service did not respond within a given time (uncommon).
   * </ol>
   * This list only includes some common reasons for the service not getting created. Not that there is never a
   * guarantee that the method returns a (non-null) service result.
   * <p>
   * The result of the service creation will only have a state of {@code DEFERRED} if a retry configuration was provided
   * to the given service configuration.
   *
   * @param serviceConfiguration the configuration to base the newly created service on.
   * @return a result representing the state of the service creation.
   * @throws NullPointerException if the given service configuration is null.
   */
  @NonNull
  ServiceCreateResult createCloudService(@NonNull ServiceConfiguration serviceConfiguration);

  /**
   * Creates and prepares a new cloud service based on the given configuration. This method can be called with the same
   * configuration multiple times and will always (if the service was created successfully) in a different result.
   * <p>
   * This method will return a task completed with null if the service could not get created. This can happen (for
   * example) if:
   * <ol>
   *   <li>no node in the cluster can currently pick up the service (for example if all nodes are draining).
   *   <li>the configured node in the configuration is not connected or draining.
   *   <li>the selected node to start the service did not respond within a given time (uncommon).
   * </ol>
   * This list only includes some common reasons for the service not getting created. Not that there is never a
   * guarantee that the method returns a (non-null) service result.
   * <p>
   * The result of the service creation will only have a state of {@code DEFERRED} if a retry configuration was provided
   * to the given service configuration.
   *
   * @param configuration the configuration to base the newly created service on.
   * @return a task completed with a result representing the state of the service creation.
   * @throws NullPointerException if the given service configuration is null.
   */
  @NonNull
  CompletableFuture<ServiceCreateResult> createCloudServiceAsync(@NonNull ServiceConfiguration configuration);
}
