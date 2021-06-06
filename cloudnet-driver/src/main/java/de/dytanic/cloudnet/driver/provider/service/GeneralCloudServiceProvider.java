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

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides methods to get information to the services in the cluster.
 */
public interface GeneralCloudServiceProvider {

  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  Collection<UUID> getServicesAsUniqueId();

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  Collection<ServiceInfoSnapshot> getCloudServices();

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  Collection<ServiceInfoSnapshot> getStartedCloudServices();

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  Collection<ServiceInfoSnapshot> getCloudServices(@NotNull String taskName);


  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param environment the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  Collection<ServiceInfoSnapshot> getCloudServices(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the case-insensitive name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group);

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


  /**
   * Gets a list with the uniqueIds of all services in the cloud
   *
   * @return a list containing the uniqueIds of every service in the whole cloud
   */
  @NotNull
  ITask<Collection<UUID>> getServicesAsUniqueIdAsync();

  /**
   * Gets a list with the infos of all services in the cloud
   *
   * @return a list containing the infos of every service in the whole cloud
   */
  @NotNull
  ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync();

  /**
   * Gets a list with the infos of all started services in the cloud
   *
   * @return a list containing the infos of every started service in the whole cloud
   */
  @NotNull
  ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync();

  /**
   * Gets a list with the infos of all services in the cloud that are from the given task
   *
   * @param taskName the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given task in the whole cloud
   */
  @NotNull
  ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull String taskName);

  /**
   * Gets a list with the infos of all services in the cloud that have the given environment
   *
   * @param environment the environment every service in the list should have
   * @return a list containing the infos of every service with the given environment in the whole cloud
   */
  @NotNull
  ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a list with the infos of all services in the cloud that have the given group
   *
   * @param group the name of the task every service in the list should have
   * @return a list containing the infos of every service with the given group in the whole cloud
   */
  @NotNull
  ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(@NotNull String group);

  /**
   * Gets the amount of services in the cloud
   *
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  ITask<Integer> getServicesCountAsync();

  /**
   * Gets the amount of services by the given group in the cloud
   *
   * @param group the group every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  ITask<Integer> getServicesCountByGroupAsync(@NotNull String group);

  /**
   * Gets the amount of services by the given task in the cloud
   *
   * @param taskName the task every service counting should have
   * @return an integer for the amount of services in the whole cloud
   */
  @NotNull
  ITask<Integer> getServicesCountByTaskAsync(@NotNull String taskName);

  /**
   * Gets the info of a cloud service by its name
   *
   * @param name the name of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @NotNull
  ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(@NotNull String name);

  /**
   * Gets the info of a cloud service by its uniqueId
   *
   * @param uniqueId the uniqueId of the service
   * @return the info of the service or {@code null} if the service doesn't exist
   */
  @NotNull
  ITask<ServiceInfoSnapshot> getCloudServiceAsync(@NotNull UUID uniqueId);

}
