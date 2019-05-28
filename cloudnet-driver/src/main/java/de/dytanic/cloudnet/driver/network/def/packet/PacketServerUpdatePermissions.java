package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Collection;

public class PacketServerUpdatePermissions extends Packet {

  public PacketServerUpdatePermissions(UpdateType updateType,
    IPermissionUser permissionUser) {
    super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument()
        .append("permissions_event", true)
        .append("updateType", updateType)
        .append("permissionUser", permissionUser)
      , null);
  }

  public PacketServerUpdatePermissions(UpdateType updateType,
    IPermissionGroup permissionGroup) {
    super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument()
        .append("permissions_event", true)
        .append("updateType", updateType)
        .append("permissionGroup", permissionGroup)
      , null);
  }

  public PacketServerUpdatePermissions(UpdateType updateType,
    Collection<? extends IPermissionGroup> permissionGroups) {
    super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument()
        .append("permissions_event", true)
        .append("updateType", updateType)
        .append("permissionGroups", permissionGroups)
      , null);
  }

  public PacketServerUpdatePermissions(UpdateType updateType,
    Iterable<? extends IPermissionUser> permissionUsers) {
    super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument()
        .append("permissions_event", true)
        .append("updateType", updateType)
        .append("permissionUsers", permissionUsers)
      , null);
  }

  public enum UpdateType {
    ADD_USER,
    UPDATE_USER,
    DELETE_USER,
    SET_USERS,
    ADD_GROUP,
    UPDATE_GROUP,
    DELETE_GROUP,
    SET_GROUPS
  }
}