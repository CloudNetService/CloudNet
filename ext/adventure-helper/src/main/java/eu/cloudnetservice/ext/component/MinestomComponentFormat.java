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

import com.google.gson.JsonElement;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class MinestomComponentFormat implements ComponentFormat<String> {

  private static final GsonComponentSerializer SERIALIZER = GsonComponentSerializer.gson();

  @Override
  public @NonNull Component toAdventure(@NonNull String component) {
    return SERIALIZER.deserialize(component);
  }

  public @NonNull Component toAdventureFromTree(@NonNull JsonElement component) {
    return SERIALIZER.deserializeFromTree(component);
  }

  @Override
  public @NonNull String fromAdventure(@NonNull Component adventure) {
    return SERIALIZER.serialize(adventure);
  }

  public @NonNull JsonElement fromAdventureToTree(@NonNull Component adventure) {
    return SERIALIZER.serializeToTree(adventure);
  }
}
