package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.CloudNetSignListener;
import de.dytanic.cloudnet.ext.signs.bukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitSignInteractionListener;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCloudNetSignsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        new BukkitSignManagement(this);

        this.initListeners();
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(super.getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void initListeners() {
        //Commands
        PluginCommand cloudSignCommand = this.getCommand("cloudsign");

        if (cloudSignCommand != null) {
            cloudSignCommand.setExecutor(new CommandCloudSign());
            cloudSignCommand.setPermission("cloudnet.command.cloudsign");
            cloudSignCommand.setUsage("/cloudsign create <targetGroup>");
            cloudSignCommand.setDescription("Add or Removes signs from the provided Group configuration");
        }

        //CloudNet listeners
        CloudNetDriver.getInstance().getEventManager().registerListener(new CloudNetSignListener());

        //Bukkit listeners
        Bukkit.getPluginManager().registerEvents(new BukkitSignInteractionListener(), this);

        //Sign knockback scheduler
        SignConfigurationEntry signConfigurationEntry = AbstractSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (signConfigurationEntry != null && signConfigurationEntry.getKnockbackDistance() > 0 && signConfigurationEntry.getKnockbackStrength() > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new BukkitSignKnockbackRunnable(signConfigurationEntry), 20, 5);
        }
    }

}