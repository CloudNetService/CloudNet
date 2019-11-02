package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class NetworkChannelReceiveJsonFilePermissionsUpdateEvent extends NetworkEvent implements ICancelable {

    private List<PermissionUser> permissionUsers;

    private List<PermissionGroup> permissionGroups;

    private boolean cancelled;

    public NetworkChannelReceiveJsonFilePermissionsUpdateEvent(INetworkChannel channel, List<PermissionUser> permissionUsers, List<PermissionGroup> permissionGroups) {
        super(channel);
        this.permissionUsers = permissionUsers;
        this.permissionGroups = permissionGroups;
    }

    public List<PermissionUser> getPermissionUsers() {
        return this.permissionUsers;
    }

    public void setPermissionUsers(List<PermissionUser> permissionUsers) {
        this.permissionUsers = permissionUsers;
    }

    public List<PermissionGroup> getPermissionGroups() {
        return this.permissionGroups;
    }

    public void setPermissionGroups(List<PermissionGroup> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}