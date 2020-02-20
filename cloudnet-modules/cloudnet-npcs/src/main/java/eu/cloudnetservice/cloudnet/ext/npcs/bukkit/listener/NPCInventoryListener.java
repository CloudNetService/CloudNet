package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;


import com.github.realpanamo.npc.event.PlayerNPCInteractEvent;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class NPCInventoryListener implements Listener {

    private BukkitNPCManagement npcManagement;

    public NPCInventoryListener(BukkitNPCManagement npcManagement) {
        this.npcManagement = npcManagement;
    }

    @EventHandler
    public void handleNPCInteract(PlayerNPCInteractEvent event) {
        if (event.getAction() == PlayerNPCInteractEvent.Action.RIGHT_CLICKED) {
            CloudNPC cloudNPC = this.npcManagement.getNPC(event.getNPC().getEntityId());

            if (cloudNPC != null) {
                event.getPlayer().openInventory(this.npcManagement.getInventory(cloudNPC));
            }
        }
    }

    @EventHandler
    public void handleInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if (inventory != null && event.getCurrentItem() != null && event.getWhoClicked() instanceof Player) {

            if (this.npcManagement.getNPCInventories().containsValue(inventory)) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                int slot = event.getSlot();

                CloudNPC cloudNPC = this.npcManagement.getByInventory(inventory);

                if (cloudNPC != null && cloudNPC.getServerSlots().containsKey(slot)) {
                    String serverName = cloudNPC.getServerSlots().get(slot);

                    BridgePlayerManager.getInstance().proxySendPlayer(player.getUniqueId(), serverName);
                }

            }

        }
    }

}
