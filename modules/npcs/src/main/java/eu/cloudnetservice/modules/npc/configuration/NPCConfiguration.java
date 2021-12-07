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
import org.jetbrains.annotations.NotNull;

public class NPCConfiguration {

  private final List<NPCConfigurationEntry> entries;

  public NPCConfiguration(List<NPCConfigurationEntry> entries) {
    this.entries = entries;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull NPCConfiguration configuration) {
    return builder().entries(configuration.getEntries());
  }

  public @NotNull List<NPCConfigurationEntry> getEntries() {
    return this.entries;
  }

  public static class Builder {

    private List<NPCConfigurationEntry> entries = new ArrayList<>();

    public @NotNull Builder entries(@NotNull Collection<NPCConfigurationEntry> entries) {
      this.entries = new ArrayList<>(entries);
      return this;
    }

    public @NotNull NPCConfiguration build() {
      return new NPCConfiguration(this.entries);
    }
  }
}
