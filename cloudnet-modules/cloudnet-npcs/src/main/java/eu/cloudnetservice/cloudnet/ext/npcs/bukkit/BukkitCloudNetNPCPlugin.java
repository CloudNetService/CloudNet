package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.NPCInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCloudNetNPCPlugin extends JavaPlugin {

    private BukkitNPCManagement npcManagement;

    @Override
    public void onEnable() {
        this.npcManagement = new BukkitNPCManagement(this);

        CloudNetDriver.getInstance().getEventManager().registerListener(this.npcManagement);
        Bukkit.getPluginManager().registerEvents(new NPCInventoryListener(this.npcManagement), this);
    }

    @Override
    public void onDisable() {
        this.npcManagement.shutdown();
    }

}
