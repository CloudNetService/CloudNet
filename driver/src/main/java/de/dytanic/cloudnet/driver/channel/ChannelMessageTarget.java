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

package de.dytanic.cloudnet.driver.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public class ChannelMessageTarget {

  private static final ChannelMessageTarget ALL = new ChannelMessageTarget(Type.ALL, null);
  private static final ChannelMessageTarget ALL_NODES = new ChannelMessageTarget(Type.NODE, null);
  private static final ChannelMessageTarget ALL_SERVICES = new ChannelMessageTarget(Type.SERVICE, null);

  private final Type type;
  private final String name;
  private final ServiceEnvironmentType environment;

  protected ChannelMessageTarget(@NonNull Type type, @Nullable String name) {
    this.type = type;
    this.name = name;
    this.environment = null;
  }

  protected ChannelMessageTarget(@NonNull ServiceEnvironmentType environment) {
    this.type = Type.ENVIRONMENT;
    this.name = null;
    this.environment = environment;
  }

  @Internal
  public ChannelMessageTarget(Type type, String name, ServiceEnvironmentType environment) {
    this.type = type;
    this.name = name;
    this.environment = environment;
  }

  public static @NonNull ChannelMessageTarget environment(@NonNull ServiceEnvironmentType type) {
    return new ChannelMessageTarget(type);
  }

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

  public @NonNull Type type() {
    return this.type;
  }

  public @UnknownNullability String name() {
    return this.name;
  }

  public @UnknownNullability ServiceEnvironmentType environment() {
    return this.environment;
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
