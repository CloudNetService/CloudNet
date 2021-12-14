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

package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
public class WorldPosition {

  public static final Type COL_TYPE = new TypeToken<Collection<WorldPosition>>() {
  }.getType();

  protected final double x;
  protected final double y;
  protected final double z;
  protected final double yaw;
  protected final double pitch;

  protected final String world;
  protected final String group;

  public WorldPosition(
    double x,
    double y,
    double z,
    double yaw,
    double pitch,
    @NotNull String world,
    @Nullable String group
  ) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.world = world;
    this.group = group;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public double getYaw() {
    return this.yaw;
  }

  public double getPitch() {
    return this.pitch;
  }

  public @NotNull String getWorld() {
    return this.world;
  }

  public @Nullable String getGroup() {
    return this.group;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    var position = (WorldPosition) o;
    return Double.compare(position.x, this.x) == 0
      && Double.compare(position.y, this.y) == 0
      && Double.compare(position.z, this.z) == 0
      && Double.compare(position.yaw, this.yaw) == 0
      && Double.compare(position.pitch, this.pitch) == 0
      && Objects.equals(this.world, position.world)
      && Objects.equals(this.group, position.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.x, this.y, this.z, this.yaw, this.pitch, this.world, this.group);
  }
}
