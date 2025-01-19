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

import eu.cloudnetservice.driver.network.rpc.annotation.RPCChained;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The main api point for accessing and managing services running in the cluster. This provider contains some common
 * methods which are widely used by developers which are working with CloudNet. Some methods are still required to be
 * implemented by you, because they are exceeding the normal range of methods.
 * <p>
 * There are two main ways to access a service running in the cluster. You can either get a snapshot of them, which
 * represents the state of the service at a given time, or a provider which then allows you to execute further actions
 * which will directly impact the service. No methods in here are available to create services, for this purpose use
 * {@link CloudServiceFactory} instead.
 *
 * @since 4.0
 */
public interface CloudServiceProvider {

  /**
   * Gets a provider for the specific service with the given unique id. No check is made if the service this provider
   * was created for actually exists. If a provider gets created for a not-existing service, then calling any method
   * will result in a dummy return value and will execute no action on any service.
   * <p>
   * Note: creating a provider with a unique id of a service which will be created in the future might work but is not
   * required to work. Getting a clean instance after knowing that the service actually exists is recommended over
   * pre-getting a provider.
   * <p>
   * There is no need to update a provider when obtained once. An update of the provider should be done however, if a
   * new service with the same unique id was created (after the current target stopped) as that will most likely not
   * change the target of the provider to the new service and tries to execute requested actions on the old,
   * unregistered service.
   *
   * @param serviceUniqueId the unique id of the service to get the provider for.
   * @return an operational or no-op provider for a service, depending on whether the requested service exists.
   * @throws NullPointerException if the given service unique id is null.
   */
  @NonNull
  @RPCChained
  SpecificCloudServiceProvider serviceProvider(@NonNull UUID serviceUniqueId);

  /**
   * Gets a provider for the specific service with the given name. No check is made if the service this provider was
   * created for actually exists. If a provider gets created for a not-existing service, then calling any method will
   * result in a dummy return value and will execute no action on any service.
   * <p>
   * Note: creating a provider with a name of a service which will be created in the future might work but is not
   * required to work. Getting a clean instance after knowing that the service actually exists is recommended over
   * pre-getting a provider.
   * <p>
   * There is no need to update a provider when obtained once. An update of the provider should be done however, if a
   * new service with the same name was created (after the current target stopped) as that will most likely not change
   * the target of the provider to the new service and tries to execute requested actions on the old, unregistered
   * service.
   *
   * @param serviceName the name of the service to get the provider for.
   * @return an operational or no-op provider for a service, depending on whether the requested service exists.
   * @throws NullPointerException if the service name is null.
   */
  @NonNull
  @RPCChained
  SpecificCloudServiceProvider serviceProviderByName(@NonNull String serviceName);

  /**
   * Gets all services which are currently registered in the cluster. Modifications to the returned collections are not
   * possible nor will they have any effect.
   *
   * @return all services which are registered in the cluster.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceInfoSnapshot> services();

  /**
   * Gets all services which are currently registered and running in the cluster. Modifications to the returned
   * collections are not possible nor will they have any effect.
   *
   * @return all services which are registered and running in the cluster.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceInfoSnapshot> runningServices();

  /**
   * Gets all services which are currently registered in the cluster and belong to the given task. Modifications to the
   * returned collections are not possible nor will they have any effect.
   *
   * @param taskName the case-sensitive name of the task to get the services of.
   * @return all services which are currently registered in the cluster and belong to the given task.
   * @throws NullPointerException if the given task name is null.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceInfoSnapshot> servicesByTask(@NonNull String taskName);

  /**
   * Gets all services which are currently registered in the cluster and belong to the given environment. Modifications
   * to the returned collections are not possible nor will they have any effect.
   *
   * @param environment the case-sensitive name of the environment to get the services of.
   * @return all services which are currently registered in the cluster and belong to the given environment.
   * @throws NullPointerException if the given environment is null.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceInfoSnapshot> servicesByEnvironment(@NonNull String environment);

  /**
   * Gets all services which are currently registered in the cluster and belong to the given group. Modifications to the
   * returned collections are not possible nor will they have any effect.
   *
   * @param group the case-sensitive name of the group to get the services of.
   * @return all services which are currently registered in the cluster and belong to the given group.
   * @throws NullPointerException if the given group name is null.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceInfoSnapshot> servicesByGroup(@NonNull String group);

  /**
   * Gets the amount of services which are currently registered within the cluster.
   *
   * @return the amount of services which are currently registered within the cluster.
   */
  int serviceCount();

  /**
   * Get the amount of services which are currently registered within the cluster and belong to the given group.
   *
   * @param group the name of the group the services to count must be in.
   * @return the amount of services which are currently registered within the cluster and in the given group.
   * @throws NullPointerException if the given group name is null.
   */
  int serviceCountByGroup(@NonNull String group);

  /**
   * Get the amount of services which are currently registered within the cluster and belong to the given task.
   *
   * @param taskName the name of the task the services to count must belong.
   * @return the amount of services which are currently registered within the cluster and belong to the given task.
   * @throws NullPointerException if the given task name is null.
   */
  int serviceCountByTask(@NonNull String taskName);

  /**
   * Gets the current snapshot of the service with the given name. This method returns null if no service with the given
   * name is currently registered within the cluster.
   * <p>
   * This method does not update the service info before returning it, use the force update methods from the specific
   * service provider if you need an up-to-date version of a service snapshot.
   *
   * @param name the name of the service to get the snapshot of.
   * @return the current snapshot of the service with the given name or null if the service is not registered.
   * @throws NullPointerException if the given service name is null.
   */
  @Nullable ServiceInfoSnapshot serviceByName(@NonNull String name);

  /**
   * Gets the current snapshot of the service with the given unique id. This method returns null if no service with the
   * given unique id is currently registered within the cluster.
   * <p>
   * This method does not update the service info before returning it, use the force update methods from the specific
   * service provider if you need an up-to-date version of a service snapshot.
   *
   * @param uniqueId the unique id of the service to get the snapshot of.
   * @return the current snapshot of the service with the given unique id or null if the service is not registered.
   * @throws NullPointerException if the given service unique id is null.
   */
  @Nullable ServiceInfoSnapshot service(@NonNull UUID uniqueId);

  /**
   * Gets all services which are currently registered in the cluster. Modifications to the returned collections are not
   * possible nor will they have any effect.
   *
   * @return a task completed with all services which are registered in the cluster.
   */
  @NonNull
  CompletableFuture<Collection<ServiceInfoSnapshot>> servicesAsync();

  /**
   * Gets all services which are currently registered and running in the cluster. Modifications to the returned
   * collections are not possible nor will they have any effect.
   *
   * @return a task completed with all services which are registered and running in the cluster.
   */
  @NonNull
  CompletableFuture<Collection<ServiceInfoSnapshot>> runningServicesAsync();

  /**
   * Gets all services which are currently registered in the cluster and belong to the given task. Modifications to the
   * returned collections are not possible nor will they have any effect.
   *
   * @param taskName the case-sensitive name of the task to get the services of.
   * @return a task completed with all services currently registered in the cluster and belonging to the given task.
   * @throws NullPointerException if the given task name is null.
   */
  @NonNull
  CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByTaskAsync(@NonNull String taskName);

  /**
   * Gets all services which are currently registered in the cluster and belong to the given environment. Modifications
   * to the returned collections are not possible nor will they have any effect.
   *
   * @param environment the case-sensitive name of the environment to get the services of.
   * @return a task completed with all services currently registered in the cluster, belonging to the given environment.
   * @throws NullPointerException if the given environment is null.
   */
  @NonNull
  CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByEnvironmentAsync(@NonNull String environment);

  /**
   * Gets all services which are currently registered in the cluster and belong to the given group. Modifications to the
   * returned collections are not possible nor will they have any effect.
   *
   * @param group the case-sensitive name of the group to get the services of.
   * @return a task completed with all services currently registered in the cluster and belonging to the given group.
   * @throws NullPointerException if the given group name is null.
   */
  @NonNull
  CompletableFuture<Collection<ServiceInfoSnapshot>> servicesByGroupAsync(@NonNull String group);

  /**
   * Gets the amount of services which are currently registered within the cluster.
   *
   * @return a task completed with the amount of services which are currently registered within the cluster.
   */
  @NonNull
  CompletableFuture<Integer> serviceCountAsync();

  /**
   * Get the amount of services which are currently registered within the cluster and belong to the given group.
   *
   * @param group the name of the group the services to count must be in.
   * @return a task completed with the amount of services currently registered, belonging the given group.
   * @throws NullPointerException if the given group name is null.
   */
  @NonNull
  CompletableFuture<Integer> serviceCountByGroupAsync(@NonNull String group);

  /**
   * Get the amount of services which are currently registered within the cluster and belong to the given task.
   *
   * @param taskName the name of the task the services to count must belong.
   * @return a task completed with the amount of services currently registered, belonging the given task.
   * @throws NullPointerException if the given task name is null.
   */
  @NonNull
  CompletableFuture<Integer> serviceCountByTaskAsync(@NonNull String taskName);

  /**
   * Gets the current snapshot of the service with the given name. This method returns null if no service with the given
   * name is currently registered within the cluster.
   * <p>
   * This method does not update the service info before returning it, use the force update methods from the specific
   * service provider if you need an up-to-date version of a service snapshot.
   *
   * @param name the name of the service to get the snapshot of.
   * @return a task completed with the current snapshot of the service or null if the service is not registered.
   * @throws NullPointerException if the given service name is null.
   */
  @NonNull
  CompletableFuture<ServiceInfoSnapshot> serviceByNameAsync(@NonNull String name);

  /**
   * Gets the current snapshot of the service with the given unique id. This method returns null if no service with the
   * given unique id is currently registered within the cluster.
   * <p>
   * This method does not update the service info before returning it, use the force update methods from the specific
   * service provider if you need an up-to-date version of a service snapshot.
   *
   * @param uniqueId the unique id of the service to get the snapshot of.
   * @return a task completed with the current snapshot of the service or null if the service is not registered.
   * @throws NullPointerException if the given service unique id is null.
   */
  @NonNull
  CompletableFuture<ServiceInfoSnapshot> serviceAsync(@NonNull UUID uniqueId);
}
