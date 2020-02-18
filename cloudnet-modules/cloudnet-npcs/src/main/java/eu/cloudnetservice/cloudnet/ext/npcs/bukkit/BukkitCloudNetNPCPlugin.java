package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import com.github.realpanamo.npc.NPCPool;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.NPCInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCloudNetNPCPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        NPCPool npcPool = new NPCPool(this);

        BukkitNPCManagement npcManagement = new BukkitNPCManagement(npcPool);

        CloudNetDriver.getInstance().getEventManager().registerListener(npcManagement);
        Bukkit.getPluginManager().registerEvents(new NPCInventoryListener(npcManagement), this);
    }

}
