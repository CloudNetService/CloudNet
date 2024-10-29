/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge;

import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A world position represents a position in a platform dependent world.
 *
 * @param x     the x coordinate of the position.
 * @param y     the y coordinate of the position.
 * @param z     the z coordinate of the position.
 * @param yaw   the yaw of the position.
 * @param pitch the pitch of the position.
 * @param world the world this position is located on.
 * @param group the group this position is created for.
 * @since 4.0
 */
public record WorldPosition(
  double x,
  double y,
  double z,
  double yaw,
  double pitch,
  @NonNull String world,
  @Nullable String group
) {

  public static final Type COL_TYPE = TypeFactory.parameterizedClass(Collection.class, WorldPosition.class);
}
