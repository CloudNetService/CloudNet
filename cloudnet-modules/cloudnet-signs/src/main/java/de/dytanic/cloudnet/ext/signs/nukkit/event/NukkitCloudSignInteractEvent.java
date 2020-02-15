package de.dytanic.cloudnet.ext.signs.nukkit.event;


import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import de.dytanic.cloudnet.ext.signs.Sign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private Sign clickedSign;

    private String targetServer;

    public NukkitCloudSignInteractEvent(@NotNull Player who, @NotNull Sign clickedSign, @Nullable String targetServer) {
        super.player = who;
        this.clickedSign = clickedSign;
        this.targetServer = targetServer;
    }

    public static HandlerList getHandlers() {
        return HANDLER_LIST;
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

}
