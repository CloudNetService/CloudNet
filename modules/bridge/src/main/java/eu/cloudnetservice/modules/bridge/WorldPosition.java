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

package eu.cloudnetservice.modules.bridge;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record WorldPosition(
  double x,
  double y,
  double z,
  double yaw,
  double pitch,
  @NonNull String world,
  @Nullable String group
) {

  public static final Type COL_TYPE = new TypeToken<Collection<WorldPosition>>() {
  }.getType();
}
