package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public class NetworkServiceInfo {

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

    public String getTaskName() {
        return this.serviceId.getTaskName();
    }

    public ServiceId getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(ServiceId serviceId) {
        this.serviceId = serviceId;
    }
}