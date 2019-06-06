package de.dytanic.cloudnet.ext.bridge.velocity;

import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.util.UUID;

final class VelocityCloudNetPlayerInfo {

    private UUID uniqueId;

    private String name, server;

    private int ping;

    private HostAndPort address;

    public VelocityCloudNetPlayerInfo(UUID uniqueId, String name, String server, int ping, HostAndPort address) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.server = server;
        this.ping = ping;
        this.address = address;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public String getServer() {
        return this.server;
    }

    public int getPing() {
        return this.ping;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof VelocityCloudNetPlayerInfo)) return false;
        final VelocityCloudNetPlayerInfo other = (VelocityCloudNetPlayerInfo) o;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$server = this.getServer();
        final Object other$server = other.getServer();
        if (this$server == null ? other$server != null : !this$server.equals(other$server)) return false;
        if (this.getPing() != other.getPing()) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $server = this.getServer();
        result = result * PRIME + ($server == null ? 43 : $server.hashCode());
        result = result * PRIME + this.getPing();
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        return result;
    }

    public String toString() {
        return "VelocityCloudNetPlayerInfo(uniqueId=" + this.getUniqueId() + ", name=" + this.getName() + ", server=" + this.getServer() + ", ping=" + this.getPing() + ", address=" + this.getAddress() + ")";
    }
}