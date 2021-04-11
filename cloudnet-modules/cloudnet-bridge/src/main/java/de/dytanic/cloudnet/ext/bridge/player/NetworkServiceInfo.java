package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public class NetworkServiceInfo implements SerializableObject {

    protected ServiceId serviceId;
    protected String[] groups;

    public NetworkServiceInfo(ServiceId serviceId, String[] groups) {
        this.serviceId = serviceId;
        this.groups = groups;
    }

    public NetworkServiceInfo() {
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.serviceId.getEnvironment();
    }

    public UUID getUniqueId() {
        return this.serviceId.getUniqueId();
    }

    public String getServerName() {
        return this.serviceId.getName();
    }

    public String[] getGroups() {
        return this.groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public String getTaskName() {
        return this.serviceId.getTaskName();
    }

    public ServiceId getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(ServiceId serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeObject(this.serviceId);
        buffer.writeStringArray(this.groups);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.serviceId = buffer.readObject(ServiceId.class);
        this.groups = buffer.readStringArray();
    }
}