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

package eu.cloudnetservice.driver.channel;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget.Type;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * Represents a sender of a channel message. A channel message sender is not required to be the actual component sending
 * the message nor is there a requirement for the name to match the driver environment when creating a new target. But
 * it is strongly recommended to not mismatch information as it may lead to confusing results on the receiver site.
 * <p>
 * Note: It is not recommended using the constructor directly. Consider using either {@link #self()} if you want a jvm
 * static sender representing the current network component or {@link  #of(String, DriverEnvironment)} if you want to
 * create a sender for another network component.
 *
 * @param name the name of the new sender.
 * @param type the type of the new sender.
 * @see ChannelMessage
 * @see ChannelMessageTarget
 * @since 4.0
 */
public record ChannelMessageSender(@NonNull String name, @NonNull DriverEnvironment type) {

  private static final ChannelMessageSender SELF = of(
    CloudNetDriver.instance().componentName(),
    CloudNetDriver.instance().environment());

  /**
   * Creates a new channel message sender with the given name and environment. If you want the sender representation of
   * the current network component consider using {@link #self()} instead.
   *
   * @param name        the name of the new sender.
   * @param environment the type of the new sender.
   * @return a new channel message sender with the given name and environment.
   * @throws NullPointerException if the given name or environment is null.
   */
  public static @NonNull ChannelMessageSender of(@NonNull String name, @NonNull DriverEnvironment environment) {
    return new ChannelMessageSender(name, environment);
  }

  /**
   * Get a jvm static sender representation of this network component.
   *
   * @return a sender representation of this network component.
   */
  public static @NonNull ChannelMessageSender self() {
    return SELF;
  }

  /**
   * Checks if this sender represents the given service.
   *
   * @param serviceInfoSnapshot the service to check.
   * @return true if this sender represents the given service, false otherwise.
   * @throws NullPointerException if the given snapshot is null.
   */
  public boolean is(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.type.equals(DriverEnvironment.WRAPPER) && this.name.equals(serviceInfoSnapshot.name());
  }

  /**
   * Checks if this sender represents the given node.
   *
   * @param node the node to check.
   * @return true if this sender represents the given node, false otherwise.
   * @throws NullPointerException if the given input is null.
   */
  public boolean is(@NonNull NetworkClusterNode node) {
    return this.type.equals(DriverEnvironment.NODE) && this.name.equals(node.uniqueId());
  }

  /**
   * Converts this sender to a target. The target has either the type {@link Type#NODE} if this sender represents a
   * node, or the type {@link Type#SERVICE} if this sender represents a wrapper (or is running embedded).
   *
   * @return a new {@link ChannelMessageTarget} based on the information of this sender.
   */
  public @NonNull ChannelMessageTarget toTarget() {
    var type = this.type.equals(DriverEnvironment.NODE) ? Type.NODE : Type.SERVICE;
    return new ChannelMessageTarget(type, this.name);
  }
}
