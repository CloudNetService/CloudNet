package de.dytanic.cloudnet.ext.syncproxy.configuration;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SyncProxyTabList {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

  protected String header;

  protected String footer;

  public SyncProxyTabList(String header, String footer) {
    this.header = header;
    this.footer = footer;
  }

  public SyncProxyTabList() {
  }

  public static String replaceTabListItem(String input, UUID playerUniqueId) {
    input = input
      .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
      .replace("%proxy_uniqueId%", Wrapper.getInstance().getServiceId().getUniqueId().toString())
      .replace("%proxy_task_name%", Wrapper.getInstance().getServiceId().getTaskName())
      .replace("%time%", DATE_FORMAT.format(System.currentTimeMillis()));

    if (SyncProxyConstants.CLOUD_PERMS_ENABLED && playerUniqueId != null) {
      IPermissionManagement permissionManagement = CloudNetDriver.getInstance().getPermissionManagement();

      IPermissionUser permissionUser = permissionManagement.getUser(playerUniqueId);

      if (permissionUser != null) {
        IPermissionGroup group = permissionManagement.getHighestPermissionGroup(permissionUser);

        if (group != null) {
          input = input.replace("%prefix%", group.getPrefix())
            .replace("%suffix%", group.getSuffix())
            .replace("%display%", group.getDisplay())
            .replace("%color%", group.getColor())
            .replace("%group%", group.getName());
        }
      }
    }

    return input.replace("&", "§");
  }

  public String getHeader() {
    return this.header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getFooter() {
    return this.footer;
  }

  public void setFooter(String footer) {
    this.footer = footer;
  }
}
