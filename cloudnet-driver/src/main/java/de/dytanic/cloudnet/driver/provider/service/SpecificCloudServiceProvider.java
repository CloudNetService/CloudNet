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
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides access to a specific service in the cluster.
 */
public interface SpecificCloudServiceProvider {

  /**
   * Gets the info of the service this provider is for.
   *
   * @return the info or {@code null}, if the service doesn't exist
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  @Nullable
  ServiceInfoSnapshot getServiceInfoSnapshot();

  /**
   * Checks whether the service info on this provider is available or not.
   *
   * @return {@code true} if it is or {@code false} if not
   */
  boolean isValid();

  /**
   * Forces this service to update its {@link ServiceInfoSnapshot}, instead of {@link #getServiceInfoSnapshot()}, this
   * method always returns the current snapshot with the current e.g. CPU and memory usage.
   *
   * @return the current info or {@code null} if the service is not connected
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  @Nullable
  ServiceInfoSnapshot forceUpdateServiceInfo();

  /**
   * Adds a service template to this service. This template won't be copied directly after adding it but when the
   * service is prepared.
   *
   * @param serviceTemplate the template to be added to the list of templates of this service
   */
  void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate);

  /**
   * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when
   * the service is prepared.
   *
   * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
   */
  void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds a deployment to this service, which will be used when {@link #deployResources()} or {@link
   * #deployResources(boolean)} is called.
   *
   * @param serviceDeployment the deployment to be added to the list of deployments of this service
   */
  void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment);

  /**
   * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in
   * the nodes config.json.
   *
   * @return a queue with the cached messages of this services console
   */
  Queue<String> getCachedLogMessages();

  /**
   * Stops this service by executing the "stop" and "end" commands in its console if it is running.
   */
  default void stop() {
    this.setCloudServiceLifeCycle(ServiceLifeCycle.STOPPED);
  }

  /**
   * Starts this service if it is prepared or stopped.
   */
  default void start() {
    this.setCloudServiceLifeCycle(ServiceLifeCycle.RUNNING);
  }

  /**
   * Deletes this service if it is not deleted yet. If this service is running, it will be stopped like {@link #kill()}
   * does.
   */
  default void delete() {
    this.setCloudServiceLifeCycle(ServiceLifeCycle.DELETED);
  }

  /**
   * Sets the life cycle of this service and starts, prepares, stops or deletes this service.
   *
   * @param lifeCycle the lifeCycle to be set
   */
  void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle);

  /**
   * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
   */
  void restart();

  /**
   * Tries to stop this service like {@link #stop()} but if the service is still running after 5 seconds, it is
   * destroyed forcibly
   */
  void kill();

  /**
   * Executes the given command in the console of this service if it is running
   *
   * @param command the command to be executed
   */
  void runCommand(@NotNull String command);

  /**
   * Copies all templates of this service into the directory where this service is located in
   *
   * @see #addServiceTemplate(ServiceTemplate)
   * @see #addServiceTemplateAsync(ServiceTemplate)
   */
  void includeWaitingServiceTemplates();

  /**
   * Copies all inclusions of this service into the directory where this service is located in
   *
   * @see #addServiceRemoteInclusion(ServiceRemoteInclusion)
   * @see #addServiceRemoteInclusionAsync(ServiceRemoteInclusion)
   */
  void includeWaitingServiceInclusions();

  /**
   * Writes all deployments to their defined templates of this service.
   *
   * @param removeDeployments whether the deployments should be removed after deploying or not
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  void deployResources(boolean removeDeployments);

  /**
   * Writes all deployments to their defined templates of this service and removes them after writing.
   *
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  default void deployResources() {
    this.deployResources(true);
  }

  /**
   * Gets the info of the service this provider is for
   *
   * @return the info or {@code null}, if the service doesn't exist
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  @NotNull
  ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync();

  /**
   * Checks whether the service info on this provider is available or not.
   *
   * @return {@code true} if it is or {@code false} if not
   */
  @NotNull
  ITask<Boolean> isValidAsync();

  /**
   * Forces this service to update its {@link ServiceInfoSnapshot}, instead of {@link #getServiceInfoSnapshot()}, this
   * method always returns the current snapshot with the current e.g. CPU and memory usage.
   *
   * @return the current info or {@code null} if the service is not connected
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  @NotNull
  ITask<ServiceInfoSnapshot> forceUpdateServiceInfoAsync();

  /**
   * Adds a service template to this service. This template won't be copied directly after adding it but when the
   * service is prepared
   *
   * @param serviceTemplate the template to be added to the list of templates of this service
   */
  @NotNull
  ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate);

  /**
   * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when
   * the service is prepared
   *
   * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
   */
  @NotNull
  ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds a deployment to this service, which will be used when {@link #deployResources()} or {@link
   * #deployResources(boolean)} is called
   *
   * @param serviceDeployment the deployment to be added to the list of deployments of this service
   */
  @NotNull
  ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment);

  /**
   * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in
   * the nodes config.json.
   *
   * @return a queue with the cached messages of this services console
   */
  @NotNull
  ITask<Queue<String>> getCachedLogMessagesAsync();

  /**
   * Stops this service by executing the "stop" and "end" commands in its console if it is running.
   */
  @NotNull
  default ITask<Void> stopAsync() {
    return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.STOPPED);
  }

  /**
   * Starts this service if it is prepared or stopped.
   */
  @NotNull
  default ITask<Void> startAsync() {
    return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.RUNNING);
  }

  /**
   * Deletes this service if it is not deleted yet. If this service is running, it will be stopped like {@link #kill()}
   * does.
   */
  @NotNull
  default ITask<Void> deleteAsync() {
    return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.DELETED);
  }

  /**
   * Sets the life cycle of this service and starts, prepares, stops or deletes this service
   *
   * @param lifeCycle the lifeCycle to be set
   */
  @NotNull
  ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle);

  /**
   * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
   */
  @NotNull
  ITask<Void> restartAsync();

  /**
   * Tries to stop this service like {@link #stop()} but if the service is still running after 5 seconds, it is
   * destroyed forcibly
   */
  @NotNull
  ITask<Void> killAsync();

  /**
   * Executes the given command in the console of this service if it is running
   *
   * @param command the command to be executed
   */
  @NotNull
  ITask<Void> runCommandAsync(@NotNull String command);

  /**
   * Copies all templates of this service into the directory where this service is located in
   *
   * @see #addServiceTemplate(ServiceTemplate)
   * @see #addServiceTemplateAsync(ServiceTemplate)
   */
  @NotNull
  ITask<Void> includeWaitingServiceTemplatesAsync();

  /**
   * Copies all inclusions of this service into the directory where this service is located in
   *
   * @see #addServiceRemoteInclusion(ServiceRemoteInclusion)
   * @see #addServiceRemoteInclusionAsync(ServiceRemoteInclusion)
   */
  @NotNull
  ITask<Void> includeWaitingServiceInclusionsAsync();

  /**
   * Writes all deployments to their defined templates of this service.
   *
   * @param removeDeployments whether the deployments should be removed after deploying or not
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  @NotNull
  ITask<Void> deployResourcesAsync(boolean removeDeployments);

  /**
   * Writes all deployments to their defined templates of this service and removes them after writing.
   *
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  @NotNull
  default ITask<Void> deployResourcesAsync() {
    return this.deployResourcesAsync(true);
  }

}
