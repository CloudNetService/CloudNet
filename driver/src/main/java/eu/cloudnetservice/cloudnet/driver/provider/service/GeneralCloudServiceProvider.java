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

package eu.cloudnetservice.cloudnet.driver.provider.service;

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class provides methods to get information to the services in the cluster.
 */
@RPCValidation
public interface GeneralCloudServiceProvider {

  /**
   * Gets a provider for the specific service snapshot with the given unique id. No check is made if the service this
   * provider was created for actually exists. If a provider gets created for a not-existing service, then calling any
   * method will result in a dummy return value and do nothing. Any service which will be created with the given unique
   * id at any time can be managed with the created provider.
   *
   * @param serviceUniqueId the unique id of the service to get the provider for.
   * @return a {@link SpecificCloudServiceProvider} to manage the service with the given unique id.
   * @since 3.5
   */
  @NonNull SpecificCloudServiceProvider specificProvider(@NonNull UUID serviceUniqueId);

  /**
   * Gets a provider for the specific service snapshot with the given name. A service name is unique within a CloudNet
   * cluster. No check is made if the service this provider was created for actually exists. If a provider gets created
   * for a not-existing service, then calling any method will result in a dummy return value and do nothing. Any service
   * which will be created with the given name at any time can be managed with the created provider.
   *
   * @param serviceName the name of the service to get the provider for.
   * @return a {@link SpecificCloudServiceProvider} to manage the service with the given name.
   * @since 3.5
   */
  @NonNull SpecificCloudServiceProvider specificProviderByName(@NonNull String serviceName);

  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<UUID> servicesAsUniqueId();

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<ServiceInfoSnapshot> services();

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<ServiceInfoSnapshot> runningServices();

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<ServiceInfoSnapshot> servicesByTask(@NonNull String taskName);

  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param environment the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<ServiceInfoSnapshot> servicesByEnvironment(@NonNull ServiceEnvironmentType environment);

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  @UnmodifiableView
  @NonNull Collection<ServiceInfoSnapshot> servicesByGroup(@NonNull String group);

  /**
   * Gets the amount of services in the cloud
   *
   * @return an integer for the amount of services in the whole cloud
   */
  int serviceCount();

  /**
   * Gets the amount of services by the given group in the cloud
   *
   * @param group the group every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  int serviceCountByGroup(@NonNull String group);

  /**
   * Gets the amount of services by the given task in the cloud
   *
   * @param taskName the task every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  int serviceCountByTask(@NonNull String taskName);

  /**
   * Gets the info of a cloud service by its name
   *
   * @param name the name of the service
   * @return the info of the service or null if the service doesn't exist
   */
  @Nullable ServiceInfoSnapshot serviceByName(@NonNull String name);

  /**
   * Gets the info of a cloud service by its uniqueId
   *
   * @param uniqueId the uniqueId of the service
   * @return the info of the service or null if the service doesn't exist
   */
  @Nullable ServiceInfoSnapshot service(@NonNull UUID uniqueId);

  default @NonNull Task<SpecificCloudServiceProvider> specificProviderAsync(@NonNull UUID serviceUniqueId) {
    return CompletableTask.supply(() -> this.specificProvider(serviceUniqueId));
  }

  default @NonNull Task<SpecificCloudServiceProvider> specificProviderByNameAsync(@NonNull String serviceName) {
    return CompletableTask.supply(() -> this.specificProviderByName(serviceName));
  }

  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  default @NonNull Task<Collection<UUID>> servicesAsUniqueIdAsync() {
    return CompletableTask.supply(this::servicesAsUniqueId);
  }

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  default @NonNull Task<Collection<ServiceInfoSnapshot>> servicesAsync() {
    return CompletableTask.supply(this::services);
  }

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  default @NonNull Task<Collection<ServiceInfoSnapshot>> runningServicesAsync() {
    return CompletableTask.supply(this::runningServices);
  }

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  default @NonNull Task<Collection<ServiceInfoSnapshot>> servicesByTaskAsync(@NonNull String taskName) {
    return CompletableTask.supply(() -> this.servicesByTask(taskName));
  }

  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param environmentType the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  default @NonNull Task<Collection<ServiceInfoSnapshot>> servicesByEnvironmentAsync(
    @NonNull ServiceEnvironmentType environmentType
  ) {
    return CompletableTask.supply(() -> this.servicesByEnvironment(environmentType));
  }

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  default @NonNull Task<Collection<ServiceInfoSnapshot>> servicesByGroupAsync(@NonNull String group) {
    return CompletableTask.supply(() -> this.servicesByGroup(group));
  }

  /**
   * Gets the amount of services in the cloud
   *
   * @return an integer for the amount of services in the whole cloud
   */
  default @NonNull Task<Integer> serviceCountAsync() {
    return CompletableTask.supply(this::serviceCount);
  }

  /**
   * Gets the amount of services by the given group in the cloud
   *
   * @param group the group every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  default @NonNull Task<Integer> serviceCountByGroupAsync(@NonNull String group) {
    return CompletableTask.supply(() -> this.serviceCountByGroup(group));
  }

  /**
   * Gets the amount of services by the given task in the cloud
   *
   * @param taskName the task every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  default @NonNull Task<Integer> serviceCountByTaskAsync(@NonNull String taskName) {
    return CompletableTask.supply(() -> this.serviceCountByTask(taskName));
  }

  /**
   * Gets the info of a cloud service by its name
   *
   * @param name the name of the service
   * @return the info of the service or null if the service doesn't exist
   */
  default @NonNull Task<ServiceInfoSnapshot> serviceByNameAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.serviceByName(name));
  }

  /**
   * Gets the info of a cloud service by its uniqueId
   *
   * @param uniqueId the uniqueId of the service
   * @return the info of the service or null if the service doesn't exist
   */
  default @NonNull Task<ServiceInfoSnapshot> serviceAsync(@NonNull UUID uniqueId) {
    return CompletableTask.supply(() -> this.service(uniqueId));
  }
}
