package eu.cloudnetservice.cloudnet.ext.npcs.configuration;


import java.util.*;

public class NPCConfigurationEntry {

    private String targetGroup = "Lobby";

    private double infoLineDistance = 0.1D;

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

    public NPCConfigurationEntry(String targetGroup, double infoLineDistance, int inventorySize, int startSlot, int endSlot, boolean showFullServices, ItemLayout onlineItem, ItemLayout emptyItem, ItemLayout fullItem, Map<Integer, ItemLayout> inventoryLayout, LabyModEmotes labyModEmotes, long npcTabListRemoveTicks) {
        this.targetGroup = targetGroup;
        this.infoLineDistance = infoLineDistance;
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

        // See https://docs.labymod.net/pages/server/emote_api/ for all available emote ids.
        private int[] emoteIds = new int[]{2, 3, 49};

        private int[] onJoinEmoteIds = new int[]{4, 20};

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
