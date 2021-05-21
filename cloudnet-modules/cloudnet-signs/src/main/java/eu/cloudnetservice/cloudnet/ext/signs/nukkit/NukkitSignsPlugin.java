package eu.cloudnetservice.cloudnet.ext.signs.nukkit;

import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.nukkit.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.nukkit.functionality.SignInteractListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.SignsServiceListener;

public class NukkitSignsPlugin extends PluginBase {

    @Override
    public void onEnable() {
        ServiceSignManagement<BlockEntitySign> signManagement = new NukkitSignManagement(this);
        signManagement.initialize();
        signManagement.registerToServiceRegistry();
        // command
        PluginCommand<?> pluginCommand = (PluginCommand<?>) this.getCommand("cloudsign");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(new CommandSigns(signManagement));
        }
        // nukkit listeners
        Server.getInstance().getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
        // cloudnet listener
        CloudNetDriver.getInstance().getEventManager().registerListeners(
                new GlobalChannelMessageListener(signManagement), new SignsServiceListener(signManagement));
    }

    @Override
    public void onDisable() {
        NukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
