package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.command.CloudNPCCommand;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.NPCInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCloudNetNPCPlugin extends JavaPlugin {

    private BukkitNPCManagement npcManagement;

    @Override
    public void onEnable() {
        this.npcManagement = new BukkitNPCManagement(this);

        PluginCommand cloudNPCCommand = this.getCommand("cloudnpc");

        if (cloudNPCCommand != null) {
            cloudNPCCommand.setExecutor(new CloudNPCCommand(this.npcManagement));
            cloudNPCCommand.setPermission("cloudnet.command.cloudnpc");
            cloudNPCCommand.setUsage("/cloudnpc create <targetGroup> <displayName> <skinUUID> <itemInHand> <shouldLookAtPlayer> <shouldImitatePlayer>");
            cloudNPCCommand.setDescription("Adds or removes server selector NPCs");
        }

        CloudNetDriver.getInstance().getEventManager().registerListener(this.npcManagement);
        Bukkit.getPluginManager().registerEvents(new NPCInventoryListener(this.npcManagement), this);
    }

    @Override
    public void onDisable() {
        this.npcManagement.shutdown();
    }

}
