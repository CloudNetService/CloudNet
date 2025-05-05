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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.base.Named;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A service id. This holds all the information needed to identify a service and its purpose within CloudNet. This class
 * is used to give each service a unique identity to work with.
 *
 * @since 4.0
 */
@EqualsAndHashCode
public class ServiceId implements Named {

  protected final String taskName;
  protected final String nameSplitter;
  protected final Set<String> allowedNodes;

  protected final UUID uniqueId;
  protected final int taskServiceId;
  protected final String nodeUniqueId;

  protected final String environmentName;
  protected final ServiceEnvironmentType environment;

  /**
   * Creates a new service identity instance.
   *
   * @param taskName        the name of the task this service is based on.
   * @param nameSplitter    the splitter of the service name, to put between the task name and task service id.
   * @param allowedNodes    the nodes which are allowed to start the service.
   * @param uniqueId        the unique id (version 4) of the service, there is never a duplicate within CloudNet.
   * @param taskServiceId   the numeric id of the service within the task.
   * @param nodeUniqueId    the unique id of the node which picked up the service, null if not yet elected.
   * @param environmentName the name of the environment type of the service.
   * @param environment     the resolved environment type, null if not yet resolved.
   * @throws NullPointerException if any given parameter is null, except for the resolved environment type and node.
   */
  protected ServiceId(
    @NonNull String taskName,
    @NonNull String nameSplitter,
    @NonNull Set<String> allowedNodes,
    @NonNull UUID uniqueId,
    int taskServiceId,
    @Nullable String nodeUniqueId,
    @NonNull String environmentName,
    @Nullable ServiceEnvironmentType environment
  ) {
    this.uniqueId = uniqueId;
    this.taskName = taskName;
    this.nameSplitter = nameSplitter;
    this.taskServiceId = taskServiceId;
    this.nodeUniqueId = nodeUniqueId;
    this.allowedNodes = allowedNodes;
    this.environmentName = environmentName;
    this.environment = environment;
  }

  /**
   * Constructs a new builder for a service id instance.
   *
   * @return a new service id builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder for a service id which has the same properties set as the given service id.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in a service id which is
   * equal but not the same as the given one.
   *
   * @param serviceId the service id to copy the properties of.
   * @return a new builder for a service id with the properties of the given one already set.
   * @throws NullPointerException if the given service id is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceId serviceId) {
    return builder()
      .uniqueId(serviceId.uniqueId())
      .taskName(serviceId.taskName())
      .nameSplitter(serviceId.nameSplitter())
      .environment(serviceId.environmentName())
      .environment(serviceId.environment())
      .taskServiceId(serviceId.taskServiceId())
      .nodeUniqueId(serviceId.nodeUniqueId())
      .allowedNodes(serviceId.allowedNodes());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.taskName + this.nameSplitter + this.taskServiceId;
  }

  /**
   * Get the unique id (version 4) of the service. There is never a service with the same unique id running
   * simultaneously within the CloudNet cluster.
   *
   * @return the unique id of the service.
   */
  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  /**
   * Get the unique id of the node which is responsible to pick up and manage the underlying service. This is null if
   * not specific node was chosen to start the service on, but will never be null after initially choosing one.
   *
   * @return the node unique id which is responsible to pick up and manage the underlying service.
   */
  public @UnknownNullability String nodeUniqueId() {
    return this.nodeUniqueId;
  }

  /**
   * Get the unique ids of all nodes which are allowed to pick up and manage the underlying service. This method returns
   * an empty collection if all nodes are allowed to pick up the service, but never null.
   * <p>
   * Note: this method might return an empty collection if a specific node was already chosen, you might need to check
   * that too.
   *
   * @return the unique ids of the nodes which are allowed to pick up and manage the underlying service.
   */
  @Unmodifiable
  public @NonNull Collection<String> allowedNodes() {
    return this.allowedNodes;
  }

  /**
   * The name of the task which was used to construct the service. There is no need for a permanent service task to
   * exist within the cluster with that name.
   *
   * @return the name of the task used when creating the service.
   */
  public @NonNull String taskName() {
    return this.taskName;
  }

  /**
   * Get the splitter to put between the name of the task and the numeric id of it when creating a full display name
   * variant of the underlying service.
   *
   * @return the name splitter of the underlying service.
   */
  public @NonNull String nameSplitter() {
    return this.nameSplitter;
  }

  /**
   * Get the name of the environment which should be used when starting the service. The returned environment must be
   * registered on the node which is picking up and managing the service in order to work correctly.
   *
   * @return the name of the environment to use for the service.
   */
  public @NonNull String environmentName() {
    return this.environmentName;
  }

  /**
   * Get the numeric id of the service within CloudNet. This id is bound to the task name, meaning that if a service
   * based on a task with 5 registered services (their ids are 1 - 5) gets created it will use the numeric id 6.
   * Services created based on other tasks are not included in the calculation.
   *
   * @return the numeric id of the service within the task based on which the service was created.
   */
  public int taskServiceId() {
    return this.taskServiceId;
  }

  /**
   * Get the resolved environment type object which was used to create the service. This method returns null when no
   * specific environment type was given and no node picked up the service yet and therefore the given environment name
   * was not resolved.
   * <p>
   * Note: this method returns never null after a service was successfully prepared at least once.
   *
   * @return the resolved service environment type, might be null if not yet resolved.
   */
  public @UnknownNullability ServiceEnvironmentType environment() {
    return this.environment;
  }

  /**
   * Constructs a stringified version of service id, containing the full display name and unique id of it.
   *
   * @return a stringified, non-cached version of this service id.
   */
  @Override
  public @NonNull String toString() {
    return this.name() + ':' + this.uniqueId;
  }

  /**
   * A builder for a service id.
   *
   * @since 4.0
   */
  public static class Builder {

    protected UUID uniqueId = UUID.randomUUID();

    protected String taskName;
    protected int taskServiceId = -1;
    protected String nodeUniqueId;
    protected String environmentName;
    protected String nameSplitter = "-";

    protected ServiceEnvironmentType environment;
    protected Set<String> allowedNodes = new HashSet<>();

    /**
     * Sets the unique id (version 4) of the service id. If the given unique id is already taken by any other service
     * running within the CloudNet cluster it gets replaced by a random unique id.
     *
     * @param uniqueId the unique id to use for the service.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given unique id is null.
     */
    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    /**
     * Sets the name of the task to use for the service id. The name must conform to the naming pattern defined in the
     * service info snapshot class.
     *
     * @param taskName the name of the task to use for the service id.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given task name is null.
     * @throws IllegalArgumentException if the given task does not match the task naming pattern.
     */
    public @NonNull Builder taskName(@NonNull String taskName) {
      Preconditions.checkArgument(ServiceTask.NAMING_PATTERN.matcher(taskName).matches(), "Invalid task name given");
      this.taskName = taskName;
      return this;
    }

    /**
     * Sets the numeric id of the service within the task. If the given unique id is already in use by any other service
     * running within the cluster, the given id will be counted up until it reached the numeric limit or a free id was
     * found.
     *
     * @param taskServiceId the base task id to use.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder taskServiceId(int taskServiceId) {
      this.taskServiceId = taskServiceId;
      return this;
    }

    /**
     * Sets the unique id of the node which is required to pick up and manage the service associated with the service
     * id. If the given node is not available within the cluster, services associated with the service id will not get
     * started at all.
     *
     * @param nodeUniqueId the unique id of the node which is required to pick up the node, null for no specific node.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder nodeUniqueId(@Nullable String nodeUniqueId) {
      this.nodeUniqueId = nodeUniqueId;
      return this;
    }

    /**
     * Sets the name splitter to use between the task name of the service and the numeric id of the service when
     * creating the display name of it. The given name splitter must conform to the naming pattern defined in the
     * service task class.
     *
     * @param nameSplitter the name splitter to use for services using the created id.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given name splitter is null.
     * @throws IllegalArgumentException if the given name splitter does not follow the naming pattern.
     */
    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      Preconditions.checkArgument(ServiceTask.NAMING_PATTERN.matcher(nameSplitter).matches(),
        "Invalid name splitter given");
      this.nameSplitter = nameSplitter;
      return this;
    }

    /**
     * Sets the name of the environment to use for services using the service id. The environment must be registered on
     * the node which is picking up the service.
     *
     * @param environmentName the name of the environment which should be used for services.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment name is null.
     */
    public @NonNull Builder environment(@NonNull String environmentName) {
      this.environmentName = environmentName;
      return this;
    }

    /**
     * Sets the environment which should be used for services using the service id. This method will set both, the name
     * and resolved environment type. If null is given this method <strong>will not</strong> reset anything previously
     * passed to it and silently ignore the setting.
     *
     * @param environment the environment to use for the service.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder environment(@Nullable ServiceEnvironmentType environment) {
      if (environment != null) {
        this.environment = environment;
        this.environmentName = environment.name();
      }
      return this;
    }

    /**
     * Sets the nodes which are allowed to pick up and manage services using the service id. If an empty collection is
     * given all nodes are allowed to pick up the service. If no node of the given ones is available to pick up a
     * service, creations will fail.
     * <p>
     * This method will override all previously added nodes which are allowed to start the service. The given collection
     * will be copied into the builder which means that further modification after the method call will not reflect into
     * the builder and vice-versa.
     *
     * @param allowedNodes the nodes which are allowed to pick up the service.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given node unique id collection is null.
     */
    public @NonNull Builder allowedNodes(@NonNull Collection<String> allowedNodes) {
      this.allowedNodes = new HashSet<>(allowedNodes);
      return this;
    }

    /**
     * Modifies the nodes which are allowed to pick up and manage services using the service id. If no node of the given
     * ones is available to pick up a service, creations will fail.
     *
     * @param modifier the modifier to be applied to the already added allowed nodes of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given node unique id collection is null.
     */
    public @NonNull Builder modifyAllowedNodes(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.allowedNodes);
      return this;
    }

    /**
     * Builds a new service id instance based on this builder.
     *
     * @return the created service id.
     * @throws NullPointerException     if no task or environment was given.
     * @throws IllegalArgumentException if the task id is invalid.
     */
    public @NonNull ServiceId build() {
      Preconditions.checkNotNull(this.taskName, "no task name given");
      Preconditions.checkNotNull(this.environmentName, "no environment given");
      Preconditions.checkArgument(this.taskServiceId == -1 || this.taskServiceId > 0, "taskServiceId <= 0");

      return new ServiceId(
        this.taskName,
        this.nameSplitter,
        Set.copyOf(this.allowedNodes),
        this.uniqueId,
        this.taskServiceId,
        this.nodeUniqueId,
        this.environmentName,
        this.environment);
    }
  }
}
