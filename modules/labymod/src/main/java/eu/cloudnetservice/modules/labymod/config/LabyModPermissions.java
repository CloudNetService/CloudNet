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

package eu.cloudnetservice.modules.labymod.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public record LabyModPermissions(
  boolean enabled,
  @NonNull Map<String, Boolean> permissions
) {

  private static final ImmutableMap<String, Boolean> DEFAULT_PERMISSIONS = ImmutableMap.<String, Boolean>builder()
    .put("IMPROVED_LAVA", false)
    .put("CROSSHAIR_SYNC", false)
    .put("REFILL_FIX", false)
    .put("GUI_ALL", true)
    .put("GUI_POTION_EFFECTS", true)
    .put("GUI_ARMOR_HUD", true)
    .put("GUI_ITEM_HUD", true)
    .put("BLOCKBUILD", true)
    .put("TAGS", true)
    .put("CHAT", true)
    .put("ANIMATIONS", true)
    .put("SATURATION_BAR", true)
    .put("RANGE", false)
    .put("SLOWDOWN", false)
    .build();

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean enabled = false;
    private Map<String, Boolean> permissions = new HashMap<>(DEFAULT_PERMISSIONS);

    public @NonNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NonNull Builder permissions(@NonNull Map<String, Boolean> permissions) {
      this.permissions = new HashMap<>(permissions);
      return this;
    }

    public @NonNull LabyModPermissions build() {
      Preconditions.checkNotNull(this.permissions, "Missing permissions");

      return new LabyModPermissions(this.enabled, this.permissions);
    }
  }
}
