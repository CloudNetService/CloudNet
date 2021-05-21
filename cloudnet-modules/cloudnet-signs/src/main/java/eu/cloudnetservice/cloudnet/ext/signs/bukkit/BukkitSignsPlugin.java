package eu.cloudnetservice.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality.SignInteractListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.SignsServiceListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSignsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ServiceSignManagement<Sign> signManagement = new BukkitSignManagement(this);
        signManagement.initialize();
        signManagement.registerToServiceRegistry();
        // bukkit command
        PluginCommand pluginCommand = this.getCommand("cloudsign");
        if (pluginCommand != null) {
            CommandSigns commandSigns = new CommandSigns(signManagement);
            pluginCommand.setExecutor(commandSigns);
            pluginCommand.setTabCompleter(commandSigns);
        }
        // bukkit listeners
        Bukkit.getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
        // cloudnet listeners
        CloudNetDriver.getInstance().getEventManager().registerListeners(
                new GlobalChannelMessageListener(signManagement), new SignsServiceListener(signManagement));
    }

    @Override
    public void onDisable() {
        BukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
