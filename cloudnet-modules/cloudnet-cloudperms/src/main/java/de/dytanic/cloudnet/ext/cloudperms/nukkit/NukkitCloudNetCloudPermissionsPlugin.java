package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.listener.NukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.lang.reflect.Field;

public final class NukkitCloudNetCloudPermissionsPlugin extends PluginBase {

    private static NukkitCloudNetCloudPermissionsPlugin instance;

    public static NukkitCloudNetCloudPermissionsPlugin getInstance() {
        return NukkitCloudNetCloudPermissionsPlugin.instance;
    }

    private final CloudPermissionsManagement permissionsManagement = CloudPermissionsManagement.newInstance();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        injectPlayersCloudPermissible();

        getServer().getPluginManager().registerEvents(new NukkitCloudNetCloudPermissionsPlayerListener(this.permissionsManagement), this);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }


    private void injectPlayersCloudPermissible() {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            injectCloudPermissible(player);
        }
    }

    public void injectCloudPermissible(Player player) {
        Preconditions.checkNotNull(player);

        try {
            Field field = Player.class.getDeclaredField("perm");
            field.setAccessible(true);
            field.set(player, new NukkitCloudNetCloudPermissionsPermissible(player, this.permissionsManagement));

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}