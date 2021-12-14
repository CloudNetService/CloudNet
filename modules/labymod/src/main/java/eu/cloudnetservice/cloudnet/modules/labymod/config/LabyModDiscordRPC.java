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

package eu.cloudnetservice.cloudnet.modules.labymod.config;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class LabyModDiscordRPC {

  protected final boolean enabled;
  protected final Collection<String> excludedGroups;

  protected LabyModDiscordRPC(boolean enabled, @NotNull Collection<String> excludedGroups) {
    this.enabled = enabled;
    this.excludedGroups = excludedGroups;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull LabyModDiscordRPC discordRPC) {
    return builder()
      .enabled(discordRPC.isEnabled())
      .excludedGroups(discordRPC.getExcludedGroups());
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public @NotNull Collection<String> getExcludedGroups() {
    return this.excludedGroups;
  }

  public boolean isExcluded(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    for (var excludedGroup : this.excludedGroups) {
      if (serviceInfoSnapshot.getConfiguration().getGroups().contains(excludedGroup)) {
        return true;
      }
    }
    return false;
  }

  public static class Builder {

    private boolean enabled = true;
    private Collection<String> excludedGroups = new ArrayList<>();

    public @NotNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NotNull Builder excludedGroups(@NotNull Collection<String> excludedGroups) {
      this.excludedGroups = new ArrayList<>(excludedGroups);
      return this;
    }

    public @NotNull Builder addExcludedGroup(@NotNull String excludedGroup) {
      this.excludedGroups.add(excludedGroup);
      return this;
    }

    public @NotNull LabyModDiscordRPC build() {
      return new LabyModDiscordRPC(this.enabled, this.excludedGroups);
    }
  }

}
