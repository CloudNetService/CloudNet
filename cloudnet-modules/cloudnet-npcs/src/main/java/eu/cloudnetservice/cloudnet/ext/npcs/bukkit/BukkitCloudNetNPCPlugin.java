package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.command.CloudNPCCommand;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod.LabyModEmotePlayer;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.NPCInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCloudNetNPCPlugin extends JavaPlugin {

    private BukkitNPCManagement npcManagement;

    @Override
    public void onEnable() {
        this.npcManagement = new BukkitNPCManagement(this);
        CloudNetDriver.getInstance().getServicesRegistry().registerService(AbstractNPCManagement.class, "BukkitNPCManagement", this.npcManagement);

        this.registerListeners();
        new LabyModEmotePlayer(this, this.npcManagement);
    }

    private void registerListeners() {
        PluginCommand pluginCommand = this.getCommand("cloudnpc");

        if (pluginCommand != null) {
            pluginCommand.setExecutor(new CloudNPCCommand(this.npcManagement));
            pluginCommand.setPermission("cloudnet.command.cloudnpc");
            pluginCommand.setUsage("/cloudnpc create <targetGroup> <displayName> <skinUUID> <itemInHand> <shouldLookAtPlayer> <shouldImitatePlayer>");
            pluginCommand.setDescription("Adds or removes server selector NPCs");
        }

        CloudNetDriver.getInstance().getEventManager().registerListener(this.npcManagement);
        Bukkit.getPluginManager().registerEvents(new NPCInventoryListener(this.npcManagement), this);
    }

    @Override
    public void onDisable() {
        if (this.npcManagement != null) {
            this.npcManagement.shutdown();
            CloudNetDriver.getInstance().getServicesRegistry().unregisterService(AbstractNPCManagement.class, this.npcManagement);
        }
    }

}
