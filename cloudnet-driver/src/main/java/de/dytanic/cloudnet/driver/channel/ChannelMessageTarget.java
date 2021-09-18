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

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class ChannelMessageTarget {

  private final Type type;
  private final String name;
  private final ServiceEnvironmentType environment;

  public ChannelMessageTarget(@NotNull Type type, @Nullable String name) {
    this(type, name, null);
  }

  public ChannelMessageTarget(@NotNull ServiceEnvironmentType environment) {
    this(Type.ENVIRONMENT, null, environment);
  }

  public ChannelMessageTarget(Type type, String name, ServiceEnvironmentType environment) {
    this.type = type;
    this.name = name;
    this.environment = environment;
  }

  public @NotNull Type getType() {
    return this.type;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  public boolean includesNode(@NotNull String uniqueId) {
    return this.type.equals(Type.ALL)
      || (this.type.equals(Type.NODE) && (this.name == null || this.name.equals(uniqueId)));
  }

  /**
   * Represents different components that can be targeted
   */
  public enum Type {
    ALL,
    NODE,
    SERVICE,
    TASK,
    GROUP,
    ENVIRONMENT
  }
}
