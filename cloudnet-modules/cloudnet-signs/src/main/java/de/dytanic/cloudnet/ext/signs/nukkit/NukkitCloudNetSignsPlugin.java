package de.dytanic.cloudnet.ext.signs.nukkit;


import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.CloudNetSignListener;
import de.dytanic.cloudnet.ext.signs.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class NukkitCloudNetSignsPlugin extends PluginBase {

    @Override
    public void onEnable() {
        new NukkitSignManagement(this);

        this.initListeners();
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void initListeners() {
        //Commands
        // todo: cloudsign command

        //CloudNet listeners
        CloudNetDriver.getInstance().getEventManager().registerListener(new CloudNetSignListener());

        //Nukkit listeners
        // todo: sign interaction listener

        //Sign knockback scheduler
        SignConfigurationEntry signConfigurationEntry = BukkitSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (signConfigurationEntry != null && signConfigurationEntry.getKnockbackDistance() > 0 && signConfigurationEntry.getKnockbackStrength() > 0) {
            // todo sign knockback runnable
        }
    }


}
