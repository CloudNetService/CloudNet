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

package eu.cloudnetservice.ext.component;

import lombok.NonNull;
import net.kyori.adventure.text.Component;

public final class InternalPlaceholder {

  public static @NonNull Component create(@NonNull String name) {
    return Component.text("{__placeholder:")
      .append(Component.text(name))
      .append(Component.text("}"));
  }

  public static @NonNull String replacePlaceholders(@NonNull String input) {
    return input.replaceAll("\\{__placeholder:(.*)\\}", "<$1>");
  }

  private InternalPlaceholder() {
    throw new UnsupportedOperationException();
  }

}
