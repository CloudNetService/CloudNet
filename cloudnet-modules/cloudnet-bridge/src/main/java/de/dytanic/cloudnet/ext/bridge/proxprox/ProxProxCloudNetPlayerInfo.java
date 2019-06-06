package de.dytanic.cloudnet.ext.bridge.proxprox;

import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.util.Locale;
import java.util.UUID;

final class ProxProxCloudNetPlayerInfo {

    private UUID uniqueId;

    private Locale locale;

    private String name, xBoxId;

    private HostAndPort address, connectedServer;

    private long ping;

    public ProxProxCloudNetPlayerInfo(UUID uniqueId, Locale locale, String name, String xBoxId, HostAndPort address, HostAndPort connectedServer, long ping) {
        this.uniqueId = uniqueId;
        this.locale = locale;
        this.name = name;
        this.xBoxId = xBoxId;
        this.address = address;
        this.connectedServer = connectedServer;
        this.ping = ping;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public String getName() {
        return this.name;
    }

    public String getXBoxId() {
        return this.xBoxId;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public HostAndPort getConnectedServer() {
        return this.connectedServer;
    }

    public long getPing() {
        return this.ping;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setXBoxId(String xBoxId) {
        this.xBoxId = xBoxId;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public void setConnectedServer(HostAndPort connectedServer) {
        this.connectedServer = connectedServer;
    }

    public void setPing(long ping) {
        this.ping = ping;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProxProxCloudNetPlayerInfo)) return false;
        final ProxProxCloudNetPlayerInfo other = (ProxProxCloudNetPlayerInfo) o;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$locale = this.getLocale();
        final Object other$locale = other.getLocale();
        if (this$locale == null ? other$locale != null : !this$locale.equals(other$locale)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$xBoxId = this.getXBoxId();
        final Object other$xBoxId = other.getXBoxId();
        if (this$xBoxId == null ? other$xBoxId != null : !this$xBoxId.equals(other$xBoxId)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$connectedServer = this.getConnectedServer();
        final Object other$connectedServer = other.getConnectedServer();
        if (this$connectedServer == null ? other$connectedServer != null : !this$connectedServer.equals(other$connectedServer))
            return false;
        if (this.getPing() != other.getPing()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $locale = this.getLocale();
        result = result * PRIME + ($locale == null ? 43 : $locale.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $xBoxId = this.getXBoxId();
        result = result * PRIME + ($xBoxId == null ? 43 : $xBoxId.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $connectedServer = this.getConnectedServer();
        result = result * PRIME + ($connectedServer == null ? 43 : $connectedServer.hashCode());
        final long $ping = this.getPing();
        result = result * PRIME + (int) ($ping >>> 32 ^ $ping);
        return result;
    }

    public String toString() {
        return "ProxProxCloudNetPlayerInfo(uniqueId=" + this.getUniqueId() + ", locale=" + this.getLocale() + ", name=" + this.getName() + ", xBoxId=" + this.getXBoxId() + ", address=" + this.getAddress() + ", connectedServer=" + this.getConnectedServer() + ", ping=" + this.getPing() + ")";
    }
}