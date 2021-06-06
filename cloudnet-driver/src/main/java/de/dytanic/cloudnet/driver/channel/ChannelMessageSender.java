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
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ChannelMessageSender implements SerializableObject {

  private String name;
  private DriverEnvironment type;

  public ChannelMessageSender(@NotNull String name, @NotNull DriverEnvironment type) {
    this.name = name;
    this.type = type;
  }

  public ChannelMessageSender() {
  }

  public static ChannelMessageSender self() {
    return new ChannelMessageSender(CloudNetDriver.getInstance().getComponentName(),
      CloudNetDriver.getInstance().getDriverEnvironment());
  }

  public String getName() {
    return this.name;
  }

  public DriverEnvironment getType() {
    return this.type;
  }

  public boolean isEqual(ServiceInfoSnapshot serviceInfoSnapshot) {
    return this.type == DriverEnvironment.WRAPPER && this.name.equals(serviceInfoSnapshot.getName());
  }

  public boolean isEqual(NetworkClusterNode node) {
    return this.type == DriverEnvironment.CLOUDNET && this.name.equals(node.getUniqueId());
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.name);
    buffer.writeEnumConstant(this.type);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.name = buffer.readString();
    this.type = buffer.readEnumConstant(DriverEnvironment.class);
  }
}
