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

package eu.cloudnetservice.modules.labymod.config;

import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import lombok.NonNull;

public record LabyModDiscordRPC(boolean enabled, @NonNull Collection<String> excludedGroups) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull LabyModDiscordRPC discordRPC) {
    return builder()
      .enabled(discordRPC.enabled())
      .excludedGroups(discordRPC.excludedGroups());
  }

  public boolean isEnabled(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    for (var excludedGroup : this.excludedGroups) {
      if (serviceInfoSnapshot.configuration().groups().contains(excludedGroup)) {
        return false;
      }
    }
    return true;
  }

  public static class Builder {

    private boolean enabled = true;
    private Collection<String> excludedGroups = new ArrayList<>();

    public @NonNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NonNull Builder excludedGroups(@NonNull Collection<String> excludedGroups) {
      this.excludedGroups = new ArrayList<>(excludedGroups);
      return this;
    }

    public @NonNull Builder addExcludedGroup(@NonNull String excludedGroup) {
      this.excludedGroups.add(excludedGroup);
      return this;
    }

    public @NonNull LabyModDiscordRPC build() {
      return new LabyModDiscordRPC(this.enabled, this.excludedGroups);
    }
  }
}
