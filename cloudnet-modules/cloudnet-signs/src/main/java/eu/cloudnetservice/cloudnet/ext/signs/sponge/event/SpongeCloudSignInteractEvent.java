package eu.cloudnetservice.cloudnet.ext.signs.sponge.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.Optional;

public class SpongeCloudSignInteractEvent extends AbstractEvent implements TargetPlayerEvent, Cancellable {

    protected final Cause cause;
    protected final Player player;

    protected final Sign sign;

    protected boolean cancelled;
    protected ServiceInfoSnapshot target;

    public SpongeCloudSignInteractEvent(Cause cause, Player player, Sign sign, boolean cancelled) {
        this.cause = cause;
        this.player = player;
        this.sign = sign;
        this.target = sign.getCurrentTarget();
        this.cancelled = cancelled;
    }

    public Sign getSign() {
        return sign;
    }

    public Optional<ServiceInfoSnapshot> getTarget() {
        return Optional.ofNullable(this.target);
    }

    public void setTarget(ServiceInfoSnapshot target) {
        this.target = target;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull Player getTargetEntity() {
        return this.player;
    }

    @Override
    public @NotNull Cause getCause() {
        return this.cause;
    }
}
