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
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides access to a specific service in the cluster.
 */
@RPCValidation
public interface SpecificCloudServiceProvider {

  /**
   * Gets the info of the service this provider is for.
   *
   * @return the info or {@code null}, if the service doesn't exist
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given when creating this provider
   */
  @Nullable ServiceInfoSnapshot serviceInfo();

  /**
   * Checks whether the service info on this provider is available or not.
   *
   * @return {@code true} if it is or {@code false} if not
   */
  boolean valid();

  /**
   * Forces this service to update its {@link ServiceInfoSnapshot}, instead of {@link #serviceInfo()}, this method
   * always returns the current snapshot with the current e.g. CPU and memory usage.
   *
   * @return the current info or {@code null} if the service is not connected
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  @Nullable ServiceInfoSnapshot forceUpdateServiceInfo();

  /**
   * Adds a service template to this service. This template won't be copied directly after adding it but when the
   * service is prepared.
   *
   * @param serviceTemplate the template to be added to the list of templates of this service
   */
  void addServiceTemplate(@NonNull ServiceTemplate serviceTemplate);

  /**
   * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when
   * the service is prepared.
   *
   * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
   */
  void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds a deployment to this service, which will be used when {@link #removeAndExecuteDeployments()} or {@link
   * #deployResources(boolean)} is called.
   *
   * @param serviceDeployment the deployment to be added to the list of deployments of this service
   */
  void addServiceDeployment(@NonNull ServiceDeployment serviceDeployment);

  /**
   * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in
   * the nodes config.json.
   *
   * @return a queue with the cached messages of this services console
   */
  Queue<String> cachedLogMessages();

  boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel);

  /**
   * Stops this service by executing the "stop" and "end" commands in its console if it is running.
   */
  default void stop() {
    this.updateLifecycle(ServiceLifeCycle.STOPPED);
  }

  /**
   * Starts this service if it is prepared or stopped.
   */
  default void start() {
    this.updateLifecycle(ServiceLifeCycle.RUNNING);
  }

  /**
   * Deletes this service if it is not deleted yet.
   */
  default void delete() {
    this.updateLifecycle(ServiceLifeCycle.DELETED);
  }

  /**
   * Deletes this service and deletes the files that belong to the service. This method differs from {@link #delete()}
   * in the point that this method deletes files of static services too.
   */
  void deleteFiles();

  /**
   * Sets the life cycle of this service and starts, prepares, stops or deletes this service.
   *
   * @param lifeCycle the lifeCycle to be set
   */
  void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle);

  /**
   * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
   */
  void restart();

  /**
   * Executes the given command in the console of this service if it is running
   *
   * @param command the command to be executed
   */
  void runCommand(@NonNull String command);

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
  default void removeAndExecuteDeployments() {
    this.deployResources(true);
  }

  /**
   * Gets the info of the service this provider is for
   *
   * @return the info or {@code null}, if the service doesn't exist
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  default @NonNull Task<ServiceInfoSnapshot> serviceInfoAsync() {
    return CompletableTask.supply(this::serviceInfo);
  }

  /**
   * Checks whether the service info on this provider is available or not.
   *
   * @return {@code true} if it is or {@code false} if not
   */
  default @NonNull Task<Boolean> validAsync() {
    return CompletableTask.supply(this::valid);
  }

  /**
   * Forces this service to update its {@link ServiceInfoSnapshot}, instead of {@link #serviceInfo()}, this method
   * always returns the current snapshot with the current e.g. CPU and memory usage.
   *
   * @return the current info or {@code null} if the service is not connected
   * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
   */
  default @NonNull Task<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
    return CompletableTask.supply(this::forceUpdateServiceInfo);
  }

  /**
   * Adds a service template to this service. This template won't be copied directly after adding it but when the
   * service is prepared
   *
   * @param serviceTemplate the template to be added to the list of templates of this service
   */
  default @NonNull Task<Void> addServiceTemplateAsync(@NonNull ServiceTemplate serviceTemplate) {
    return CompletableTask.supply(() -> this.addServiceTemplate(serviceTemplate));
  }

  /**
   * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when
   * the service is prepared
   *
   * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
   */
  default @NonNull Task<Void> addServiceRemoteInclusionAsync(@NonNull ServiceRemoteInclusion serviceRemoteInclusion) {
    return CompletableTask.supply(() -> this.addServiceRemoteInclusion(serviceRemoteInclusion));
  }

  /**
   * Adds a deployment to this service, which will be used when {@link #removeAndExecuteDeployments()} or {@link
   * #deployResources(boolean)} is called
   *
   * @param serviceDeployment the deployment to be added to the list of deployments of this service
   */
  default @NonNull Task<Void> addServiceDeploymentAsync(@NonNull ServiceDeployment serviceDeployment) {
    return CompletableTask.supply(() -> this.addServiceDeployment(serviceDeployment));
  }

  /**
   * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in
   * the nodes config.json.
   *
   * @return a queue with the cached messages of this services console
   */
  default @NonNull Task<Queue<String>> cachedLogMessagesAsync() {
    return CompletableTask.supply(this::cachedLogMessages);
  }

  /**
   * Stops this service by executing the "stop" and "end" commands in its console if it is running.
   */
  default @NonNull Task<Void> stopAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.STOPPED);
  }

  /**
   * Starts this service if it is prepared or stopped.
   */
  default @NonNull Task<Void> startAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.RUNNING);
  }

  /**
   * Deletes this service if it is not deleted yet.
   */
  default @NonNull Task<Void> deleteAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.DELETED);
  }

  /**
   * Sets the life cycle of this service and starts, prepares, stops or deletes this service
   *
   * @param lifeCycle the lifeCycle to be set
   */
  default @NonNull Task<Void> updateLifecycleAsync(@NonNull ServiceLifeCycle lifeCycle) {
    return CompletableTask.supply(() -> this.updateLifecycle(lifeCycle));
  }

  /**
   * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
   */
  default @NonNull Task<Void> restartAsync() {
    return CompletableTask.supply(this::restart);
  }

  /**
   * Executes the given command in the console of this service if it is running
   *
   * @param command the command to be executed
   */
  default @NonNull Task<Void> runCommandAsync(@NonNull String command) {
    return CompletableTask.supply(() -> this.runCommand(command));
  }

  /**
   * Copies all templates of this service into the directory where this service is located in
   *
   * @see #addServiceTemplate(ServiceTemplate)
   * @see #addServiceTemplateAsync(ServiceTemplate)
   */
  default @NonNull Task<Void> includeWaitingServiceTemplatesAsync() {
    return CompletableTask.supply(this::includeWaitingServiceTemplates);
  }

  /**
   * Copies all inclusions of this service into the directory where this service is located in
   *
   * @see #addServiceRemoteInclusion(ServiceRemoteInclusion)
   * @see #addServiceRemoteInclusionAsync(ServiceRemoteInclusion)
   */
  default @NonNull Task<Void> includeWaitingServiceInclusionsAsync() {
    return CompletableTask.supply(this::includeWaitingServiceInclusions);
  }

  /**
   * Writes all deployments to their defined templates of this service.
   *
   * @param removeDeployments whether the deployments should be removed after deploying or not
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  default @NonNull Task<Void> deployResourcesAsync(boolean removeDeployments) {
    return CompletableTask.supply(() -> this.deployResources(removeDeployments));
  }

  /**
   * Writes all deployments to their defined templates of this service and removes them after writing.
   *
   * @see #addServiceDeployment(ServiceDeployment)
   * @see #addServiceDeploymentAsync(ServiceDeployment)
   */
  default @NonNull Task<Void> executeAndRemoveDeploymentsAsync() {
    return this.deployResourcesAsync(true);
  }
}
