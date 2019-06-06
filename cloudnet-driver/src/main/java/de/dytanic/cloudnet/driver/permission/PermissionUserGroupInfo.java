package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

}