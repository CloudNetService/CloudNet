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

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class ChannelMessageTarget implements SerializableObject {

  private Type type;
  private String name;
  private ServiceEnvironmentType environment;

  public ChannelMessageTarget(@NotNull Type type, @Nullable String name) {
    this.type = type;
    this.name = name;
  }

  public ChannelMessageTarget(@NotNull ServiceEnvironmentType environment) {
    this.type = Type.ENVIRONMENT;
    this.environment = environment;
  }

  public ChannelMessageTarget() {
  }

  public Type getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  public ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  public boolean includesNode(String uniqueId) {
    return this.type.equals(Type.ALL) || (this.type.equals(Type.NODE) && (this.name == null || this.name
      .equals(uniqueId)));
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeEnumConstant(this.type);
    buffer.writeOptionalString(this.name);
    buffer.writeOptionalEnumConstant(this.environment);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.type = buffer.readEnumConstant(Type.class);
    this.name = buffer.readOptionalString();
    this.environment = buffer.readOptionalEnumConstant(ServiceEnvironmentType.class);
  }

  public enum Type {
    ALL,
    NODE,
    SERVICE,
    TASK,
    GROUP,
    ENVIRONMENT
  }

}
