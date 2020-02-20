package eu.cloudnetservice.cloudnet.ext.npcs.configuration;


import java.util.*;

public class NPCConfigurationEntry {

    private String targetGroup = "Lobby";

    private int inventorySize = 54;

    private int startSlot = 9;

    private boolean showFullServices = true;

    private ItemLayout onlineItem = new ItemLayout("LIME_DYE", "§a%name%", Arrays.asList(
            " ",
            "§e%state%",
            "§e%online_players%§8/§e%max_players%",
            "§e%motd%"
    ));

    private ItemLayout emptyItem = new ItemLayout("LIGHT_GRAY_DYE", "§7%name%", Arrays.asList(
            " ",
            "§e%state%",
            "§e%online_players%§8/§e%max_players%",
            "§e%motd%"
    ));

    private ItemLayout fullItem = new ItemLayout("REDSTONE", "§c%name%", Arrays.asList(
            " ",
            "§e%state%",
            "§e%online_players%§8/§e%max_players%",
            "§e%motd%"
    ));

    private Map<Integer, ItemLayout> inventoryLayout = new HashMap<>();

    public NPCConfigurationEntry() {
        for (int i = 0; i < 9; i++) {
            this.inventoryLayout.put(i, new ItemLayout("BLACK_STAINED_GLASS_PANE", " ", new ArrayList<>()));
        }
    }

    public NPCConfigurationEntry(String targetGroup, int inventorySize, int startSlot, boolean showFullServices, ItemLayout onlineItem, ItemLayout emptyItem, ItemLayout fullItem, Map<Integer, ItemLayout> inventoryLayout) {
        this.targetGroup = targetGroup;
        this.inventorySize = inventorySize;
        this.startSlot = startSlot;
        this.showFullServices = showFullServices;
        this.onlineItem = onlineItem;
        this.emptyItem = emptyItem;
        this.fullItem = fullItem;
        this.inventoryLayout = inventoryLayout;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public int getInventorySize() {
        return inventorySize;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public boolean isShowFullServices() {
        return showFullServices;
    }

    public ItemLayout getOnlineItem() {
        return onlineItem;
    }

    public ItemLayout getEmptyItem() {
        return emptyItem;
    }

    public ItemLayout getFullItem() {
        return fullItem;
    }

    public Map<Integer, ItemLayout> getInventoryLayout() {
        return inventoryLayout;
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
