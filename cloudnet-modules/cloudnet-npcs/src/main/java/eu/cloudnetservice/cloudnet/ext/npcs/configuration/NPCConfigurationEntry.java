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

package eu.cloudnetservice.cloudnet.ext.npcs.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPCConfigurationEntry {

  private String targetGroup = "Lobby";
  private double infoLineDistance = 0.1D;
  private double knockbackDistance = 0.7D;
  private double knockbackStrength = 0.8D;
  private int inventorySize = 54;
  private int startSlot = 10;
  private int endSlot = 54;
  private boolean showFullServices = true;

  private ItemLayout onlineItem = new ItemLayout("LIME_DYE", "§a%name%", Arrays.asList(
    " ",
    "§8● §e%state%",
    "§8● §7%online_players%§8/§7%max_players%",
    "§8● §7%motd%"
  ));

  private ItemLayout emptyItem = new ItemLayout("LIGHT_GRAY_DYE", "§7%name%", Arrays.asList(
    " ",
    "§8● §e%state%",
    "§8● §7%online_players%§8/§7%max_players%",
    "§8● §7%motd%"
  ));

  private ItemLayout fullItem = new ItemLayout("REDSTONE", "§c%name%", Arrays.asList(
    " ",
    "§8● §e%state%",
    "§8● §7%online_players%§8/§7%max_players%",
    "§8● §7%motd%"
  ));

  private Map<Integer, ItemLayout> inventoryLayout = new HashMap<>();
  private LabyModEmotes labyModEmotes = new LabyModEmotes();
  private long npcTabListRemoveTicks = 40L;

  public NPCConfigurationEntry() {
    for (int i = 1; i < 10; i++) {
      this.inventoryLayout.put(i, new ItemLayout("BLACK_STAINED_GLASS_PANE", " ", new ArrayList<>()));
    }
  }

  public NPCConfigurationEntry(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public NPCConfigurationEntry(String targetGroup, double infoLineDistance, int inventorySize, int startSlot,
    int endSlot, boolean showFullServices, ItemLayout onlineItem, ItemLayout emptyItem, ItemLayout fullItem,
    Map<Integer, ItemLayout> inventoryLayout, LabyModEmotes labyModEmotes, long npcTabListRemoveTicks) {
    this(targetGroup, infoLineDistance, 0.7D, 0.8D, inventorySize, startSlot, endSlot, showFullServices, onlineItem,
      emptyItem, fullItem, inventoryLayout, labyModEmotes, npcTabListRemoveTicks);
  }

  public NPCConfigurationEntry(String targetGroup, double infoLineDistance, double knockbackDistance,
    double knockbackStrength, int inventorySize, int startSlot, int endSlot, boolean showFullServices,
    ItemLayout onlineItem, ItemLayout emptyItem, ItemLayout fullItem, Map<Integer, ItemLayout> inventoryLayout,
    LabyModEmotes labyModEmotes, long npcTabListRemoveTicks) {
    this.targetGroup = targetGroup;
    this.infoLineDistance = infoLineDistance;
    this.knockbackDistance = knockbackDistance;
    this.knockbackStrength = knockbackStrength;
    this.inventorySize = inventorySize;
    this.startSlot = startSlot;
    this.endSlot = endSlot;
    this.showFullServices = showFullServices;
    this.onlineItem = onlineItem;
    this.emptyItem = emptyItem;
    this.fullItem = fullItem;
    this.inventoryLayout = inventoryLayout;
    this.labyModEmotes = labyModEmotes;
    this.npcTabListRemoveTicks = npcTabListRemoveTicks;
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public double getInfoLineDistance() {
    return this.infoLineDistance;
  }

  public void setInfoLineDistance(double infoLineDistance) {
    this.infoLineDistance = infoLineDistance;
  }

  public double getKnockbackDistance() {
    return this.knockbackDistance;
  }

  public void setKnockbackDistance(double knockbackDistance) {
    this.knockbackDistance = knockbackDistance;
  }

  public double getKnockbackStrength() {
    return this.knockbackStrength;
  }

  public void setKnockbackStrength(double knockbackStrength) {
    this.knockbackStrength = knockbackStrength;
  }

  public int getInventorySize() {
    return this.inventorySize;
  }

  public void setInventorySize(int inventorySize) {
    this.inventorySize = inventorySize;
  }

  public int getStartSlot() {
    return this.startSlot;
  }

  public void setStartSlot(int startSlot) {
    this.startSlot = startSlot;
  }

  public int getEndSlot() {
    return this.endSlot;
  }

  public void setEndSlot(int endSlot) {
    this.endSlot = endSlot;
  }

  public boolean isShowFullServices() {
    return this.showFullServices;
  }

  public void setShowFullServices(boolean showFullServices) {
    this.showFullServices = showFullServices;
  }

  public ItemLayout getOnlineItem() {
    return this.onlineItem;
  }

  public void setOnlineItem(ItemLayout onlineItem) {
    this.onlineItem = onlineItem;
  }

  public ItemLayout getEmptyItem() {
    return this.emptyItem;
  }

  public void setEmptyItem(ItemLayout emptyItem) {
    this.emptyItem = emptyItem;
  }

  public ItemLayout getFullItem() {
    return this.fullItem;
  }

  public void setFullItem(ItemLayout fullItem) {
    this.fullItem = fullItem;
  }

  public Map<Integer, ItemLayout> getInventoryLayout() {
    return this.inventoryLayout;
  }

  public void setInventoryLayout(Map<Integer, ItemLayout> inventoryLayout) {
    this.inventoryLayout = inventoryLayout;
  }

  public LabyModEmotes getLabyModEmotes() {
    return this.labyModEmotes;
  }

  public void setLabyModEmotes(LabyModEmotes labyModEmotes) {
    this.labyModEmotes = labyModEmotes;
  }

  public long getNPCTabListRemoveTicks() {
    return this.npcTabListRemoveTicks;
  }

  public void setNPCTabListRemoveTicks(long npcTabListRemoveTicks) {
    this.npcTabListRemoveTicks = npcTabListRemoveTicks;
  }

  public static class LabyModEmotes {

    // See https://docs.labymod.net/pages/server/labymod/emote_api/ for all available emote ids.
    private int[] emoteIds = new int[]{2, 3, 49};
    private int[] onJoinEmoteIds = new int[]{4, 20};
    private int[] onKnockbackEmoteIds = new int[]{37};
    private long minEmoteDelayTicks = 20 * 20;
    private long maxEmoteDelayTicks = 30 * 20;
    private boolean playEmotesSynchronous = false;

    public LabyModEmotes() {
    }

    public int[] getEmoteIds() {
      return this.emoteIds;
    }

    public void setEmoteIds(int[] emoteIds) {
      this.emoteIds = emoteIds;
    }

    public int[] getOnJoinEmoteIds() {
      return this.onJoinEmoteIds;
    }

    public void setOnJoinEmoteIds(int[] onJoinEmoteIds) {
      this.onJoinEmoteIds = onJoinEmoteIds;
    }

    public int[] getOnKnockbackEmoteIds() {
      return this.onKnockbackEmoteIds;
    }

    public void setOnKnockbackEmoteIds(int[] onKnockbackEmoteIds) {
      this.onKnockbackEmoteIds = onKnockbackEmoteIds;
    }

    public long getMinEmoteDelayTicks() {
      return this.minEmoteDelayTicks;
    }

    public void setMinEmoteDelayTicks(long minEmoteDelayTicks) {
      this.minEmoteDelayTicks = minEmoteDelayTicks;
    }

    public long getMaxEmoteDelayTicks() {
      return this.maxEmoteDelayTicks;
    }

    public void setMaxEmoteDelayTicks(long maxEmoteDelayTicks) {
      this.maxEmoteDelayTicks = maxEmoteDelayTicks;
    }

    public boolean isPlayEmotesSynchronous() {
      return this.playEmotesSynchronous;
    }

    public void setPlayEmotesSynchronous(boolean playEmotesSynchronous) {
      this.playEmotesSynchronous = playEmotesSynchronous;
    }

  }

  public static class ItemLayout {

    private String material;
    private int subId = -1;
    private String displayName;
    private List<String> lore;

    public ItemLayout() {
    }

    public ItemLayout(String material, String displayName, List<String> lore) {
      this.material = material;
      this.displayName = displayName;
      this.lore = lore;
    }

    public String getMaterial() {
      return this.material;
    }

    public void setMaterial(String material) {
      this.material = material;
    }

    public int getSubId() {
      return this.subId;
    }

    public void setSubId(int subId) {
      this.subId = subId;
    }

    public String getDisplayName() {
      return this.displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public List<String> getLore() {
      return this.lore;
    }

    public void setLore(List<String> lore) {
      this.lore = lore;
    }

  }

}
