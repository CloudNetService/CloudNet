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
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceTask;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 *
 */
public final class ChannelMessageTarget {

  private static final ChannelMessageTarget ALL = new ChannelMessageTarget(Type.ALL, null);
  private static final ChannelMessageTarget ALL_NODES = new ChannelMessageTarget(Type.NODE, null);
  private static final ChannelMessageTarget ALL_SERVICES = new ChannelMessageTarget(Type.SERVICE, null);

  private final Type type;
  private final String name;

  /**
   * @param type
   * @param name
   */
  private ChannelMessageTarget(@NonNull Type type, @Nullable String name) {
    this.type = type;
    this.name = name;
  }

  /**
   * @return
   */
  public static @NonNull ChannelMessageTarget all() {
    return ALL;
  }

  /**
   * @return
   */
  public static @NonNull ChannelMessageTarget allNodes() {
    return ALL_NODES;
  }

  /**
   * @return
   */
  public static @NonNull ChannelMessageTarget allServices() {
    return ALL_SERVICES;
  }

  /**
   * @param node
   * @return
   */
  public static @NonNull ChannelMessageTarget node(@NonNull NetworkClusterNode node) {
    return node(node.uniqueId());
  }

  /**
   * @param nodeId
   * @return
   */
  public static @NonNull ChannelMessageTarget node(@NonNull String nodeId) {
    return new ChannelMessageTarget(Type.NODE, nodeId);
  }

  /**
   * @param serviceId
   * @return
   */
  public static @NonNull ChannelMessageTarget service(@NonNull ServiceId serviceId) {
    return service(serviceId.name());
  }

  /**
   * @param serviceName
   * @return
   */
  public static @NonNull ChannelMessageTarget service(@NonNull String serviceName) {
    return new ChannelMessageTarget(Type.SERVICE, serviceName);
  }

  /**
   * @param task
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByTask(@NonNull ServiceTask task) {
    return servicesByTask(task.name());
  }

  /**
   * @param taskName
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByTask(@NonNull String taskName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_TASK, taskName);
  }

  /**
   * @param group
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByGroup(@NonNull GroupConfiguration group) {
    return servicesByGroup(group.name());
  }

  /**
   * @param groupName
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByGroup(@NonNull String groupName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_GROUP, groupName);
  }

  /**
   * @param environment
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByEnvironment(@NonNull ServiceEnvironmentType environment) {
    return servicesByEnvironment(environment.name());
  }

  /**
   * @param environmentName
   * @return
   */
  public static @NonNull ChannelMessageTarget servicesByEnvironment(@NonNull String environmentName) {
    return new ChannelMessageTarget(Type.SERVICES_BY_ENV, environmentName);
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
  }
}
