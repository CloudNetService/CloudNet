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

package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class provides methods to get information to the services in the cluster.
 *
 * @author derrop (derrop@cloudnetservice.eu)
 * @author Pasqual K. (derklaro@cloudnetservice.eu)
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
  @NotNull SpecificCloudServiceProvider getSpecificProvider(@NotNull UUID serviceUniqueId);

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
  @NotNull SpecificCloudServiceProvider getSpecificProviderByName(@NotNull String serviceName);

  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<UUID> getServicesAsUniqueId();

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<ServiceInfoSnapshot> getCloudServices();

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<ServiceInfoSnapshot> getStartedCloudServices();

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByTask(@NotNull String taskName);

  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param environment the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByEnvironment(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  @UnmodifiableView
  @NotNull Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group);

  /**
   * Gets the amount of services in the cloud
   *
   * @return an integer for the amount of services in the whole cloud
   */
  int getServicesCount();

  /**
   * Gets the amount of services by the given group in the cloud
   *
   * @param group the group every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  int getServicesCountByGroup(@NotNull String group);

  /**
   * Gets the amount of services by the given task in the cloud
   *
   * @param taskName the task every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  int getServicesCountByTask(@NotNull String taskName);

  /**
   * Gets the info of a cloud service by its name
   *
   * @param name the name of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @Nullable
  ServiceInfoSnapshot getCloudServiceByName(@NotNull String name);

  /**
   * Gets the info of a cloud service by its uniqueId
   *
   * @param uniqueId the uniqueId of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @Nullable
  ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId);

  default @NotNull ITask<SpecificCloudServiceProvider> getSpecificProviderAsync(@NotNull UUID serviceUniqueId) {
    return CompletableTask.supply(() -> this.getSpecificProvider(serviceUniqueId));
  }

  default @NotNull ITask<SpecificCloudServiceProvider> getSpecificProviderByNameAsync(@NotNull String serviceName) {
    return CompletableTask.supply(() -> this.getSpecificProviderByName(serviceName));
  }

  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  @NotNull
  default ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
    return CompletableTask.supply(this::getServicesAsUniqueId);
  }

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  @NotNull
  default ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
    return CompletableTask.supply(() -> this.getCloudServices());
  }

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  @NotNull
  default ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
    return CompletableTask.supply(this::getStartedCloudServices);
  }

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  @NotNull
  default ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByTaskAsync(@NotNull String taskName) {
    return CompletableTask.supply(() -> this.getCloudServicesByTask(taskName));
  }

  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param e the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  @NotNull
  default ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByEnvironmentAsync(@NotNull ServiceEnvironmentType e) {
    return CompletableTask.supply(() -> this.getCloudServicesByEnvironment(e));
  }

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  @NotNull
  default ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(@NotNull String group) {
    return CompletableTask.supply(() -> this.getCloudServicesByGroup(group));
  }

  /**
   * Gets the amount of services in the cloud
   *
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  default ITask<Integer> getServicesCountAsync() {
    return CompletableTask.supply(this::getServicesCount);
  }

  /**
   * Gets the amount of services by the given group in the cloud
   *
   * @param group the group every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  default ITask<Integer> getServicesCountByGroupAsync(@NotNull String group) {
    return CompletableTask.supply(() -> this.getServicesCountByGroup(group));
  }

  /**
   * Gets the amount of services by the given task in the cloud
   *
   * @param taskName the task every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  default ITask<Integer> getServicesCountByTaskAsync(@NotNull String taskName) {
    return CompletableTask.supply(() -> this.getServicesCountByTask(taskName));
  }

  /**
   * Gets the info of a cloud service by its name
   *
   * @param name the name of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @NotNull
  default ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.getCloudServiceByName(name));
  }

  /**
   * Gets the info of a cloud service by its uniqueId
   *
   * @param uniqueId the uniqueId of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @NotNull
  default ITask<ServiceInfoSnapshot> getCloudServiceAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supply(() -> this.getCloudService(uniqueId));
  }

}
