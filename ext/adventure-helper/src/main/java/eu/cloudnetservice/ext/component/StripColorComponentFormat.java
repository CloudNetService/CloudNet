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

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public record StripColorComponentFormat<C>(ComponentFormat<C> parent) implements ComponentFormat<C> {

  @Override
  public @NonNull Component toAdventure(@NonNull C component) {
    return this.strip(this.parent.toAdventure(component));
  }

  @Override
  public @NonNull C fromAdventure(@NonNull Component adventure) {
    return this.parent.fromAdventure(this.strip(adventure));
  }

  private @NonNull Component strip(@NonNull Component component) {
    return component.color(null)
      .decorations(Arrays.stream(TextDecoration.values()).collect(Collectors.toSet()), false)
      .children(component.children().stream().map(this::strip).toList());
  }
}
