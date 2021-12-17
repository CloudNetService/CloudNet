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

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class ServicePlayer extends JsonDocPropertyHolder implements Comparable<ServicePlayer>, INameable {

  public ServicePlayer(@NotNull UUID uniqueId, @NotNull String name) {
    this.properties = JsonDocument.newDocument().append("uniqueId", uniqueId).append("name", name);
  }

  public ServicePlayer(@NotNull JsonDocument properties) {
    this.properties = properties;
  }

  public @NotNull UUID uniqueId() {
    return this.properties.get("uniqueId", UUID.class);
  }

  @Override
  public @NotNull String name() {
    return this.properties.getString("name");
  }

  @Override
  public int compareTo(@NotNull ServicePlayer o) {
    return this.name().compareTo(o.name());
  }
}
