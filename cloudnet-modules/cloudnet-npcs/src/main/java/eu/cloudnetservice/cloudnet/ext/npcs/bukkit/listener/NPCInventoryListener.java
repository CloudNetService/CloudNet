package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;


import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCAction;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCProperties;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class NPCInventoryListener implements Listener {

    private static final Random RANDOM = new Random();

    private final BukkitNPCManagement npcManagement;

    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    private final Map<Integer, BukkitNPCProperties> propertiesCache = new HashMap<>();

    public NPCInventoryListener(BukkitNPCManagement npcManagement) {
        this.npcManagement = npcManagement;
    }

    @EventHandler
    public void handleNPCInteract(PlayerNPCInteractEvent event) {
        Player player = event.getPlayer();
        int entityId = event.getNPC().getEntityId();

        BukkitNPCProperties properties = this.propertiesCache.computeIfAbsent(entityId, key -> this.npcManagement.getNPCProperties().stream()
                .filter(npcProperty -> npcProperty.getEntityId() == key)
                .findFirst()
                .orElse(null));

        if (properties != null) {
            CloudNPC cloudNPC = properties.getHolder();

            if ((event.getAction() == EnumWrappers.EntityUseAction.INTERACT_AT && cloudNPC.getRightClickAction() == NPCAction.OPEN_INVENTORY)
                    || (event.getAction() == EnumWrappers.EntityUseAction.ATTACK && cloudNPC.getLeftClickAction() == NPCAction.OPEN_INVENTORY)) {
                player.openInventory(properties.getInventory());
            } else if ((event.getAction() == EnumWrappers.EntityUseAction.INTERACT_AT && cloudNPC.getRightClickAction() == NPCAction.DIRECT_CONNECT)
                    || (event.getAction() == EnumWrappers.EntityUseAction.ATTACK && cloudNPC.getLeftClickAction() == NPCAction.DIRECT_CONNECT)) {
                List<String> serviceNames = new ArrayList<>(properties.getServerSlots().values());

                if (serviceNames.size() > 0) {
                    String randomServiceName = serviceNames.get(RANDOM.nextInt(serviceNames.size()));
                    this.playerManager.getPlayerExecutor(player.getUniqueId()).connect(randomServiceName);
                }
            }
        }
    }

    @EventHandler
    public void handleInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();

        if (inventory != null && currentItem != null && inventory.getHolder() == null && event.getWhoClicked() instanceof Player) {
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
