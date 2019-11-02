package de.dytanic.cloudnet.examples.bridge;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * The Bridge Methods are only available when the bridge plugin is enabled on a service
 */
public final class BukkitSpongeNukkitBridgeExample {

    public void changeMotd() {
        BukkitCloudNetHelper.setApiMotd("My new Motd"); //Change the motd string, which will be included with the next updates
        BridgeHelper.updateServiceInfo();
    }

    public void changeMaxPlayers() {
        SpongeCloudNetHelper.setMaxPlayers(16); //Changed the displayed player limit
        BridgeHelper.updateServiceInfo();
    }

    public void changeState() {
        NukkitCloudNetHelper.setState("PREMIUM"); //Sets the server state for this instance. Text like "LOBBY", "FULL", "INGAME" which other services indicates, what state the service has
        NukkitCloudNetHelper.setExtra("Waterfall"); //Extra is an custom string, which can displayed on signs or as communication string
        Wrapper.getInstance().publishServiceInfoUpdate(); //Original alternative to XXXCloudNetHelper.updateServiceInfo()
    }

    public void changeToIngame() {
        BukkitCloudNetHelper.setApiMotd("INGAME"); //if the motd, state or extra is "ingame", "running" or "playing", the default modules indicate as service which should be hide.
        BukkitCloudNetHelper.setState("running");
        BukkitCloudNetHelper.setExtra("playing");
        BridgeHelper.updateServiceInfo();
    }

    public void changeToIngameShortCut() {
        BukkitCloudNetHelper.changeToIngame(); //Set the state to "INGAME" and send an ServiceInfoSnapshot update
    }
}