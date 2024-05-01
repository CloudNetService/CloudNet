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

package eu.cloudnetservice.modules.npc.configuration;

import eu.cloudnetservice.ext.component.InternalPlaceholder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public record InventoryConfiguration(
  int inventorySize,
  boolean dynamicSize,
  @NonNull ItemLayoutHolder defaultItems,
  @NonNull Map<String, ItemLayoutHolder> perGroupLayouts,
  @NonNull Map<Integer, ItemLayout> fixedItems
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull InventoryConfiguration configuration) {
    return builder()
      .inventorySize(configuration.inventorySize())
      .dynamicSize(configuration.dynamicSize())
      .defaultItems(configuration.defaultItems())
      .perGroupLayouts(configuration.perGroupLayouts())
      .fixedItems(configuration.fixedItems());
  }

  public @NonNull ItemLayoutHolder getHolder(String @NonNull ... groups) {
    if (groups.length == 0) {
      return this.defaultItems;
    } else if (groups.length == 1) {
      return this.perGroupLayouts.getOrDefault(groups[0], this.defaultItems);
    } else {
      for (var group : groups) {
        var holder = this.perGroupLayouts.get(group);
        if (holder != null) {
          return holder;
        }
      }
      // use the default holder
      return this.defaultItems;
    }
  }

  public record ItemLayoutHolder(
    @NonNull ItemLayout emptyLayout,
    @NonNull ItemLayout onlineLayout,
    @NonNull ItemLayout fullLayout,
    @NonNull ItemLayout ingameLayout
  ) {

  }

  public static class Builder {

    private int inventorySize = 54;
    private boolean dynamicSize = true;

    private ItemLayoutHolder defaultItems = new ItemLayoutHolder(
      ItemLayout.builder()
        .material("EGG")
        .displayName(InternalPlaceholder.create("name").color(NamedTextColor.GRAY))
        .lore(Arrays.asList(
          Component.text(" "),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("state").color(NamedTextColor.YELLOW)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("online_players").color(NamedTextColor.GRAY))
            .append(Component.text("/"))
            .append(InternalPlaceholder.create("max_players").color(NamedTextColor.GRAY)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("motd").color(NamedTextColor.GRAY))
        )).build(),
      ItemLayout.builder()
        .material("EMERALD")
        .displayName(InternalPlaceholder.create("name").color(NamedTextColor.GRAY))
        .lore(Arrays.asList(
          Component.text(" "),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("state").color(NamedTextColor.YELLOW)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("online_players").color(NamedTextColor.GRAY))
            .append(Component.text("/"))
            .append(InternalPlaceholder.create("max_players").color(NamedTextColor.GRAY)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("motd").color(NamedTextColor.GRAY))
        )).build(),
      ItemLayout.builder()
        .material("REDSTONE")
        .displayName(InternalPlaceholder.create("name").color(NamedTextColor.GRAY))
        .lore(Arrays.asList(
          Component.text(" "),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("state").color(NamedTextColor.YELLOW)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("online_players").color(NamedTextColor.GRAY))
            .append(Component.text("/"))
            .append(InternalPlaceholder.create("max_players").color(NamedTextColor.GRAY)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("motd").color(NamedTextColor.GRAY))
        )).build(),
      ItemLayout.builder()
        .material("REDSTONE")
        .displayName(InternalPlaceholder.create("name").color(NamedTextColor.GRAY))
        .lore(Arrays.asList(
          Component.text(" "),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(Component.text("Ingame", NamedTextColor.YELLOW)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("online_players").color(NamedTextColor.GRAY))
            .append(Component.text("/"))
            .append(InternalPlaceholder.create("max_players").color(NamedTextColor.GRAY)),
          Component.text("● ", NamedTextColor.DARK_GRAY)
            .append(InternalPlaceholder.create("motd").color(NamedTextColor.GRAY))
        )).build());

    private Map<Integer, ItemLayout> fixedItems = new HashMap<>();
    private Map<String, ItemLayoutHolder> perGroupLayouts = new HashMap<>();

    public @NonNull Builder inventorySize(int inventorySize) {
      this.inventorySize = inventorySize;
      return this;
    }

    public @NonNull Builder dynamicSize(boolean dynamicSize) {
      this.dynamicSize = dynamicSize;
      return this;
    }

    public @NonNull Builder defaultItems(@NonNull ItemLayoutHolder defaultItems) {
      this.defaultItems = defaultItems;
      return this;
    }

    public @NonNull Builder perGroupLayouts(@NonNull Map<String, ItemLayoutHolder> perGroupLayouts) {
      this.perGroupLayouts = new HashMap<>(perGroupLayouts);
      return this;
    }

    public @NonNull Builder fixedItems(@NonNull Map<Integer, ItemLayout> fixedItems) {
      this.fixedItems = new HashMap<>(fixedItems);
      return this;
    }

    public @NonNull InventoryConfiguration build() {
      return new InventoryConfiguration(
        this.inventorySize,
        this.dynamicSize,
        this.defaultItems,
        this.perGroupLayouts,
        this.fixedItems);
    }
  }
}
