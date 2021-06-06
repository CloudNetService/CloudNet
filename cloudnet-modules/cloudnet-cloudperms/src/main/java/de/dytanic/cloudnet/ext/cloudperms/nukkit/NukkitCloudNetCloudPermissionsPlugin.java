package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.listener.NukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Field;

public final class NukkitCloudNetCloudPermissionsPlugin extends PluginBase {

  @Override
  public void onEnable() {
    this.injectPlayersCloudPermissible();

    super.getServer().getPluginManager().registerEvents(
      new NukkitCloudNetCloudPermissionsPlayerListener(this, CloudNetDriver.getInstance().getPermissionManagement()),
      this
    );
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void injectPlayersCloudPermissible() {
    Server.getInstance().getOnlinePlayers().values().forEach(this::injectCloudPermissible);
  }

  public void injectCloudPermissible(Player player) {
    Preconditions.checkNotNull(player);

    try {
      Field field = Player.class.getDeclaredField("perm");
      field.setAccessible(true);
      field.set(player,
        new NukkitCloudNetCloudPermissionsPermissible(player, CloudNetDriver.getInstance().getPermissionManagement()));

    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

}
