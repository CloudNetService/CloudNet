package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;

import java.util.UUID;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkServiceInfo)) return false;
        final NetworkServiceInfo other = (NetworkServiceInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$environment = this.getEnvironment();
        final Object other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment))
            return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$serverName = this.getServerName();
        final Object other$serverName = other.getServerName();
        if (this$serverName == null ? other$serverName != null : !this$serverName.equals(other$serverName))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkServiceInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $environment = this.getEnvironment();
        result = result * PRIME + ($environment == null ? 43 : $environment.hashCode());
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $serverName = this.getServerName();
        result = result * PRIME + ($serverName == null ? 43 : $serverName.hashCode());
        return result;
    }

    public String toString() {
        return "NetworkServiceInfo(environment=" + this.getEnvironment() + ", uniqueId=" + this.getUniqueId() + ", serverName=" + this.getServerName() + ")";
    }
}