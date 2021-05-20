package eu.cloudnetservice.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSignsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ServiceSignManagement<Sign> signManagement = new BukkitSignManagement(this);
        signManagement.registerToServiceRegistry();

        CloudNetDriver.getInstance().getEventManager().registerListener(new GlobalChannelMessageListener(signManagement));
    }

    @Override
    public void onDisable() {
        BukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
