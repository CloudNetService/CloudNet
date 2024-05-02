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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

public record PreciseLegacyComponentFormat(@NonNull PlaceholderComponentFormat<String> parent)
  implements PlaceholderComponentFormat<String> {


  @Override
  public @NonNull Component toAdventure(@NonNull String component) {
    var lastStyle = this.lastStyle(component);
    var reset = Style.style(NamedTextColor.WHITE);
    for (var decoration : TextDecoration.values()) {
      reset = reset.decoration(decoration, false);
    }
    return Component.empty().style(lastStyle)
      .append(Component.empty().style(reset)
        .append(this.parent.toAdventure(component))
      );
  }

  @Override
  public @NonNull String fromAdventure(@NonNull Component adventure) {
    var placeholder = "{__style__}";
    var result = this.parent.fromAdventure(adventure.append(Component.text(placeholder)));
    result = result.replace(placeholder, "");
    return result;
  }

  private @NonNull Style lastStyle(@NonNull String str) {
    var placeholder = "{__style__}";
    str = str + placeholder;
    var component = this.parent.toAdventure(str);
    return this.find(null, new HashSet<>(), component, placeholder);
  }

  private @Nullable Style find(@Nullable TextColor color, @NonNull Set<TextDecoration> decorations,
    @NonNull Component component, @NonNull String placeholder) {
    if (component instanceof TextComponent textComponent) {
      if (textComponent.color() != null) {
        color = textComponent.color();
      }
      for (var decoration : textComponent.decorations().entrySet()) {
        switch (decoration.getValue()) {
          case FALSE -> decorations.remove(decoration.getKey());
          case TRUE -> decorations.add(decoration.getKey());
        }
      }
      if (textComponent.content().contains(placeholder)) {
        return Style.style(color, decorations.toArray(new TextDecoration[0]));
      }
    }

    for (var child : component.children()) {
      var style = this.find(color, decorations, child, placeholder);
      if (style != null) {
        return style;
      }
    }

    return null;
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> withPlaceholders(@NonNull Map<String, Component> placeholders) {
    return new PreciseLegacyComponentFormat(this.parent.withPlaceholders(placeholders));
  }

  @Override
  public @NonNull PlaceholderComponentFormat limitPlaceholders() {
    return new PreciseLegacyComponentFormat(this.parent.limitPlaceholders());
  }

  @Override
  public @NonNull PlaceholderComponentFormat withColorPlaceholder(@NonNull String name, @Nullable TextColor color) {
    return new PreciseLegacyComponentFormat(this.parent.withColorPlaceholder(name, color));
  }
}
