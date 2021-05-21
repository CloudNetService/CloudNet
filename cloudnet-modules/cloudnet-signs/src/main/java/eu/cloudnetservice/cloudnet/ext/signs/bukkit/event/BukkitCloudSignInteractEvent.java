package eu.cloudnetservice.cloudnet.ext.signs.bukkit.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Optional;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Sign clickedSign;

    private boolean cancelled;
    private ServiceInfoSnapshot target;

    public BukkitCloudSignInteractEvent(Player who, Sign clickedSign, boolean cancelled) {
        super(who);
        this.clickedSign = clickedSign;
        this.cancelled = cancelled;
        this.target = clickedSign.getCurrentTarget();
    }

    public Sign getClickedSign() {
        return clickedSign;
    }

    public Optional<ServiceInfoSnapshot> getTarget() {
        return Optional.ofNullable(this.target);
    }

    public void setTarget(ServiceInfoSnapshot target) {
        this.target = target;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
