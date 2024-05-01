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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.Nullable;

public record MinimessageComponentFormat(@NonNull MiniMessage deserializer,
                                         @NonNull List<TagResolver> placeholders) implements PlaceholderComponentFormat<String> {

  public MinimessageComponentFormat() {
    this(MiniMessage.builder().build(), List.of());
  }

  @Override
  public @NonNull String fromAdventure(@NonNull Component adventure) {
    return this.deserializer.serialize(adventure);
  }

  @Override
  public @NonNull Component toAdventure(@NonNull String component) {
    return this.deserializer.deserialize(component, this.placeholders().toArray(new TagResolver[0]));
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> withPlaceholders(@NonNull Map<String, Component> placeholders) {
    var list = new ArrayList<>(this.placeholders);
    placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.component(entry.getKey(), entry.getValue()))
            .forEach(list::add);
    return new MinimessageComponentFormat(this.deserializer, list);
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> limitPlaceholders() {
    return new MinimessageComponentFormat(
      MiniMessage.builder()
        .tags(TagResolver.builder()
          .resolvers(
            StandardTags.color(),
            StandardTags.decorations(),
            StandardTags.rainbow(),
            StandardTags.gradient(),
            StandardTags.reset()
          ).build()
        )
        .build(),
      this.placeholders
    );
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> withColorPlaceholder(@NonNull String name, @Nullable TextColor color) {
    var list = new ArrayList<>(this.placeholders);
    list.add(TagResolver.builder()
      .tag(name, Tag.styling((builder) -> builder.color(color)))
      .build());
    return new MinimessageComponentFormat(this.deserializer, list);
  }
}
