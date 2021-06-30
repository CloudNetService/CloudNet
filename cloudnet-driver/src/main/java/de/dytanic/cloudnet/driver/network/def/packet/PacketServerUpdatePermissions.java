/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.Collection;

public class PacketServerUpdatePermissions extends Packet {

  public PacketServerUpdatePermissions(UpdateType updateType, IPermissionUser permissionUser) {
    super(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL,
      ProtocolBuffer.create().writeEnumConstant(updateType).writeObject(permissionUser));
  }

  public PacketServerUpdatePermissions(UpdateType updateType, IPermissionGroup permissionGroup) {
    super(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL,
      ProtocolBuffer.create().writeEnumConstant(updateType).writeObject(permissionGroup));
  }

  private PacketServerUpdatePermissions(UpdateType updateType, Collection<? extends SerializableObject> content) {
    super(PacketConstants.PERMISSIONS_PUBLISH_CHANNEL,
      ProtocolBuffer.create().writeEnumConstant(updateType).writeObjectCollection(content));
  }

  public static PacketServerUpdatePermissions setGroups(Collection<? extends IPermissionGroup> permissionGroups) {
    return new PacketServerUpdatePermissions(UpdateType.SET_GROUPS, permissionGroups);
  }

  public static PacketServerUpdatePermissions setUsers(Collection<? extends IPermissionUser> permissionUsers) {
    return new PacketServerUpdatePermissions(UpdateType.SET_USERS, permissionUsers);
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
