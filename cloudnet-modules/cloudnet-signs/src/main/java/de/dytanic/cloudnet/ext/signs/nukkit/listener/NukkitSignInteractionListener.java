package de.dytanic.cloudnet.ext.signs.nukkit.listener;


import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.Location;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.nukkit.NukkitSignManagement;

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

                    BridgePlayerManager.getInstance().proxySendPlayer(event.getPlayer().getUniqueId(), sign.getServiceInfoSnapshot().getServiceId().getName());

                    event.getPlayer().sendMessage(
                            SignConfigurationProvider.load().getMessages().get("server-connecting-message")
                                    .replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                                    .replace('&', '§')
                    );

                    return;
                }
            }
        }
    }


}
