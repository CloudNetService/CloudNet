package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.listener.NukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public final class NukkitCloudNetCloudPermissionsPlugin extends PluginBase {

    @Getter
    private static NukkitCloudNetCloudPermissionsPlugin instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        new CloudPermissionsPermissionManagement();
        injectPlayersCloudPermissible();

        getServer().getPluginManager().registerEvents(new NukkitCloudNetCloudPermissionsPlayerListener(), this);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    /*= ----------------------------------------------------------------- =*/

    private void injectPlayersCloudPermissible() {
        for (Player player : Server.getInstance().getOnlinePlayers().values())
            injectCloudPermissible(player);
    }

    public void injectCloudPermissible(Player player) {
        Validate.checkNotNull(player);

        try {
            Field field = Player.class.getDeclaredField("perm");
            field.setAccessible(true);
            field.set(player, new NukkitCloudNetCloudPermissionsPermissible(player));

        } catch (Exception ignored) {
        }
    }
}