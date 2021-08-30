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

  private final String name;
  private final DriverEnvironment type;

  public ChannelMessageSender(@NotNull String name, @NotNull DriverEnvironment type) {
    this.name = name;
    this.type = type;
  }

  public static @NotNull ChannelMessageSender self() {
    return new ChannelMessageSender(
      CloudNetDriver.getInstance().getComponentName(),
      CloudNetDriver.getInstance().getDriverEnvironment());
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull DriverEnvironment getType() {
    return this.type;
  }

  public boolean isEqual(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.type == DriverEnvironment.WRAPPER && this.name.equals(serviceInfoSnapshot.getName());
  }

  public boolean isEqual(@NotNull NetworkClusterNode node) {
    return this.type == DriverEnvironment.CLOUDNET && this.name.equals(node.getUniqueId());
  }
}
