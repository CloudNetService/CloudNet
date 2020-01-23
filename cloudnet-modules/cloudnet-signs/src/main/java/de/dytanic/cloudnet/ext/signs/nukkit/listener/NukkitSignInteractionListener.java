package de.dytanic.cloudnet.ext.signs.nukkit.listener;


import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.Location;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.NukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.nukkit.event.NukkitCloudSignInteractEvent;

public class NukkitSignInteractionListener implements Listener {

    @EventHandler
    public void handleInteract(PlayerInteractEvent event) {
        SignConfigurationEntry entry = AbstractSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (entry != null) {
            if ((event.getAction().equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) &&
                    event.getBlock() != null &&
                    event.getBlock().getLevel().getBlockEntity(event.getBlock().getLocation()) instanceof BlockEntitySign) {
                for (Sign sign : AbstractSignManagement.getInstance().getSigns()) {
                    Location location = NukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location == null || sign.getServiceInfoSnapshot() == null ||
                            !location.equals(event.getBlock().getLocation())) {
                        continue;
                    }

                    NukkitCloudSignInteractEvent signInteractEvent = new NukkitCloudSignInteractEvent(event.getPlayer(), sign, sign.getServiceInfoSnapshot().getServiceId().getName());
                    Server.getInstance().getPluginManager().callEvent(signInteractEvent);

                    if (!signInteractEvent.isCancelled() && signInteractEvent.getTargetServer() != null) {
                        BridgePlayerManager.getInstance().proxySendPlayer(event.getPlayer().getUniqueId(), signInteractEvent.getTargetServer());

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
