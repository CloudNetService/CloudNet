package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;


import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NPCInventoryListener implements Listener {

    private static final Random RANDOM = new Random();

    private final BukkitNPCManagement npcManagement;

    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    public NPCInventoryListener(BukkitNPCManagement npcManagement) {
        this.npcManagement = npcManagement;
    }

    @EventHandler
    public void handleNPCInteract(PlayerNPCInteractEvent event) {
        Player player = event.getPlayer();
        int entityId = event.getNPC().getEntityId();

        this.npcManagement.getNPCProperties().stream()
                .filter(npcProperty -> npcProperty.getEntityId() == entityId)
                .findFirst()
                .ifPresent(properties -> {
                    if (event.getAction() == PlayerNPCInteractEvent.Action.RIGHT_CLICKED) {
                        player.openInventory(properties.getInventory());
                    } else {
                        List<String> services = this.npcManagement.filterNPCServices(properties.getHolder()).stream()
                                .map(pair -> pair.getFirst().getName())
                                .collect(Collectors.toList());

                        if (services.size() > 0) {
                            String randomServiceName = services.get(RANDOM.nextInt(services.size()));
                            this.playerManager.getPlayerExecutor(player.getUniqueId()).connect(randomServiceName);
                        }
                    }
                });
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

                            this.playerManager.getPlayerExecutor(player.getUniqueId()).connect(serverName);
                        }
                    });
        }
    }

}
