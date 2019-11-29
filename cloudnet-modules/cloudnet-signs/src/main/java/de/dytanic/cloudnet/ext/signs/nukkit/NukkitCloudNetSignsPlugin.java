package de.dytanic.cloudnet.ext.signs.nukkit;


import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.CloudNetSignListener;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.nukkit.listener.NukkitSignInteractionListener;
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
        super.getServer().getCommandMap().register("CloudNet-Signs", new CommandCloudSign());

        //CloudNet listeners
        CloudNetDriver.getInstance().getEventManager().registerListener(new CloudNetSignListener());

        //Nukkit listeners
        super.getServer().getPluginManager().registerEvents(new NukkitSignInteractionListener(), this);

        //Sign knockback scheduler
        SignConfigurationEntry signConfigurationEntry = AbstractSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (signConfigurationEntry != null && signConfigurationEntry.getKnockbackDistance() > 0 && signConfigurationEntry.getKnockbackStrength() > 0) {
            super.getServer().getScheduler().scheduleDelayedRepeatingTask(this, new NukkitSignKnockbackRunnable(signConfigurationEntry), 20, 5);
        }
    }


}
