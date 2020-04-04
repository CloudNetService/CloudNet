package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;


import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NPCInventoryListener implements Listener {

    private BukkitNPCManagement npcManagement;

    public NPCInventoryListener(BukkitNPCManagement npcManagement) {
        this.npcManagement = npcManagement;
    }

    @EventHandler
    public void handleNPCInteract(PlayerNPCInteractEvent event) {
        if (event.getAction() == PlayerNPCInteractEvent.Action.RIGHT_CLICKED) {
            this.npcManagement.getNPCProperties().stream()
                    .filter(properties -> properties.getEntityId() == event.getNPC().getEntityId())
                    .findFirst()
                    .ifPresent(properties -> event.getPlayer().openInventory(properties.getInventory()));
        }
    }

    @EventHandler
    public void handleInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();

        if (inventory != null && currentItem != null && event.getWhoClicked() instanceof Player) {
            this.npcManagement.getNPCProperties().stream()
                    .filter(properties -> properties.getInventory().equals(inventory))
                    .findFirst()
                    .ifPresent(properties -> {
                        event.setCancelled(true);
                        int slot = event.getSlot();

                        if (properties.getServerSlots().containsKey(slot)) {
                            Player player = (Player) event.getWhoClicked();
                            String serverName = properties.getServerSlots().get(slot);

                            BridgePlayerManager.getInstance().proxySendPlayer(player.getUniqueId(), serverName);
                        }
                    });
        }
    }

}
