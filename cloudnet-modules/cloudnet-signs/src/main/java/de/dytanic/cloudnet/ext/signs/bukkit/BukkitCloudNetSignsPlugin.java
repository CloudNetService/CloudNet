package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.bukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitCloudNetSignListener;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitSignInteractionListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCloudNetSignsPlugin extends JavaPlugin {

    private static BukkitCloudNetSignsPlugin instance;

    public BukkitCloudNetSignsPlugin() {
        instance = this;
    }

    public static BukkitCloudNetSignsPlugin getInstance() {
        return BukkitCloudNetSignsPlugin.instance;
    }

    @Override
    public void onEnable() {
        new BukkitSignManagement(this);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.initListeners();
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(getClassLoader());
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
        CloudNetDriver.getInstance().getEventManager().registerListener(new BukkitCloudNetSignListener());

        //Bukkit listeners
        Bukkit.getPluginManager().registerEvents(new BukkitSignInteractionListener(), this);

        //Sign knockback scheduler
        SignConfigurationEntry signConfigurationEntry = BukkitSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (signConfigurationEntry.getKnockbackDistance() > 0 && signConfigurationEntry.getKnockbackStrength() > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SignKnockbackRunnable(signConfigurationEntry), 20, 5);
        }
    }
}