package eu.cloudnetservice.cloudnet.ext.signs.nukkit;

import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;

public class NukkitSignsPlugin extends PluginBase {

    @Override
    public void onEnable() {
        ServiceSignManagement<BlockEntitySign> signManagement = new NukkitSignManagement(this);
        signManagement.registerToServiceRegistry();

        CloudNetDriver.getInstance().getEventManager().registerListener(new GlobalChannelMessageListener(signManagement));
    }

    @Override
    public void onDisable() {
        NukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
