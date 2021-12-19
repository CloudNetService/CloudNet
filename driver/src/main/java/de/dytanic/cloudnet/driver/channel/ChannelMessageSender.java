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
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget.Type;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

public record ChannelMessageSender(@NonNull String name, @NonNull DriverEnvironment type) {

  private static final ChannelMessageSender SELF = of(
    CloudNetDriver.instance().componentName(),
    CloudNetDriver.instance().environment());

  public static @NonNull ChannelMessageSender of(@NonNull String name, @NonNull DriverEnvironment environment) {
    return new ChannelMessageSender(name, environment);
  }

  public static @NonNull ChannelMessageSender self() {
    return SELF;
  }

  public boolean is(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.type == DriverEnvironment.WRAPPER && this.name.equals(serviceInfoSnapshot.name());
  }

  public boolean is(@NonNull NetworkClusterNode node) {
    return this.type == DriverEnvironment.CLOUDNET && this.name.equals(node.uniqueId());
  }

  public @NonNull ChannelMessageTarget toTarget() {
    var type = this.type == DriverEnvironment.CLOUDNET ? Type.NODE : Type.SERVICE;
    return new ChannelMessageTarget(type, this.name);
  }
}
