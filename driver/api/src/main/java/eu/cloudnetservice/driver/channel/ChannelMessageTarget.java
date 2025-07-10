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

package eu.cloudnetservice.driver.channel;

import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceTask;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Identifies one or more targets of a channel message, optionally with a {@link #name()} for further specification of a
 * target. A channel message target can, for example, represent all components in the network or only a single node or
 * service. For services, there are special targets for filtering, for example, based on the task they are based on.
 *
 * @since 4.0
 */
public final class ChannelMessageTarget {

  private static final ChannelMessageTarget ALL = new ChannelMessageTarget(Type.ALL, null);
  private static final ChannelMessageTarget ALL_NODES = new ChannelMessageTarget(Type.NODE, null);
  private static final ChannelMessageTarget ALL_SERVICES = new ChannelMessageTarget(Type.SERVICE, null);

  private final Type type;
  private final String name;

  /**
   * Constructs a new channel message target using the given type and optional name. For internal use only.
   *
   * @param type the type of component targeted by this channel message target.
   * @param name an optional name to further narrow the target. The meaning of the name is derived from the type.
   * @throws NullPointerException if the given type is null.
   */
  private ChannelMessageTarget(@NonNull Type type, @Nullable String name) {
    this.type = type;
    this.name = name;
  }

  /**
   * Get a channel message target that targets all components in the network.
   *
   * @return a channel message target that targets all components in the network.
   */
  public static @NonNull ChannelMessageTarget all() {
    return ALL;
  }

  /**
   * Get a channel message target that targets all node components in the network.
   *
   * @return a channel message target that targets all node components in the network.
   */
  public static @NonNull ChannelMessageTarget allNodes() {
    return ALL_NODES;
  }

  /**
   * Get a channel message target that targets all service (wrapper) components in the network.
   *
   * @return a channel message target that targets all service components in the network.
   */
  public static @NonNull ChannelMessageTarget allServices() {
    return ALL_SERVICES;
  }

  /**
   * Get a channel message target that targets the given node in the network.
   *
   * @param node the node to target.
   * @return a channel message target that targets the given node in the network.
   * @throws NullPointerException if the given node to target is null.
   */
  public static @NonNull ChannelMessageTarget node(@NonNull NetworkClusterNode node) {
    return node(node.uniqueId());
  }

  /**
   * Get a channel message target that targets the node with the given name in the network.
   *
   * @param nodeId the id of the node to target.
   * @return a channel message target that targets the node with the given name in the network.
   * @throws NullPointerException if the given node id is null.
   */
  public static @NonNull ChannelMessageTarget node(@NonNull String nodeId) {
    return new ChannelMessageTarget(Type.NODE, nodeId);
  }

  /**
   * Get a channel message target that targets the given service. Note that services are identified by their name and
   * not by their unique id, therefore a channel message might be sent to a different service if the actual target
   * restarts.
   *
   * @param serviceId the service id of the service to target.
   * @return a channel message target that targets the given service.
   * @throws NullPointerException if the given service id is null.
   */
  public static @NonNull ChannelMessageTarget service(@NonNull ServiceId serviceId) {
    return service(serviceId.name());
  }

  /**
   * Get a channel message target that targets the service identified by the given name. Note that services are
   * identified by their name and not by their unique id, therefore a channel message might be sent to a different
   * service if the actual target restarts.
   *
   * @param serviceName the name of the service to target.
   * @return a channel message target that targets the service identified by the given name.
   * @throws NullPointerException if the given service name is null.
   */
  public static @NonNull ChannelMessageTarget service(@NonNull String serviceName) {
    return new ChannelMessageTarget(Type.SERVICE, serviceName);
  }

  /**
   * Get a channel message target that targets all services that are based on the given service task.
   *
   * @param task the service task to target.
   * @return a channel message target that targets all services that are based on the given service task.
   * @throws NullPointerException if the given service task is null.
   */
  public static @NonNull ChannelMessageTarget servicesByTask(@NonNull ServiceTask task) {
    return servicesByTask(task.name());
  }

  /**
   * Get a channel message target that targets all services that are based on the service task identified by the given
   * name.
   *
   * @param taskName the name of the task to target.
   * @return a channel message target that targets all services that are based on the given service task.
   * @throws NullPointerException if the given service task name is null.
   */
  public static @NonNull ChannelMessageTarget servicesByTask(@NonNull String taskName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_TASK, taskName);
  }

  /**
   * Get a channel message target that targets all services that are in the given group.
   *
   * @param group the group to target.
   * @return a channel message target that targets all services that are in the given group.
   * @throws NullPointerException if the given group is null.
   */
  public static @NonNull ChannelMessageTarget servicesByGroup(@NonNull GroupConfiguration group) {
    return servicesByGroup(group.name());
  }

  /**
   * Get a channel message target that targets all services that are in the group identified by the given name.
   *
   * @param groupName the name of the group to target.
   * @return a channel message target that targets all services that are in the given group.
   * @throws NullPointerException if the given group name is null.
   */
  public static @NonNull ChannelMessageTarget servicesByGroup(@NonNull String groupName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_GROUP, groupName);
  }

  /**
   * Get a channel message target that targets all services that are using the given service environment.
   *
   * @param environment the environment to target.
   * @return a channel message target that targets all services that are using the given service environment.
   * @throws NullPointerException if the given environment is null.
   */
  public static @NonNull ChannelMessageTarget servicesByEnvironment(@NonNull ServiceEnvironmentType environment) {
    return servicesByEnvironment(environment.name());
  }

  /**
   * Get a channel message target that targets all services that are using the service environment identified by the
   * given name.
   *
   * @param environmentName the name of the environment to target.
   * @return a channel message target that targets all services that are using the given service environment.
   * @throws NullPointerException if the given environment name is null.
   */
  public static @NonNull ChannelMessageTarget servicesByEnvironment(@NonNull String environmentName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_ENV, environmentName);
  }

  /**
   * Get a channel message target that targets all services that have the given property associated with any value.
   *
   * @param property the property that must be associated on target services.
   * @return a channel message target that targets all services that have the given property associated with any value.
   * @throws NullPointerException if the given property is null.
   */
  public static @NonNull ChannelMessageTarget servicesWithProperty(@NonNull DocProperty<?> property) {
    return servicesWithProperty(property.key());
  }

  /**
   * Get a channel message target that targets all services that have the property identified by the given key
   * associated with any value.
   *
   * @param propertyKey the key of the property that must be associated on target services.
   * @return a channel message target that targets all services that have the given property associated with any value.
   * @throws NullPointerException if the given property key is null.
   */
  public static @NonNull ChannelMessageTarget servicesWithProperty(@NonNull String propertyKey) {
    return new ChannelMessageTarget(Type.SERVICES_WITH_PROPERTY, propertyKey);
  }

  /**
   * Get the type of component that is targeted. For internal differentiation only.
   *
   * @return the type of component that is targeted.
   */
  @ApiStatus.Internal
  public @NonNull Type type() {
    return this.type;
  }

  /**
   * Get an optional name of the target component. Can be null if the target type does not demand for a name (for
   * example, when targeting all services). For internal differentiation only.
   *
   * @return an optional name of the target component.
   */
  @ApiStatus.Internal
  @UnknownNullability("only null when the associated target type does not demand a name")
  public String name() {
    return this.name;
  }

  /**
   * The possible types of targets a channel message can have. Intended for internal differentiation only.
   *
   * @since 4.0
   */
  @ApiStatus.Internal
  public enum Type {

    /**
     * All available components. No name is associated with this target.
     */
    ALL,
    /**
     * One or multiple nodes in the network. A name can be supplied to select one particular target node.
     */
    NODE,
    /**
     * One or multiple services in the network. A name can be supplied to select one particular target service.
     */
    SERVICE,

    /**
     * All services in the network that are based on the provided task (identified by the required name).
     */
    SERVICES_BY_TASK,
    /**
     * All services in the network that are in on the provided group (identified by the required name).
     */
    SERVICES_BY_GROUP,
    /**
     * All services in the network that are using the provided environment (identified by the required name).
     */
    SERVICES_BY_ENV,
    /**
     * All services in the network that have the given property key with any value (identified by the required name).
     */
    SERVICES_WITH_PROPERTY,
  }
}
