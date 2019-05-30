package de.dytanic.cloudnet.ext.syncproxy.bungee.util;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;

@Getter
public class LoginPendingConnectionCommandSender implements CommandSender {

  private final Collection<String> permissions = Iterables
    .newArrayList(), groups = Iterables.newArrayList();

  private final LoginEvent loginEvent;

  private final UUID uniqueId;

  public LoginPendingConnectionCommandSender(LoginEvent loginEvent,
    UUID uniqueId) {
    this.loginEvent = loginEvent;
    this.uniqueId = uniqueId;

    this.groups.addAll(ProxyServer.getInstance().getConfigurationAdapter()
      .getGroups(loginEvent.getConnection().getName()));

    for (String group : groups) {
      for (String permission : ProxyServer.getInstance()
        .getConfigurationAdapter().getPermissions(group)) {
        this.setPermission(permission, true);
      }
    }
  }

  @Override
  public String getName() {
    return loginEvent.getConnection().getName();
  }

  @Override
  public void sendMessage(String message) {
    //Not supported
  }

  @Override
  public void sendMessages(String... messages) {
    //Not supported
  }

  @Override
  public void addGroups(String... groups) {
    //Not supported
  }

  @Override
  public void removeGroups(String... groups) {
    //Not supported
  }

  @Override
  public boolean hasPermission(String permission) {
    Validate.checkNotNull(permission);

    return ProxyServer.getInstance().getPluginManager()
      .callEvent(new PermissionCheckEvent(
        this,
        permission,
        this.permissions.contains(permission.toLowerCase()))
      ).hasPermission();
  }

  @Override
  public void setPermission(String permission, boolean value) {
    Validate.checkNotNull(permission);

    if (value) {
      this.permissions.add(permission.toLowerCase());
    } else {
      this.permissions.remove(permission.toLowerCase());
    }
  }
}