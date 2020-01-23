package de.dytanic.cloudnet.ext.signs.bukkit.event;


import de.dytanic.cloudnet.ext.signs.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private boolean cancelled;

    private Sign clickedSign;

    private String targetServer;

    public BukkitCloudSignInteractEvent(Player who, Sign clickedSign, String targetServer) {
        super(who);
        this.clickedSign = clickedSign;
        this.targetServer = targetServer;
    }

    public Sign getClickedSign() {
        return clickedSign;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

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
