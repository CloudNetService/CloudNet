package eu.cloudnetservice.cloudnet.ext.signs.nukkit.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected final Sign sign;
    protected ServiceInfoSnapshot target;

    public NukkitCloudSignInteractEvent(@NotNull Player who, @NotNull Sign sign, boolean cancelled) {
        this.player = who;
        this.sign = sign;
        this.target = sign.getCurrentTarget();
        this.setCancelled(cancelled);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Sign getSign() {
        return sign;
    }

    public Optional<ServiceInfoSnapshot> getTarget() {
        return Optional.ofNullable(this.target);
    }
}
