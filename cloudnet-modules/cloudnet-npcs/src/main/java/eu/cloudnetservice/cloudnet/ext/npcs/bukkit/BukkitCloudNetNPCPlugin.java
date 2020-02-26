package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import com.comphenix.protocol.utility.MinecraftVersion;
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
        if (this.isCompatibleVersion()) {
            this.npcManagement = new BukkitNPCManagement(this);

            this.registerListeners();
        }
    }

    private boolean isCompatibleVersion() {
        boolean paper = true;
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        } catch (ClassNotFoundException exception) {
            paper = false;
        }

        if (!paper || !MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE)) {
            CloudNetDriver.getInstance().getLogger().error("The NPC Selector extension does only work on Paper 1.13.2+ servers!");
            Bukkit.getPluginManager().disablePlugin(this);

            return false;
        }

        return true;
    }

    private void registerListeners() {
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
        if (this.npcManagement != null) {
            this.npcManagement.shutdown();
        }
    }

}
