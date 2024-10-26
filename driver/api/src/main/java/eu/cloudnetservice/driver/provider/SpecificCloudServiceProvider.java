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

import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An instance of this class represents a manageable service in the cluster which is stateless. This is the main
 * difference to a service snapshot. While the snapshot holds the service information in a specific moment, the provider
 * of the service will always be ready to execute actions on a service (unless the provider is no longer valid).
 * <p>
 * The provider for a service could possibly target other services which were created with the same name or unique id
 * this provider was created for. Therefore, acquiring a new provider for a service when you're unsure if the old
 * service still exists is recommended.
 *
 * @since 4.0
 */
public interface SpecificCloudServiceProvider {

  /**
   * Get the last reported service info snapshot of the service. This snapshot is updated on an event basis, therefore
   * this method will not always return a snapshot which is brand-new. More specifically, for example the bridge will
   * cause a service info update each time a player joins/leaves the current server. This leads to unexpected
   * information when for example querying the process snapshot or the creation time, as these data can be very old.
   * <p>
   * This information is null when either the underlying service of this provider was unregistered from the system or
   * when creating the provider the service didn't yet exist.
   * <p>
   * If you need an update-to-date version of the snapshot, use {@link #forceUpdateServiceInfo()} instead.
   *
   * @return the last reported information snapshot of the service, can be null as described above.
   */
  @Nullable
  ServiceInfoSnapshot serviceInfo();

  /**
   * Get if this provider is still valid. A provider which is valid
   * <ol>
   *   <li>targets a service which still exists.
   *   <li>targets a service which is not marked as deleted.
   * </ol>
   *
   * @return true if this provider is still valid, false otherwise.
   */
  boolean valid();

  /**
   * Forces the service to update its service info and always returns a newly created snapshot of the service (other
   * than returning the last reported snapshot what {@link #serviceInfo()} does). This method returns null when the
   * underlying service
   * <ol>
   *   <li>doesn't exist anymore.
   *   <li>is not started and therefore not connected to a node.
   * </ol>
   *
   * @return a newly created service snapshot, can be null as described above.
   */
  @Nullable
  ServiceInfoSnapshot forceUpdateServiceInfo();

  /**
   * Adds the given service template to the inclusion queue. This does not mean that the given template will be copied
   * directly onto the service. The template will be copied when
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the templates get included via the associated method in this provider.
   * </ol>
   *
   * @param serviceTemplate the service template to enqueue.
   * @throws NullPointerException if the given service template is null.
   */
  void addServiceTemplate(@NonNull ServiceTemplate serviceTemplate);

  /**
   * Adds the given service remote inclusion to the queue. This does not mean that the inclusion gets copied directly
   * onto the service. The inclusion will be included when:
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the inclusions get included via the associated methods in this provider.
   * </ol>
   *
   * @param serviceRemoteInclusion the inclusion to enqueue.
   * @throws NullPointerException if the given inclusion is null.
   */
  void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds the given service deployment to the queue. This does not mean that the deployment gets executed directly. It
   * will be executed when:
   * <ol>
   *   <li>the service stops, for example when deleting it.
   *   <li>the deployments get executed via the associated methods in this provider.
   * </ol>
   *
   * @param serviceDeployment the deployment to enqueue.
   * @throws NullPointerException if the given deployment is null.
   */
  void addServiceDeployment(@NonNull ServiceDeployment serviceDeployment);

  /**
   * Get all log messages which are currently cached on the node this service is running on. Modifications to the
   * returned queue might be possible. The size of the returned collection is always not bigger than configured in the
   * node configuration the associated service is running on.
   * <p>
   * This method never return null but can return an empty queue if the underlying service does not exist.
   *
   * @return all cached log messages of the service on the node the service is running on.
   */
  @NonNull
  Queue<String> cachedLogMessages();

  /**
   * Enables or disabled the screen event handling. When the log events get enabled an event will be called on the given
   * sender of the request holding information about the log line. The provided channel represents the event channel to
   * which the listener need to listen in order to receive the events, set this to {@code *} to call all event
   * listeners.
   *
   * @param channelMessageSender the sender who should receive the log events.
   * @param channel              the event channel to call the log entry event in.
   * @return true if the log events were enabled for the sender, false if they got disabled.
   * @throws NullPointerException if either the given message sender or channel is null.
   */
  boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel);

  /**
   * Sets the service lifecycle to stopped and executes the appropriate actions to change to the stopped state.
   */
  default void stop() {
    this.updateLifecycle(ServiceLifeCycle.STOPPED);
  }

  /**
   * Sets the service lifecycle to started and executes the appropriate actions to change to the started state.
   */
  default void start() {
    this.updateLifecycle(ServiceLifeCycle.RUNNING);
  }

  /**
   * Sets the service lifecycle to deleted and executes the appropriate actions to change to the deleted state.
   */
  default void delete() {
    this.updateLifecycle(ServiceLifeCycle.DELETED);
  }

  /**
   * Stops this service and then tries to start it again. Note that this method will stop and delete the service, but
   * not start the service again when auto delete on stop is active for the service.
   */
  void restart();

  /**
   * Requests a change of the service lifecycle to the given one. This method has no effect if to the given lifecycle
   * cannot be switched from the current lifecycle of the service.
   *
   * @param lifeCycle the service lifecycle to switch to.
   * @throws NullPointerException if the given lifecycle is null.
   */
  void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle);

  /**
   * Stops the service if it is currently running marks it as deleted. Other than the delete method, in this case all
   * files associated with the service will get deleted permanently even if the service is static. If you just want to
   * stop and delete all files of the service when it is non-static use {@link #delete()} instead.
   * <p>
   * Deployments added to the service will get executed before the files get deleted.
   */
  void deleteFiles();

  /**
   * Executes the given command on the service if it is running. The given command line will be sent to stdin directly.
   *
   * @param command the command line to execute.
   * @throws NullPointerException if the given command line is null.
   */
  void runCommand(@NonNull String command);

  /**
   * Gets the templates that actually are installed on the service. If a template is present in the configuration
   * {@link eu.cloudnetservice.driver.service.ServiceConfiguration} but wasn't pulled onto the service it won't appear
   * in this collection.
   *
   * @return all installed templates of the service.
   */
  @NonNull
  Collection<ServiceTemplate> installedTemplates();

  /**
   * Gets the inclusions that actually are installed on the service. If an inclusion is present in the configuration
   * {@link eu.cloudnetservice.driver.service.ServiceConfiguration} but wasn't pulled onto the service it won't appear
   * in this collection.
   *
   * @return all installed inclusions of the service.
   */
  @NonNull
  Collection<ServiceRemoteInclusion> installedInclusions();

  /**
   * Gets the deployments that were actually executed for this service. If a deployment is present in the configuration
   * {@link eu.cloudnetservice.driver.service.ServiceConfiguration} but wasn't executed until now it won't appear in
   * this collection.
   *
   * @return all executed deployments of the service.
   */
  @NonNull
  Collection<ServiceDeployment> installedDeployments();

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   * <p>
   * This method forces the inclusion of all templates, see {@link #includeWaitingServiceTemplates(boolean)} for more
   * information.
   */
  void includeWaitingServiceTemplates();

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   * <p>
   * This method only copies all templates to a service if the force option is set to {@code true}. If disabled the
   * normal checks are made before trying to copy a template (for example if a template should be copied to a static
   * service).
   *
   * @param force if the inclusions of the templates should be forced.
   */
  void includeWaitingServiceTemplates(boolean force);

  /**
   * Downloads and copies all waiting inclusions onto the service without further checks. Note that this can lead to
   * errors if you try to override locked files or files which are in use (for example the application jar file).
   */
  void includeWaitingServiceInclusions();

  /**
   * Executes all deployments which were previously added to the associated service and optionally removes them once
   * they were executed successfully.
   *
   * @param removeDeployments if the deployments should get removed after executing them.
   */
  void deployResources(boolean removeDeployments);

  /**
   * Executes all deployments which were previously added to the associated service and removes them once they were
   * executed. This method call is identical to {@code provider.deployResources(true)}.
   */
  default void removeAndExecuteDeployments() {
    this.deployResources(true);
  }

  /**
   * Updates the properties of the current service info to include all properties set in the given document. All
   * existing properties will be overridden when using this method. If the associated service is currently running, a
   * request will be sent to update the current service information to use the given properties. Plugins on the service
   * can decide to ignore values set in the given document and override them.
   * <p>
   * Example use in an async context might look like this:
   * <pre>
   * {@code
   * public void updateCustomProperties(@NonNull SpecificCloudServiceProvider provider) {
   *   provider.serviceInfoAsync().thenAcceptAsync(info -> {
   *     var properties = info.properties();
   *     properties.append("hello", "world");
   *     properties.append("world", 123);
   *     provider.updateProperties(properties);
   *   });
   * }
   * }
   * </pre>
   * The difference to using the {@link #updatePropertiesAsync(Document)} method is that the update operation is
   * executed in the same async context as the service info retrieval, rather than moving the update (without a need)
   * into a separate thread.
   * <p>
   * Update request of the properties might not reflect instantly into new service snapshots produced by the service.
   *
   * @param properties the new properties of the service to cleanly set.
   * @throws NullPointerException if the given properties document is null.
   */
  void updateProperties(@NonNull Document properties);

  /**
   * Get the last reported service info snapshot of the service. This snapshot is updated on an event basis, therefore
   * this method will not always return a snapshot which is brand-new. More specifically, for example the bridge will
   * cause a service info update each time a player joins/leaves the current server. This leads to unexpected
   * information when for example querying the process snapshot or the creation time, as these data can be very old.
   * <p>
   * This information is null when either the underlying service of this provider was unregistered from the system or
   * when creating the provider the service didn't yet exist.
   * <p>
   * If you need an update-to-date version of the snapshot, use {@link #forceUpdateServiceInfo()} instead.
   *
   * @return a task completed with the last reported snapshot of the service, can be null as described above.
   */
  @NonNull
  CompletableFuture<ServiceInfoSnapshot> serviceInfoAsync();

  /**
   * Get if this provider is still valid. A provider which is valid
   * <ol>
   *   <li>targets a service which still exists.
   *   <li>targets a service which is not marked as deleted.
   * </ol>
   *
   * @return a task completed with true if this provider is still valid, false otherwise.
   */
  @NonNull
  CompletableFuture<Boolean> validAsync();

  /**
   * Forces the service to update its service info and always returns a newly created snapshot of the service (other
   * than returning the last reported snapshot what {@link #serviceInfo()} does). This method returns null when the
   * underlying service
   * <ol>
   *   <li>doesn't exist anymore.
   *   <li>is not started and therefore not connected to a node.
   * </ol>
   *
   * @return a task completed with a newly created service snapshot, can be null as described above.
   */
  @NonNull
  CompletableFuture<ServiceInfoSnapshot> forceUpdateServiceInfoAsync();

  /**
   * Adds the given service template to the inclusion queue. This does not mean that the given template will be copied
   * directly onto the service. The template will be copied when
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the templates get included via the associated method in this provider.
   * </ol>
   *
   * @param serviceTemplate the service template to enqueue.
   * @return a task completed when the given service template was enqueued.
   * @throws NullPointerException if the given service template is null.
   */
  @NonNull
  CompletableFuture<Void> addServiceTemplateAsync(@NonNull ServiceTemplate serviceTemplate);

  /**
   * Adds the given service remote inclusion to the queue. This does not mean that the inclusion gets copied directly
   * onto the service. The inclusion will be included when:
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the inclusions get included via the associated methods in this provider.
   * </ol>
   *
   * @param serviceRemoteInclusion the inclusion to enqueue.
   * @return a task completed when the given service remote inclusion was enqueued.
   * @throws NullPointerException if the given inclusion is null.
   */
  @NonNull
  CompletableFuture<Void> addServiceRemoteInclusionAsync(@NonNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds the given service deployment to the queue. This does not mean that the deployment gets executed directly. It
   * wil be executed when:
   * <ol>
   *   <li>the service stops, for example when deleting it.
   *   <li>the deployments get executed via the associated methods in this provider.
   * </ol>
   *
   * @param serviceDeployment the deployment to enqueue.
   * @return a task completed when the given service deployment was enqueued.
   * @throws NullPointerException if the given deployment is null.
   */
  @NonNull
  CompletableFuture<Void> addServiceDeploymentAsync(@NonNull ServiceDeployment serviceDeployment);

  /**
   * Get all log messages which are currently cached on the node this service is running on. Modifications to the
   * returned queue might be possible. The size of the returned collection is always not bigger than configured in the
   * node configuration the associated service is running on.
   * <p>
   * This method never return null but can return an empty queue if the underlying service does not exist.
   *
   * @return a task completed with all cached service log messages on the node the service is running on.
   */
  @NonNull
  CompletableFuture<Queue<String>> cachedLogMessagesAsync();

  /**
   * Enables or disabled the screen event handling. When the log events get enabled an event will be called on the given
   * sender of the request holding information about the log line. The provided channel represents the event channel to
   * which the listener need to listen in order to receive the events, set this to {@code *} to call all event
   * listeners.
   *
   * @param sender  the sender who should receive the log events.
   * @param channel the event channel to call the log entry event in.
   * @return a task completed with true if the log events were enabled for the sender, false if they got disabled.
   * @throws NullPointerException if either the given message sender or channel is null.
   */
  @NonNull
  CompletableFuture<Boolean> toggleScreenEventsAsync(@NonNull ChannelMessageSender sender, @NonNull String channel);

  /**
   * Sets the service lifecycle to stopped and executes the appropriate actions to change to the stopped state.
   *
   * @return a task completed when the service lifecycle changed to stopped.
   */
  default @NonNull CompletableFuture<Void> stopAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.STOPPED);
  }

  /**
   * Sets the service lifecycle to started and executes the appropriate actions to change to the started state.
   *
   * @return a task completed when the service lifecycle changed to running.
   */
  default @NonNull CompletableFuture<Void> startAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.RUNNING);
  }

  /**
   * Sets the service lifecycle to deleted and executes the appropriate actions to change to the deleted state.
   *
   * @return a task completed when the service state changed to deleted.
   */
  default @NonNull CompletableFuture<Void> deleteAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.DELETED);
  }

  /**
   * Stops this service and then tries to start it again. Note that this method will stop and delete the service, but
   * not start the service again when auto delete on stop is active for the service.
   *
   * @return a task completed when the service was restarted.
   */
  @NonNull
  CompletableFuture<Void> restartAsync();

  /**
   * Requests a change of the service lifecycle to the given one. This method has no effect if to the given lifecycle
   * cannot be switched from the current lifecycle of the service.
   *
   * @param lifeCycle the service lifecycle to switch to.
   * @return a task completed when the lifecycle change was tried.
   * @throws NullPointerException if the given lifecycle is null.
   */
  @NonNull
  CompletableFuture<Void> updateLifecycleAsync(@NonNull ServiceLifeCycle lifeCycle);

  /**
   * Stops the service if it is currently running marks it as deleted. Other than the delete method, in this case all
   * files associated with the service will get deleted permanently even if the service is static. If you just want to
   * stop and delete all files of the service when it is non-static use {@link #delete()} instead.
   * <p>
   * Deployments added to the service will get executed before the files get deleted.
   *
   * @return a task completed when the service files were deleted.
   */
  @NonNull
  CompletableFuture<Void> deleteFilesAsync();

  /**
   * Executes the given command on the service if it is running. The given command line will be sent to stdin directly.
   *
   * @param command the command line to execute.
   * @return a task completed when the command was send to the service.
   * @throws NullPointerException if the given command line is null.
   */
  @NonNull
  CompletableFuture<Void> runCommandAsync(@NonNull String command);

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   * <p>
   * This method forces the inclusion of all templates, see {@link #includeWaitingServiceTemplates(boolean)} for more
   * information.
   *
   * @return a task completed when the waiting service templates were included.
   */
  @NonNull
  CompletableFuture<Void> includeWaitingServiceTemplatesAsync();

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   * <p>
   * This method only copies all templates to a service if the force option is set to {@code true}. If disabled the
   * normal checks are made before trying to copy a template (for example if a template should be copied to a static
   * service).
   *
   * @param force if the inclusions of the templates should be forced.
   * @return a task completed when the waiting service templates were included.
   */
  @NonNull
  CompletableFuture<Void> includeWaitingServiceTemplatesAsync(boolean force);

  /**
   * Downloads and copies all waiting inclusions onto the service without further checks. Note that this can lead to
   * errors if you try to override locked files or files which are in use (for example the application jar file).
   *
   * @return a task completed when the waiting service inclusions were included.
   */
  @NonNull
  CompletableFuture<Void> includeWaitingServiceInclusionsAsync();

  /**
   * Executes all deployments which were previously added to the associated service and optionally removes them once
   * they were executed successfully.
   *
   * @param removeDeployments if the deployments should get removed after executing them.
   * @return a task completed when all waiting service deployments were executed.
   */
  @NonNull
  CompletableFuture<Void> deployResourcesAsync(boolean removeDeployments);

  /**
   * Executes all deployments which were previously added to the associated service and removes them once they were
   * executed. This method call is identical to {@code provider.deployResourcesAsync(true)}.
   *
   * @return a task completed when all waiting service deployments were executed.
   */
  @NonNull
  CompletableFuture<Void> removeAndExecuteDeploymentsAsync();

  /**
   * Updates the properties of the current service info to include all properties set in the given document. All
   * existing properties will be overridden when using this method. If the associated service is currently running, a
   * request will be sent to update the current service information to use the given properties. Plugins on the service
   * can decide to ignore values set in the given document and override them.
   * <p>
   * Update request of the properties might not reflect instantly into new service snapshots produced by the service.
   *
   * @param properties the new properties of the service to cleanly set.
   * @return a task completed when the update request was received and processed by the node the service runs on.
   * @throws NullPointerException if the given properties document is null.
   */
  @NonNull
  CompletableFuture<Void> updatePropertiesAsync(@NonNull Document properties);
}
