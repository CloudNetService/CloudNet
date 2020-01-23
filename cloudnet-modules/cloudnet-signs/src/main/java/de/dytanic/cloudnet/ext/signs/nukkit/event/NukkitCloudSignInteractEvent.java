package de.dytanic.cloudnet.ext.signs.nukkit.event;


import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.player.PlayerEvent;
import de.dytanic.cloudnet.ext.signs.Sign;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

    private Sign clickedSign;

    private String targetServer;

    public NukkitCloudSignInteractEvent(Player who, Sign clickedSign, String targetServer) {
        super.player = who;
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

}
