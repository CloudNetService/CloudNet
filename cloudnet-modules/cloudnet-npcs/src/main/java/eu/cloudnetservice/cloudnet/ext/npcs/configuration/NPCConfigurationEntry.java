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

    public NPCConfigurationEntry() {
        for (int i = 1; i < 10; i++) {
            this.inventoryLayout.put(i, new ItemLayout("BLACK_STAINED_GLASS_PANE", " ", new ArrayList<>()));
        }
    }

    public NPCConfigurationEntry(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public NPCConfigurationEntry(String targetGroup, double infoLineDistance, int inventorySize, int startSlot, int endSlot, boolean showFullServices, ItemLayout onlineItem, ItemLayout emptyItem, ItemLayout fullItem, Map<Integer, ItemLayout> inventoryLayout) {
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
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public double getInfoLineDistance() {
        return infoLineDistance;
    }

    public void setInfoLineDistance(double infoLineDistance) {
        this.infoLineDistance = infoLineDistance;
    }

    public int getInventorySize() {
        return inventorySize;
    }

    public void setInventorySize(int inventorySize) {
        this.inventorySize = inventorySize;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public void setStartSlot(int startSlot) {
        this.startSlot = startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(int endSlot) {
        this.endSlot = endSlot;
    }

    public boolean isShowFullServices() {
        return showFullServices;
    }

    public void setShowFullServices(boolean showFullServices) {
        this.showFullServices = showFullServices;
    }

    public ItemLayout getOnlineItem() {
        return onlineItem;
    }

    public void setOnlineItem(ItemLayout onlineItem) {
        this.onlineItem = onlineItem;
    }

    public ItemLayout getEmptyItem() {
        return emptyItem;
    }

    public void setEmptyItem(ItemLayout emptyItem) {
        this.emptyItem = emptyItem;
    }

    public ItemLayout getFullItem() {
        return fullItem;
    }

    public void setFullItem(ItemLayout fullItem) {
        this.fullItem = fullItem;
    }

    public Map<Integer, ItemLayout> getInventoryLayout() {
        return inventoryLayout;
    }

    public void setInventoryLayout(Map<Integer, ItemLayout> inventoryLayout) {
        this.inventoryLayout = inventoryLayout;
    }

    public static class ItemLayout {

        private String material;

        private String displayName;

        private List<String> lore;

        public ItemLayout(String material, String displayName, List<String> lore) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public String getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }

    }

}
