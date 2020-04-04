package de.dytanic.cloudnet.ext.signs.nukkit.listener;


import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.Location;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.NukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.nukkit.event.NukkitCloudSignInteractEvent;

public class NukkitSignInteractionListener implements Listener {

    private NukkitSignManagement nukkitSignManagement;

    public NukkitSignInteractionListener(NukkitSignManagement nukkitSignManagement) {
        this.nukkitSignManagement = nukkitSignManagement;
    }

    @EventHandler
    public void handleInteract(PlayerInteractEvent event) {
        SignConfigurationEntry entry = this.nukkitSignManagement.getOwnSignConfigurationEntry();

        if (entry != null) {
            if ((event.getAction().equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) &&
                    event.getBlock() != null &&
                    event.getBlock().getLevel().getBlockEntity(event.getBlock().getLocation()) instanceof BlockEntitySign) {
                for (Sign sign : this.nukkitSignManagement.getSigns()) {
                    Location location = this.nukkitSignManagement.toLocation(sign.getWorldPosition());

                    if (location == null || !location.equals(event.getBlock().getLocation())) {
                        continue;
                    }

                    String targetServer = sign.getServiceInfoSnapshot() == null ? null : sign.getServiceInfoSnapshot().getName();

                    NukkitCloudSignInteractEvent signInteractEvent = new NukkitCloudSignInteractEvent(event.getPlayer(), sign, targetServer);
                    Server.getInstance().getPluginManager().callEvent(signInteractEvent);

                    if (!signInteractEvent.isCancelled() && signInteractEvent.getTargetServer() != null) {
                        CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
                                .getPlayerExecutor(event.getPlayer().getUniqueId()).connect(signInteractEvent.getTargetServer());

                        event.getPlayer().sendMessage(
                                SignConfigurationProvider.load().getMessages().get("server-connecting-message")
                                        .replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                                        .replace('&', '§')
                        );

                    }

                    return;
                }
            }
        }
    }


}
