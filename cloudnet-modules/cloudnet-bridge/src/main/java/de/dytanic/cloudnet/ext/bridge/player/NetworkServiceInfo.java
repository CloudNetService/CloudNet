package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public class NetworkServiceInfo {

    protected ServiceEnvironmentType environment;

    protected UUID uniqueId;

    protected String serverName;

    public NetworkServiceInfo(ServiceEnvironmentType environment, UUID uniqueId, String serverName) {
        this.environment = environment;
        this.uniqueId = uniqueId;
        this.serverName = serverName;
    }

    public NetworkServiceInfo() {
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setEnvironment(ServiceEnvironmentType environment) {
        this.environment = environment;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

}