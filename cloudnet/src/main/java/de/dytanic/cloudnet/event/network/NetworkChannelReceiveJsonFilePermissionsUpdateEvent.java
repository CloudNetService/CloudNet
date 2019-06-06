package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;

import java.util.List;

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

    public List<PermissionGroup> getPermissionGroups() {
        return this.permissionGroups;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setPermissionUsers(List<PermissionUser> permissionUsers) {
        this.permissionUsers = permissionUsers;
    }

    public void setPermissionGroups(List<PermissionGroup> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String toString() {
        return "NetworkChannelReceiveJsonFilePermissionsUpdateEvent(permissionUsers=" + this.getPermissionUsers() + ", permissionGroups=" + this.getPermissionGroups() + ", cancelled=" + this.isCancelled() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkChannelReceiveJsonFilePermissionsUpdateEvent))
            return false;
        final NetworkChannelReceiveJsonFilePermissionsUpdateEvent other = (NetworkChannelReceiveJsonFilePermissionsUpdateEvent) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$permissionUsers = this.getPermissionUsers();
        final Object other$permissionUsers = other.getPermissionUsers();
        if (this$permissionUsers == null ? other$permissionUsers != null : !this$permissionUsers.equals(other$permissionUsers))
            return false;
        final Object this$permissionGroups = this.getPermissionGroups();
        final Object other$permissionGroups = other.getPermissionGroups();
        if (this$permissionGroups == null ? other$permissionGroups != null : !this$permissionGroups.equals(other$permissionGroups))
            return false;
        if (this.isCancelled() != other.isCancelled()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkChannelReceiveJsonFilePermissionsUpdateEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $permissionUsers = this.getPermissionUsers();
        result = result * PRIME + ($permissionUsers == null ? 43 : $permissionUsers.hashCode());
        final Object $permissionGroups = this.getPermissionGroups();
        result = result * PRIME + ($permissionGroups == null ? 43 : $permissionGroups.hashCode());
        result = result * PRIME + (this.isCancelled() ? 79 : 97);
        return result;
    }
}