package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import org.checkerframework.checker.optional.qual.MaybePresent;

public class VelocityCloudNetCloudPermissionsPermissionProvider implements PermissionProvider {

  private final IPermissionManagement permissionsManagement;

  public VelocityCloudNetCloudPermissionsPermissionProvider(IPermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @Override
  public @MaybePresent PermissionFunction createFunction(@MaybePresent PermissionSubject subject) {
    return subject instanceof Player
      ? new VelocityCloudNetCloudPermissionsPermissionFunction(((Player) subject).getUniqueId(),
      this.permissionsManagement)
      : null;
  }
}
