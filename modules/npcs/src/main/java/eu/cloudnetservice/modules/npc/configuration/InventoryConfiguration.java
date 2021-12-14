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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class InventoryConfiguration {

  private final int inventorySize;
  private final boolean dynamicSize;
  private final boolean showFullServices;

  private final ItemLayoutHolder defaultItems;
  private final Map<String, ItemLayoutHolder> perGroupLayouts;

  private final Map<Integer, ItemLayout> fixedItems;

  protected InventoryConfiguration(
    int inventorySize,
    boolean dynamicSize,
    boolean showFullServices,
    @NotNull ItemLayoutHolder defaultItems,
    @NotNull Map<String, ItemLayoutHolder> perGroupLayouts,
    @NotNull Map<Integer, ItemLayout> fixedItems
  ) {
    this.inventorySize = inventorySize;
    this.dynamicSize = dynamicSize;
    this.showFullServices = showFullServices;
    this.defaultItems = defaultItems;
    this.perGroupLayouts = perGroupLayouts;
    this.fixedItems = fixedItems;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull InventoryConfiguration configuration) {
    return builder()
      .inventorySize(configuration.getInventorySize())
      .dynamicSize(configuration.isDynamicSize())
      .showFullServices(configuration.isShowFullServices())
      .defaultItems(configuration.getDefaultItems())
      .perGroupLayouts(configuration.getPerGroupLayouts())
      .fixedItems(configuration.getFixedItems());
  }

  public int getInventorySize() {
    return this.inventorySize;
  }

  public boolean isDynamicSize() {
    return this.dynamicSize;
  }

  public boolean isShowFullServices() {
    return this.showFullServices;
  }

  public @NotNull ItemLayoutHolder getDefaultItems() {
    return this.defaultItems;
  }

  public @NotNull Map<String, ItemLayoutHolder> getPerGroupLayouts() {
    return this.perGroupLayouts;
  }

  public @NotNull ItemLayoutHolder getHolder(String @NotNull ... groups) {
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

  public @NotNull Map<Integer, ItemLayout> getFixedItems() {
    return this.fixedItems;
  }

  public static class ItemLayoutHolder {

    private final ItemLayout emptyLayout;
    private final ItemLayout onlineLayout;
    private final ItemLayout fullLayout;

    public ItemLayoutHolder(
      @NotNull ItemLayout emptyLayout,
      @NotNull ItemLayout onlineLayout,
      @NotNull ItemLayout fullLayout
    ) {
      this.emptyLayout = emptyLayout;
      this.onlineLayout = onlineLayout;
      this.fullLayout = fullLayout;
    }

    public @NotNull ItemLayout getEmptyLayout() {
      return this.emptyLayout;
    }

    public @NotNull ItemLayout getOnlineLayout() {
      return this.onlineLayout;
    }

    public @NotNull ItemLayout getFullLayout() {
      return this.fullLayout;
    }
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

    public @NotNull Builder inventorySize(int inventorySize) {
      this.inventorySize = inventorySize;
      return this;
    }

    public @NotNull Builder dynamicSize(boolean dynamicSize) {
      this.dynamicSize = dynamicSize;
      return this;
    }

    public @NotNull Builder showFullServices(boolean showFullServices) {
      this.showFullServices = showFullServices;
      return this;
    }

    public @NotNull Builder defaultItems(@NotNull ItemLayoutHolder defaultItems) {
      this.defaultItems = defaultItems;
      return this;
    }

    public @NotNull Builder perGroupLayouts(@NotNull Map<String, ItemLayoutHolder> perGroupLayouts) {
      this.perGroupLayouts = new HashMap<>(perGroupLayouts);
      return this;
    }

    public @NotNull Builder fixedItems(@NotNull Map<Integer, ItemLayout> fixedItems) {
      this.fixedItems = new HashMap<>(fixedItems);
      return this;
    }

    public @NotNull InventoryConfiguration build() {
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
