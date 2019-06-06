package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

public class PermissionUserGroupInfo extends BasicJsonDocPropertyable {

    protected String group;

    protected long timeOutMillis;

    public PermissionUserGroupInfo(String group, long timeOutMillis) {
        this.group = group;
        this.timeOutMillis = timeOutMillis;
    }

    public String getGroup() {
        return this.group;
    }

    public long getTimeOutMillis() {
        return this.timeOutMillis;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setTimeOutMillis(long timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

    public String toString() {
        return "PermissionUserGroupInfo(group=" + this.getGroup() + ", timeOutMillis=" + this.getTimeOutMillis() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionUserGroupInfo)) return false;
        final PermissionUserGroupInfo other = (PermissionUserGroupInfo) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$group = this.getGroup();
        final Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) return false;
        if (this.getTimeOutMillis() != other.getTimeOutMillis()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PermissionUserGroupInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final long $timeOutMillis = this.getTimeOutMillis();
        result = result * PRIME + (int) ($timeOutMillis >>> 32 ^ $timeOutMillis);
        return result;
    }
}