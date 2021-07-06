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

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class NetworkServiceInfo implements SerializableObject {

  protected ServiceId serviceId;
  protected String[] groups;

  public NetworkServiceInfo(ServiceId serviceId, String[] groups) {
    this.serviceId = serviceId;
    this.groups = groups;
  }

  public NetworkServiceInfo() {
  }

  public ServiceEnvironmentType getEnvironment() {
    return this.serviceId.getEnvironment();
  }

  public UUID getUniqueId() {
    return this.serviceId.getUniqueId();
  }

  public String getServerName() {
    return this.serviceId.getName();
  }

  public String[] getGroups() {
    return this.groups;
  }

  public void setGroups(String[] groups) {
    this.groups = groups;
  }

  public String getTaskName() {
    return this.serviceId.getTaskName();
  }

  public ServiceId getServiceId() {
    return this.serviceId;
  }

  public void setServiceId(ServiceId serviceId) {
    this.serviceId = serviceId;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeObject(this.serviceId);
    buffer.writeStringArray(this.groups);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.serviceId = buffer.readObject(ServiceId.class);
    this.groups = buffer.readStringArray();
  }
}
