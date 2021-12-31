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

package eu.cloudnetservice.modules.npc.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public record InventoryConfiguration(
  int inventorySize,
  boolean dynamicSize,
  boolean showFullServices,
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
      .showFullServices(configuration.showFullServices())
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
    @NonNull ItemLayout fullLayout
  ) {

  }

  public static class Builder {

    private int inventorySize = 54;
    private boolean dynamicSize = true;
    private boolean showFullServices = true;

    private ItemLayoutHolder defaultItems = new ItemLayoutHolder(
      ItemLayout.builder()
        .material("EGG")
        .displayName("§7%name%")
        .lore(Arrays.asList(
          " ",
          "§8● §e%state%",
          "§8● §7%online_players%§8/§7%max_players%",
          "§8● §7%motd%"
        )).build(),
      ItemLayout.builder()
        .material("EMERALD")
        .displayName("§7%name%")
        .lore(Arrays.asList(
          " ",
          "§8● §e%state%",
          "§8● §7%online_players%§8/§7%max_players%",
          "§8● §7%motd%"
        )).build(),
      ItemLayout.builder()
        .material("REDSTONE")
        .displayName("§7%name%")
        .lore(Arrays.asList(
          " ",
          "§8● §e%state%",
          "§8● §7%online_players%§8/§7%max_players%",
          "§8● §7%motd%"
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

    public @NonNull Builder showFullServices(boolean showFullServices) {
      this.showFullServices = showFullServices;
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
        this.showFullServices,
        this.defaultItems,
        this.perGroupLayouts,
        this.fixedItems);
    }
  }
}
