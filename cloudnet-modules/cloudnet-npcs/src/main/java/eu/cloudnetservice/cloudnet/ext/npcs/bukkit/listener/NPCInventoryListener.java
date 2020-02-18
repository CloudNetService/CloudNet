package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;


import com.github.realpanamo.npc.event.PlayerNPCInteractEvent;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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
        if (event.getClickedInventory() != null && event.getWhoClicked() instanceof Player) {

            if (this.npcManagement.getNPCInventories().containsValue(event.getClickedInventory())) {
                event.setCancelled(true);
                // todo: handle item click
            }

        }
    }

}
