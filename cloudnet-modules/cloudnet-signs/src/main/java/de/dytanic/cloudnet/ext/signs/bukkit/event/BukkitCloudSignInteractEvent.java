package de.dytanic.cloudnet.ext.signs.bukkit.event;


import de.dytanic.cloudnet.ext.signs.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private boolean cancelled;

    private final Sign clickedSign;

    private String targetServer;

    public BukkitCloudSignInteractEvent(@NotNull Player who, @NotNull Sign clickedSign, @Nullable String targetServer) {
        super(who);
        this.clickedSign = clickedSign;
        this.targetServer = targetServer;
    }

    @NotNull
    public Sign getClickedSign() {
        return clickedSign;
    }

    @Nullable
    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

}
