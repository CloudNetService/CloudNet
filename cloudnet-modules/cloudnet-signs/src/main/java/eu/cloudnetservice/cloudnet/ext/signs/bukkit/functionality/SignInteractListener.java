package eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.event.BukkitCloudSignInteractEvent;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignInteractListener implements Listener {

    protected final ServiceSignManagement<org.bukkit.block.Sign> signManagement;

    public SignInteractListener(ServiceSignManagement<org.bukkit.block.Sign> signManagement) {
        this.signManagement = signManagement;
    }

    @EventHandler
    public void handle(PlayerInteractEvent event) {
        SignConfigurationEntry entry = this.signManagement.getApplicableSignConfigurationEntry();
        if (entry != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getState() instanceof org.bukkit.block.Sign) {

            Sign sign = this.signManagement.getSignAt((org.bukkit.block.Sign) event.getClickedBlock().getState());
            if (sign != null) {
                boolean canConnect = this.signManagement.canConnect(sign, event.getPlayer()::hasPermission);

                BukkitCloudSignInteractEvent interactEvent = new BukkitCloudSignInteractEvent(event.getPlayer(), sign, !canConnect);
                Bukkit.getPluginManager().callEvent(interactEvent);

                if (!interactEvent.isCancelled() && interactEvent.getTarget().isPresent()) {
                    this.signManagement.getSignsConfiguration().sendMessage("server-connecting-message",
                            event.getPlayer()::sendMessage, m -> m.replace("%server%", interactEvent.getTarget().get().getName()));
                    this.getPlayerManager().getPlayerExecutor(event.getPlayer().getUniqueId())
                            .connect(interactEvent.getTarget().get().getName());
                }
            }
        }
    }

    protected IPlayerManager getPlayerManager() {
        return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
    }
}
