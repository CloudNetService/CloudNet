package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends BasicJsonDocPropertyable {

    protected String group;

    protected long timeOutMillis;

    public PermissionUserGroupInfo(@NotNull String group, long timeOutMillis) {
        this.group = group;
        this.timeOutMillis = timeOutMillis;
    }

    @NotNull
    public String getGroup() {
        return this.group;
    }

    public void setGroup(@NotNull String group) {
        this.group = group;
    }

    public long getTimeOutMillis() {
        return this.timeOutMillis;
    }

    public void setTimeOutMillis(long timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

}