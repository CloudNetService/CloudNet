package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.gomint.listener.GoMintCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.plugin.Plugin;
import io.gomint.plugin.PluginName;
import io.gomint.plugin.Version;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import lombok.Getter;

@Getter
@PluginName("CloudNet-CloudPerms")
@Version(major = 1, minor = 0)
public final class GoMintCloudNetCloudPermissionsPlugin extends Plugin {

  @Getter
  private static GoMintCloudNetCloudPermissionsPlugin instance;

  @Override
  public void onInstall() {
    instance = this;
  }

  @Override
  public void onStartup() {
    new CloudPermissionsPermissionManagement();
    injectEntityPlayersCloudPermissionManager();

    registerListener(new GoMintCloudNetCloudPermissionsPlayerListener());
  }

  @Override
  public void onUninstall() {
    CloudNetDriver.getInstance().getEventManager()
      .unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
      this.getClass().getClassLoader());
  }

  /*= ------------------------------------------------------------------------------- =*/

  private void injectEntityPlayersCloudPermissionManager() {
    for (EntityPlayer entityPlayer : GoMint.instance().getPlayers()) {
      injectPermissionManager(entityPlayer);
    }
  }

  public void injectPermissionManager(EntityPlayer entityPlayer) {
    Validate.checkNotNull(entityPlayer);

    try {
      Field field = entityPlayer.getClass()
        .getDeclaredField("permissionManager");
      field.setAccessible(true);

      Field modifiersField = Field.class.getDeclaredField("modifiers");

      AccessController.doPrivileged(new PrivilegedAction() {
        @Override
        public Object run() {
          modifiersField.setAccessible(true);
          return null;
        }
      });

      modifiersField
        .setInt(field, modifiersField.getModifiers() & ~Modifier.FINAL);
      field.set(entityPlayer,
        new GoMintCloudNetCloudPermissionsPermissionManager(
          (io.gomint.server.entity.EntityPlayer) entityPlayer,
          entityPlayer.getPermissionManager()));

    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
  }
}