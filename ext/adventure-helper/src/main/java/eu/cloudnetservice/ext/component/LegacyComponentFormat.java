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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

record LegacyComponentFormat(@NonNull Supplier<LegacyComponentSerializer> serializer,
                             @NonNull Map<String, String> placeholders) implements PlaceholderComponentFormat<String> {

  public LegacyComponentFormat(@NonNull LegacyComponentSerializer serializer) {
    this(() -> serializer, Map.of());
  }

  public LegacyComponentFormat(@NonNull Supplier<LegacyComponentSerializer> serializer) {
    this(serializer, Map.of());
  }

  @Override
  public @NonNull Component toAdventure(@NonNull String component) {
    return this.serializer.get().deserialize(component);
  }

  @Override
  public @NonNull String fromAdventure(@NonNull Component adventure) {
    return this.serializer.get().serialize(adventure);
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> withPlaceholders(@NonNull Map<String, Component> placeholders) {
    var map = new HashMap<>(this.placeholders);
    placeholders.entrySet()
      .stream()
      .map((entry) -> Map.entry(entry.getKey(), this.serializer.get().serialize(entry.getValue())))
      .forEach((entry) -> map.put(entry.getKey(), entry.getValue()));
    return new LegacyComponentFormat(this.serializer, map);
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> limitPlaceholders() {
    return new LegacyComponentFormat(this.serializer, Map.of());
  }

  @Override
  public @NonNull PlaceholderComponentFormat<String> withColorPlaceholder(String name, TextColor color) {
    var map = new HashMap<>(this.placeholders);
    map.put(name, this.serializer.get().serialize(Component.empty().color(color)));
    return new LegacyComponentFormat(this.serializer, map);
  }
}
