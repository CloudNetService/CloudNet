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

package eu.cloudnetservice.modules.bridge.player;

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.UUID;
import lombok.NonNull;

public class ServicePlayer extends JsonDocPropertyHolder implements Comparable<ServicePlayer>, Nameable {

  public ServicePlayer(@NonNull UUID uniqueId, @NonNull String name) {
    super(JsonDocument.newDocument().append("uniqueId", uniqueId).append("name", name));
  }

  public ServicePlayer(@NonNull JsonDocument properties) {
    super(properties);
  }

  public @NonNull UUID uniqueId() {
    return this.properties.get("uniqueId", UUID.class);
  }

  @Override
  public @NonNull String name() {
    return this.properties.getString("name");
  }

  @Override
  public int compareTo(@NonNull ServicePlayer o) {
    return this.name().compareTo(o.name());
  }
}
