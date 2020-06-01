package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends SerializableJsonDocPropertyable implements SerializableObject {

    protected String group;

    protected long timeOutMillis;

    public PermissionUserGroupInfo(@NotNull String group, long timeOutMillis) {
        this.group = group;
        this.timeOutMillis = timeOutMillis;
    }

    public PermissionUserGroupInfo() {
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.group);
        buffer.writeLong(this.timeOutMillis);
        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.group = buffer.readString();
        this.timeOutMillis = buffer.readLong();
        super.read(buffer);
    }
}