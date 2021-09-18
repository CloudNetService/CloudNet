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

package de.dytanic.cloudnet.driver.channel;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ChannelMessageSender {

  private static final ChannelMessageSender selfSender;

  static {
    selfSender = new ChannelMessageSender(
      CloudNetDriver.getInstance().getComponentName(),
      CloudNetDriver.getInstance().getDriverEnvironment()
    );
  }

  private final String name;
  private final DriverEnvironment type;

  public ChannelMessageSender(@NotNull String name, @NotNull DriverEnvironment type) {
    this.name = name;
    this.type = type;
  }

  /**
   * @return a sender that corresponds to the component the method is called on
   */
  public static @NotNull ChannelMessageSender self() {
    return selfSender;
  }

  /**
   * @return the name of the channel message sender
   */
  public @NotNull String getName() {
    return this.name;
  }

  /**
   * @return the environment of the sender
   * @see DriverEnvironment
   */
  public @NotNull DriverEnvironment getType() {
    return this.type;
  }

  /**
   * Check if the given ServiceInfoSnapshot is the sender of the channel message
   *
   * @param serviceInfoSnapshot the {@link ServiceInfoSnapshot} to check
   * @return whether the given ServiceInfoSnapshot is the sender of the channel message
   */
  public boolean isEqual(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.type == DriverEnvironment.WRAPPER && this.name.equals(serviceInfoSnapshot.getName());
  }

  /**
   * Check if the given NetworkClusterNode is the sender of the channel message
   *
   * @param node the {@link NetworkClusterNode} to check
   * @return whether the given NetworkClusterNode is the sender of the channel message
   */
  public boolean isEqual(@NotNull NetworkClusterNode node) {
    return this.type == DriverEnvironment.CLOUDNET && this.name.equals(node.getUniqueId());
  }
}
