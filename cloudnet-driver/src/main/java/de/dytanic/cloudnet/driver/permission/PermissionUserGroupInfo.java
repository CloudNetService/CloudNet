package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends BasicJsonDocPropertyable implements SerializableObject {

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
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.group);
        buffer.writeLong(this.timeOutMillis);
        buffer.writeString(super.properties.toJson());
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.group = buffer.readString();
        this.timeOutMillis = buffer.readLong();
        super.properties = JsonDocument.newDocument(buffer.readString());
    }
}