package de.dytanic.cloudnet.ext.bridge.bungee;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public final class BungeeCloudNetPlayerInfo {

    private UUID uniqueId;

    private String name, server;

    private int ping;

    private HostAndPort address;

    public BungeeCloudNetPlayerInfo(UUID uniqueId, String name, String server, int ping, HostAndPort address) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.server = server;
        this.ping = ping;
        this.address = address;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPing() {
        return this.ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

}