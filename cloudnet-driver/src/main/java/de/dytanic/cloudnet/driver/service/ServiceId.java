package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public final class ServiceId implements INameable, SerializableObject {

    private UUID uniqueId;

    private String nodeUniqueId, taskName;

    private int taskServiceId;

    private ServiceEnvironmentType environment;

    public ServiceId(@NotNull UUID uniqueId, @NotNull String nodeUniqueId, String taskName, int taskServiceId, ServiceEnvironmentType environment) {
        this.uniqueId = uniqueId;
        this.nodeUniqueId = nodeUniqueId;
        this.taskName = taskName;
        this.taskServiceId = taskServiceId;
        this.environment = environment;
    }

    public ServiceId() {
    }

    public String getName() {
        return this.taskName + "-" + this.taskServiceId;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getNodeUniqueId() {
        return this.nodeUniqueId;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public int getTaskServiceId() {
        return this.taskServiceId;
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeUUID(this.uniqueId);
        buffer.writeString(this.nodeUniqueId);
        buffer.writeString(this.taskName);
        buffer.writeVarInt(this.taskServiceId);
        buffer.writeEnumConstant(this.environment);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.uniqueId = buffer.readUUID();
        this.nodeUniqueId = buffer.readString();
        this.taskName = buffer.readString();
        this.taskServiceId = buffer.readVarInt();
        this.environment = buffer.readEnumConstant(ServiceEnvironmentType.class);
    }
}