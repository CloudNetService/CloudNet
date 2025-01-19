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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a receiver of a channel message. A channel message can be sent to a variety of receiver types including:
 * <ul>
 *   <li>ALL: includes all network components which are within the network
 *   <li>NODE: includes all nodes which are within the network
 *   <li>SERVICE: includes all services which are within the network
 *   <li>TASK: includes all services of the given task
 *   <li>GROUP: includes all services which are in the given group
 *   <li>ENVIRONMENT: includes all services which are using the given environment. See {@link #environment(ServiceEnvironmentType)}
 * </ul>
 * <p>
 * A channel message target can optionally take a name of the target. If no name is provided its interpreted as
 * &quot;all components of the given type&quot;. It is illegal to create a target with no name given for the types
 * {@link Type#TASK} and {@link Type#GROUP}. However, a creation of these types in combination with null as the
 * name is a valid operation and will not fail.
 * <p>
 * Note: Types are not overriding meaning that setting a target for all services and (for example) specifically the
 * service Abc-1 is valid and will cause the service to receive the channel message twice.
 * <p>
 * Do not extend this class to get access to it's constructors, use {@link #of(Type, String)} or for targeting a specific
 * environment {@link #environment(ServiceEnvironmentType)} instead.
 *
 * @see ChannelMessage
 * @see ChannelMessageSender
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public final class ChannelMessageTarget {

  private static final ChannelMessageTarget ALL = new ChannelMessageTarget(Type.ALL, null);
  private static final ChannelMessageTarget ALL_NODES = new ChannelMessageTarget(Type.NODE, null);
  private static final ChannelMessageTarget ALL_SERVICES = new ChannelMessageTarget(Type.SERVICE, null);

  private final Type type;
  private final String name;
  private final ServiceEnvironmentType environment;

  /**
   * Constructs a new ChannelMessageTarget with no environment as its target.
   *
   * @param type the type of the target.
   * @param name the optional name of the target.
   * @throws NullPointerException if the given type is null.
   */
  protected ChannelMessageTarget(@NonNull Type type, @Nullable String name) {
    this.type = type;
    this.name = name;
    this.environment = null;
  }

  /**
   * Constructs a new ChannelMessageTarget targeting a specific service environment. No name will be set.
   *
   * @param environment the environment of the target.
   * @throws NullPointerException if the given environment is null.
   */
  protected ChannelMessageTarget(@NonNull ServiceEnvironmentType environment) {
    this.type = Type.ENVIRONMENT;
    this.name = null;
    this.environment = environment;
  }

  /**
   * Internal method used for automated instantiation of this class.
   *
   * @param type        the type of the target.
   * @param name        the optional name of the target.
   * @param environment the optional environment of the target.
   * @throws NullPointerException if the given type is null.
   */
  protected ChannelMessageTarget(
    @NonNull Type type,
    @Nullable String name,
    @Nullable ServiceEnvironmentType environment
  ) {
    this.type = type;
    this.name = name;
    this.environment = environment;
  }

  /**
   * Constructs a new ChannelMessageTarget targeting a specific service environment.
   *
   * @param type the environment to target.
   * @return the created ChannelMessageTarget.
   * @throws NullPointerException if the given type is null.
   */
  public static @NonNull ChannelMessageTarget environment(@NonNull ServiceEnvironmentType type) {
    return new ChannelMessageTarget(type);
  }

  /**
   * Constructs a new ChannelMessageTarget of the given type with the optional name set.
   *
   * @param type the type of the new channel message target.
   * @param name optional name of the component with the given type to target.
   * @return the created ChannelMessageTarget.
   * @throws NullPointerException     if given type is null.
   * @throws IllegalArgumentException if type is ENVIRONMENT. For that purpose use {@link #environment(ServiceEnvironmentType)}
   *                                  instead.
   */
  public static @NonNull ChannelMessageTarget of(@NonNull Type type, @Nullable String name) {
    Preconditions.checkArgument(type != Type.ENVIRONMENT, "Unable to target environment using name");
    // check if we have a constant value for the type
    if (name == null) {
      switch (type) {
        case ALL:
          return ALL;
        case NODE:
          return ALL_NODES;
        case SERVICE:
          return ALL_SERVICES;
        default:
          break;
      }
    }
    // create a new target for the type
    return new ChannelMessageTarget(type, name);
  }

  /**
   * Returns the type of network component this ChannelMessageTarget targets. Can never be null.
   *
   * @return the type of network component this ChannelMessageTarget targets.
   */
  public @NonNull Type type() {
    return this.type;
  }

  /**
   * Returns the name of the network component this ChannelMessageTarget targets. This may not be null if the target
   * type is TASK or GROUP, but will always be null if the type is ENVIRONMENT. If a name is given when this channel
   * message target has its type set to ALL it will silently be ignored.
   *
   * @return the name of the network component this ChannelMessageTarget targets.
   */
  public @UnknownNullability String name() {
    return this.name;
  }

  /**
   * Returns the service environment this ChannelMessageTarget targets. This may not be null if the target type is
   * ENVIRONMENT. In all other cases the environment can be non-null but will silently get ignored.
   *
   * @return the service environment this ChannelMessageTarget targets.
   */
  public @UnknownNullability ServiceEnvironmentType environment() {
    return this.environment;
  }

  /**
   * Represents a type of network component this channel message targets.
   */
  public enum Type {

    /**
     * Represents all network components in the network. A set name or environment will always get ignored.
     */
    ALL,
    /**
     * Represents a node within the network. A name can optionally be present to target a specific node.
     */
    NODE,
    /**
     * Represents a service within the network. A name can optionally be present to target a specific node.
     */
    SERVICE,
    /**
     * Represents all services within the network which are using the given task as its base. A name must be present.
     */
    TASK,
    /**
     * Represents all services within the network which are in the given group. A name must be present.
     */
    GROUP,
    /**
     * Represents all services within the network which are using the given environment. An environment must be
     * present.
     */
    ENVIRONMENT
  }
}
