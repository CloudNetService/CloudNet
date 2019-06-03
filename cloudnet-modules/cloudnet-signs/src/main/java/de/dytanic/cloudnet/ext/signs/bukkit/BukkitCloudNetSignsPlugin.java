package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.bukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitCloudNetSignListener;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitSignInteractionListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCloudNetSignsPlugin extends JavaPlugin {

    @Getter
    private static BukkitCloudNetSignsPlugin instance;

    public BukkitCloudNetSignsPlugin() {
        instance = this;
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
        getCommand("cloudsign").setExecutor(new CommandCloudSign());
        getCommand("cloudsign").setPermission("cloudnet.command.cloudsign");
        getCommand("cloudsign").setUsage("/cloudsign create <targetGroup>");
        getCommand("cloudsign").setDescription("Add or Removes signs from the provided Group configuration");

        //CloudNet listeners
        CloudNetDriver.getInstance().getEventManager().registerListener(new BukkitCloudNetSignListener());

        //Bukkit listeners
        Bukkit.getPluginManager().registerEvents(new BukkitSignInteractionListener(), this);
    }
}