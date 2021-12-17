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

package eu.cloudnetservice.modules.npc.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;

public record NPCConfiguration(@NonNull List<NPCConfigurationEntry> entries) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull NPCConfiguration configuration) {
    return builder().entries(configuration.entries());
  }

  public static class Builder {

    private List<NPCConfigurationEntry> entries = new ArrayList<>();

    public @NonNull Builder entries(@NonNull Collection<NPCConfigurationEntry> entries) {
      this.entries = new ArrayList<>(entries);
      return this;
    }

    public @NonNull NPCConfiguration build() {
      return new NPCConfiguration(this.entries);
    }
  }
}
