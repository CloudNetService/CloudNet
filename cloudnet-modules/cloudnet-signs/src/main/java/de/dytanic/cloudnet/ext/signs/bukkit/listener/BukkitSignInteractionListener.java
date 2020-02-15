package de.dytanic.cloudnet.ext.signs.bukkit.listener;

import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.bukkit.event.BukkitCloudSignInteractEvent;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class BukkitSignInteractionListener implements Listener {

    @EventHandler
    public void handleInteract(PlayerInteractEvent event) {
        SignConfigurationEntry entry = AbstractSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (entry != null) {
            if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) &&
                    event.getClickedBlock() != null &&
                    event.getClickedBlock().getState() instanceof org.bukkit.block.Sign) {
                for (Sign sign : BukkitSignManagement.getInstance().getSigns()) {
                    Location location = BukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location == null || !location.equals(event.getClickedBlock().getLocation())) {
                        continue;
                    }

                    String targetServer = sign.getServiceInfoSnapshot() == null ? null : sign.getServiceInfoSnapshot().getName();

                    BukkitCloudSignInteractEvent signInteractEvent = new BukkitCloudSignInteractEvent(event.getPlayer(), sign, targetServer);
                    Bukkit.getPluginManager().callEvent(signInteractEvent);

                    if (!signInteractEvent.isCancelled() && signInteractEvent.getTargetServer() != null) {
                        BridgePlayerManager.getInstance().proxySendPlayer(event.getPlayer().getUniqueId(), signInteractEvent.getTargetServer());

                        event.getPlayer().sendMessage(
                                ChatColor.translateAlternateColorCodes('&',
                                        SignConfigurationProvider.load().getMessages().get("server-connecting-message")
                                                .replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                                )
                        );
                    }

                    return;
                }
            }
        }
    }
}